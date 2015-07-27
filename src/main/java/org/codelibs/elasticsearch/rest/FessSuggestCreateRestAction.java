package org.codelibs.elasticsearch.rest;

import org.codelibs.fess.suggest.Suggester;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.rest.*;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.elasticsearch.rest.RestStatus.OK;

public class FessSuggestCreateRestAction extends BaseRestHandler {
    public static final String PARAM_INDEX = "index";

    protected final ThreadPool threadPool;

    @Inject
    public FessSuggestCreateRestAction(final Settings settings, final Client client,
                                       final RestController controller, final ThreadPool threadPool) {
        super(settings, controller, client);

        this.threadPool = threadPool;

        controller.registerHandler(RestRequest.Method.POST,
            "/{index}/_fsuggest/create", this);
        controller.registerHandler(RestRequest.Method.PUT,
            "/{index}/_fsuggest/create", this);
    }

    @Override
    protected void handleRequest(RestRequest restRequest, RestChannel restChannel, Client client) throws Exception {
        threadPool.executor(ThreadPool.Names.SUGGEST).execute(() -> {
            try {
                final String indexId = restRequest.param(PARAM_INDEX);
                Suggester suggester = Suggester.builder().build(client, indexId);
                boolean created = suggester.createIndexIfNothing();

                final XContentBuilder builder = JsonXContent.contentBuilder();
                builder.startObject();
                builder.field("acknowledged", created);
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
