package org.codelibs.elasticsearch.rest;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.index.SuggestIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.rest.*;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.rest.RestStatus.OK;

public class FessSuggestUpdateRestAction extends BaseRestHandler {
    public static final String PARAM_INDEX = "index";

    protected final ThreadPool threadPool;


    @Inject
    public FessSuggestUpdateRestAction(final Settings settings, final Client client,
                                       final RestController controller, final ThreadPool threadPool) {
        super(settings, controller, client);

        this.threadPool = threadPool;

        controller.registerHandler(RestRequest.Method.POST,
            "/{index}/_fsuggest/update", this);
        controller.registerHandler(RestRequest.Method.PUT,
            "/{index}/_fsuggest/update", this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleRequest(RestRequest restRequest, RestChannel restChannel, Client client) throws Exception {
        threadPool.executor(ThreadPool.Names.SUGGEST).execute(() -> {
            try {
                final String indexId = restRequest.param(PARAM_INDEX);

                final Suggester suggester = Suggester.builder().build(client, indexId);

                final String source = restRequest.content().toUtf8();
                final Map<String, Object> requestMap = XContentFactory
                    .xContent(source)
                    .createParser(source).mapAndClose();

                final Object keyword = requestMap.getOrDefault("keyword", "");
                if(Strings.isNullOrEmpty(keyword.toString())) {
                    restChannel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, "keyword is empyty."));
                    return;
                }

                final List<String> fields;
                final List<String> tags;
                final List<String> roles;

                final Object fieldsObj = requestMap.get("fields");
                if(fieldsObj != null && fieldsObj instanceof List) {
                    fields = (List)fieldsObj;
                } else {
                    fields = new ArrayList<>();
                }

                final Object tagsObj = requestMap.get("tags");
                if(tagsObj != null && tagsObj instanceof List) {
                    tags = (List)tagsObj;
                } else {
                    tags = new ArrayList<>();
                }

                final Object rolesObj = requestMap.get("roles");
                if(rolesObj != null && rolesObj instanceof List) {
                    roles = (List)rolesObj;
                } else {
                    roles = new ArrayList<>();
                }

                final SuggestIndexResponse suggestIndexResponse = suggester.indexer()
                    .indexFromSearchWord(
                        keyword.toString(),
                        fields.toArray(new String[fields.size()]),
                        tags.toArray(new String[tags.size()]),
                        roles.toArray(new String[roles.size()]),
                        1);
                suggestIndexResponse.getTook();
                final XContentBuilder builder = JsonXContent.contentBuilder();
                builder.startObject();
                builder.field("took", suggestIndexResponse.getTook());
                builder.field("acknowledged", true);
                builder.endObject();
                restChannel.sendResponse(new BytesRestResponse(OK, builder));
            } catch (Throwable t) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                restChannel.sendResponse(new BytesRestResponse(OK, sw.toString()));
            }
        });
    }
}
