package org.codelibs.elasticsearch.rest;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.elasticsearch.runner.net.Curl;
import org.codelibs.elasticsearch.runner.net.CurlResponse;
import org.elasticsearch.index.IndexNotFoundException;
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
            settingsBuilder.put("http.cors.allow-origin", "*");
            settingsBuilder.put("index.number_of_shards", 1);
            settingsBuilder.put("index.number_of_replicas", 0);
            settingsBuilder.putArray("discovery.zen.ping.unicast.hosts", "localhost:9301-9399");
            settingsBuilder.put("plugin.types", "org.codelibs.elasticsearch.FessSuggestPlugin,org.codelibs.elasticsearch.kuromoji.neologd.KuromojiNeologdPlugin");
            settingsBuilder.put("fsuggest.ngquery", "k,ken");
        }).build(newConfigs().clusterName("FessSuggestRestActionTest").numOfNode(1));

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
        } catch (IndexNotFoundException ignore) {

        }
        runner.refresh();
    }

    @Test
    public void test_suggest() throws Exception {
        final int docNum = 10;

        Curl.post(runner.masterNode(), "fess/_fsuggest/create").execute();
        for (int i = 0; i < docNum; i++) {
            Curl.post(runner.masterNode(), "fess/_fsuggest/update/searchword").body(
                "{\n" +
                    "\"keyword\" : \"検索" + i + " エンジン" + "\",\n" +
                    "\"fields\" : [\"aaa\", \"bbb\"],\n" +
                    "\"tags\" : [\"tag1\", \"tag2\"],\n" +
                    "\"roles\" : [\"role1\", \"role2\"]\n" +
                    "}"
            ).execute();
        }
        runner.refresh();

        CurlResponse response0 = Curl.get(runner.masterNode(), "fess/_fsuggest")
            .param("q", "").param("roles", "role1").execute();
        assertEquals(0, (int) response0.getContentAsMap().get("total"));

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

    @Test
    public void test_suggestFromDocument() throws Exception {
        Curl.post(runner.masterNode(), "fess/_fsuggest/create").execute();
        Curl.post(runner.masterNode(), "fess/_fsuggest/update/document").body(
            "{\n" +
                "\"document\" : \"検索エンジンの仕組み。" + "\",\n" +
                "\"fields\" : [\"aaa\", \"bbb\"],\n" +
                "\"tags\" : [\"tag1\", \"tag2\"],\n" +
                "\"roles\" : [\"role1\", \"role2\"]\n" +
                "}"
        ).execute();
        runner.refresh();

        CurlResponse response0 = Curl.get(runner.masterNode(), "fess/_fsuggest")
            .param("q", "").param("roles", "role1").execute();
        assertEquals(0, (int) response0.getContentAsMap().get("total"));

        CurlResponse response1 = Curl.get(runner.masterNode(), "fess/_fsuggest")
            .param("q", "検索").param("roles", "role1").execute();
        assertEquals(1, (int) response1.getContentAsMap().get("total"));

        CurlResponse response2= Curl.get(runner.masterNode(), "fess/_fsuggest")
            .param("q", "検索エンジン").param("roles", "role1").execute();
        assertEquals(1, (int) response2.getContentAsMap().get("total"));

        CurlResponse response3= Curl.get(runner.masterNode(), "fess/_fsuggest")
            .param("q", "仕組み").param("roles", "role1").execute();
        assertEquals(1, (int) response3.getContentAsMap().get("total"));
    }

    @Test
    public void test_ngQuery() throws Exception {
        Curl.post(runner.masterNode(), "fess/_fsuggest/create").execute();
        Curl.post(runner.masterNode(), "fess/_fsuggest/update/searchword").body(
            "{\n" +
                "\"keyword\" : \"検索エンジン\",\n" +
                "\"fields\" : [\"aaa\", \"bbb\"],\n" +
                "\"tags\" : [\"tag1\", \"tag2\"],\n" +
                "\"roles\" : [\"role1\", \"role2\"]\n" +
                "}"
        ).execute();
        runner.refresh();

        CurlResponse response1 = Curl.get(runner.masterNode(), "fess/_fsuggest")
            .param("q", "ke").param("roles", "role1").execute();
        assertEquals(1, (int) response1.getContentAsMap().get("total"));

        CurlResponse response2 = Curl.get(runner.masterNode(), "fess/_fsuggest")
            .param("q", "k").param("roles", "role1").execute();
        assertEquals(0, (int) response2.getContentAsMap().get("total"));

        CurlResponse response3 = Curl.get(runner.masterNode(), "fess/_fsuggest")
            .param("q", "ken").param("roles", "role1").execute();
        assertEquals(0, (int) response3.getContentAsMap().get("total"));

    }

    @Test
    public void test_popularWords() throws Exception {
        final int docNum = 10;

        Curl.post(runner.masterNode(), "fess/_fsuggest/create").execute();
        for (int i = 0; i < docNum; i++) {
            Curl.post(runner.masterNode(), "fess/_fsuggest/update/searchword").body(
                "{\n" +
                    "\"keyword\" : \"検索" + i + "\",\n" +
                    "\"fields\" : [\"aaa\", \"bbb\"]\n" +
                    "}"
            ).execute();
        }
        runner.refresh();

        CurlResponse response = Curl.get(runner.masterNode(), "fess/_fsuggest/pwords?query_freq=0")
            .execute();
        assertEquals(10, response.getContentAsMap().get("total"));
    }
}
