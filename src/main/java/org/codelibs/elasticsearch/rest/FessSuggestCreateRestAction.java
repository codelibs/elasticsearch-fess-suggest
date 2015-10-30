package org.codelibs.elasticsearch.rest;

import static org.elasticsearch.rest.RestStatus.OK;

import java.io.IOException;

import org.codelibs.elasticsearch.service.FessSuggestService;
import org.codelibs.fess.suggest.Suggester;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.threadpool.ThreadPool;

public class FessSuggestCreateRestAction extends BaseRestHandler {
    public static final String PARAM_INDEX = "index";

    protected final ThreadPool threadPool;

    protected final FessSuggestService fessSuggestService;

    @Inject
    public FessSuggestCreateRestAction(final Settings settings, final Client client,
                                       final RestController controller, final ThreadPool threadPool, final FessSuggestService fessSuggestService) {
        super(settings, controller, client);

        this.threadPool = threadPool;

        this.fessSuggestService = fessSuggestService;

        controller.registerHandler(RestRequest.Method.POST,
            "/{index}/_fsuggest/create", this);
        controller.registerHandler(RestRequest.Method.PUT,
            "/{index}/_fsuggest/create", this);
    }

    @Override
    protected void handleRequest(final RestRequest restRequest, final RestChannel restChannel, final Client client) throws Exception {
        threadPool.executor(ThreadPool.Names.SUGGEST).execute(() -> {
            try {
                final String indexId = restRequest.param(PARAM_INDEX);

                fessSuggestService.deleteSuggester(indexId);
                final Suggester suggester = fessSuggestService.suggester(indexId);
                final boolean created = suggester.createIndexIfNothing();

                final XContentBuilder builder = JsonXContent.contentBuilder();
                final String pretty = restRequest.param("pretty");
                if (pretty != null && !"false".equalsIgnoreCase(pretty)) {
                    builder.prettyPrint().lfAtEnd();
                }
                builder.startObject();
                builder.field("acknowledged", created);
                builder.endObject();
                restChannel.sendResponse(new BytesRestResponse(OK, builder));
            } catch (final Throwable t) {
                sendErrorResponse(restChannel, t);
            }
        });
    }

    private void sendErrorResponse(final RestChannel channel, final Throwable t) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to process the request.", t);
            }
            channel.sendResponse(new BytesRestResponse(channel, t));
        } catch (final IOException e) {
            logger.error("Failed to send a failure response.", e);
        }
    }

}
