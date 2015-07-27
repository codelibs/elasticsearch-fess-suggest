package org.codelibs.elasticsearch.rest;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.elasticsearch.runner.net.Curl;
import org.codelibs.elasticsearch.runner.net.CurlResponse;
import org.elasticsearch.indices.IndexMissingException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.*;


public class FessSuggestRestActionTest {
    private static ElasticsearchClusterRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = new ElasticsearchClusterRunner();
        runner.onBuild((number, settingsBuilder) -> {
            settingsBuilder.put("http.cors.enabled", true);
            settingsBuilder.put("index.number_of_replicas", 0);
            settingsBuilder.put("script.disable_dynamic", false);
            settingsBuilder.put("script.groovy.sandbox.enabled", true);
        }).build(newConfigs().ramIndexStore().numOfNode(1));
        runner.ensureYellow();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        runner.close();
        runner.clean();
    }

    @Before
    public void before() throws Exception {
        try {
            runner.admin().indices().prepareDelete("_all").execute().actionGet();
        } catch (IndexMissingException ignore) {

        }
        runner.refresh();
    }

    @Test
    public void test_suggest() throws Exception {
        final int docNum = 10;

        Curl.post(runner.masterNode(), "fess/_fsuggest/create").execute();
        for(int i=0;i<docNum;i++) {
            Curl.post(runner.masterNode(), "fess/_fsuggest/update").body(
                "{\n" +
                    "\"keyword\" : \"検索" + i + " エンジン" + "\",\n" +
                    "\"fields\" : [\"aaa\", \"bbb\"],\n" +
                    "\"tags\" : [\"tag1\", \"tag2\"],\n" +
                    "\"roles\" : [\"role1\", \"role2\"]\n" +
                    "}"
            ).execute();
        }
        runner.refresh();

        CurlResponse response1 = Curl.get(runner.masterNode(), "fess/_fsuggest")
            .param("q", "検索").param("roles", "role1").execute();
        assertEquals(docNum, (int) response1.getContentAsMap().get("total"));

        CurlResponse response2 = Curl.get(runner.masterNode(), "fess/_fsuggest")
            .param("q", "検索9").param("roles", "role1").execute();
        assertEquals(1, (int) response2.getContentAsMap().get("total"));

        CurlResponse response3 = Curl.get(runner.masterNode(), "fess/_fsuggest")
            .param("q", "検索1 エンジ").param("roles", "role1").execute();
        assertEquals(1, (int) response3.getContentAsMap().get("total"));

        CurlResponse response4 = Curl.get(runner.masterNode(), "fess/_fsuggest")
            .param("q", "検索 エンジ").param("roles", "role1").execute();
        assertEquals(0, (int) response4.getContentAsMap().get("total"));

        CurlResponse response5 = Curl.get(runner.masterNode(), "fess/_fsuggest")
            .param("q", "けんさく").param("roles", "role1").execute();
        assertEquals(docNum, (int) response5.getContentAsMap().get("total"));
    }
}
