package org.codelibs.elasticsearch.rest;

import static org.elasticsearch.rest.RestStatus.OK;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.core.lang.StringUtil;
import org.codelibs.fess.suggest.index.SuggestIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.threadpool.ThreadPool;

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
    protected void handleRequest(final RestRequest restRequest, final RestChannel restChannel, final Client client) throws Exception {
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

                final String[] fields;
                final String[] tags;
                final String[] roles;

                final Object fieldsObj = requestMap.get("fields");
                if(fieldsObj instanceof List) {
                    fields = ((List<String>)fieldsObj).stream().toArray(n->new String[n]);
                } else {
                    fields = StringUtil.EMPTY_STRINGS;
                }

                final Object tagsObj = requestMap.get("tags");
                if(tagsObj instanceof List) {
                    tags = ((List<String>)tagsObj).stream().toArray(n->new String[n]);
                } else {
                    tags = StringUtil.EMPTY_STRINGS;
                }

                final Object rolesObj = requestMap.get("roles");
                if(rolesObj instanceof List) {
                    roles = ((List<String>)rolesObj).stream().toArray(n->new String[n]);
                } else {
                    roles = StringUtil.EMPTY_STRINGS;
                }

                final SuggestIndexResponse suggestIndexResponse = suggester.indexer()
                    .indexFromSearchWord(
                        keyword.toString(),
                        fields,
                        tags,
                        roles,
                        1);
                suggestIndexResponse.getTook();
                final XContentBuilder builder = JsonXContent.contentBuilder();
                final String pretty = restRequest.param("pretty");
                if (pretty != null && !"false".equalsIgnoreCase(pretty)) {
                    builder.prettyPrint().lfAtEnd();
                }
                builder.startObject();
                builder.field("took", suggestIndexResponse.getTook());
                builder.field("acknowledged", true);
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
