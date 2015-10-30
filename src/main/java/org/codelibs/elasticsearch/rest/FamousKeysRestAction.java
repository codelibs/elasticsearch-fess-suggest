package org.codelibs.elasticsearch.rest;

import org.codelibs.elasticsearch.service.FessSuggestService;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.famouskeys.FamousKeysRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.rest.*;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.rest.RestStatus.OK;

public class FamousKeysRestAction extends BaseRestHandler {

    public static final String PARAM_INDEX = "index";
    public static final String PARAM_WINDOW_SIZE = "window_size";
    public static final String PARAM_SIZE = "size";
    public static final String PARAM_TAGS = "tags";
    public static final String PARAM_ROLES = "roles";
    public static final String PARAM_FIELDS = "fields";

    private static final String SEP_PARAM = ",";

    protected final ThreadPool threadPool;

    protected final FessSuggestService fessSuggestService;

    @Inject
    public FamousKeysRestAction(final Settings settings, final Client client,
                                 final RestController controller, final ThreadPool threadPool, final FessSuggestService fessSuggestService) {
        super(settings, controller, client);

        this.threadPool = threadPool;

        this.fessSuggestService = fessSuggestService;

        controller.registerHandler(RestRequest.Method.GET,
            "/{index}/_famouskeys", this);
        controller.registerHandler(RestRequest.Method.GET,
            "/{index}/{type}/_famouskeys", this);
    }

    @Override
    protected void handleRequest(final RestRequest request,
                                 final RestChannel channel, final Client client) {
        threadPool.executor(ThreadPool.Names.SUGGEST).execute(() -> {
            try {
                final String index = request.param(PARAM_INDEX);
                final int size = request.paramAsInt(PARAM_SIZE, 10);
                final int windowSize = request.paramAsInt(PARAM_WINDOW_SIZE, 20);
                final String tags = request.param(PARAM_TAGS);
                final String roles = request.param(PARAM_ROLES);
                final String fields = request.param(PARAM_FIELDS);

                final Suggester suggester = fessSuggestService.suggester(index);
                final FamousKeysRequestBuilder famousKeysRequestBuilder = suggester.famousKeys().setSize(size);
                if (!Strings.isNullOrEmpty(tags)) {
                    final String[] tagsArray = tags.split(SEP_PARAM);
                    for (final String tag : tagsArray) {
                        famousKeysRequestBuilder.addTag(tag);
                    }
                }
                if (!Strings.isNullOrEmpty(roles)) {
                    final String[] rolesArray = roles.split(SEP_PARAM);
                    for (final String role : rolesArray) {
                        famousKeysRequestBuilder.addRole(role);
                    }
                }
                if (!Strings.isNullOrEmpty(fields)) {
                    final String[] fieldsArray = fields.split(SEP_PARAM);
                    for (final String field : fieldsArray) {
                        famousKeysRequestBuilder.addRole(field);
                    }
                }

                famousKeysRequestBuilder.execute()
                    .done(r -> {
                        try {
                            final XContentBuilder builder = JsonXContent.contentBuilder();
                            final String pretty = request.param("pretty");
                            if (pretty != null && !"false".equalsIgnoreCase(pretty)) {
                                builder.prettyPrint().lfAtEnd();
                            }
                            builder.startObject();
                            builder.field("index", r.getIndex());
                            builder.field("took", r.getTookMs());
                            builder.field("total", r.getTotal());
                            builder.field("num", r.getNum());
                            final List<SuggestItem> suggestItems = r.getItems();
                            if (suggestItems.size() > 0) {
                                builder.startArray("hits");
                                for (final SuggestItem item : suggestItems) {
                                    builder.startObject();
                                    builder.field("text", item.getText());
                                    builder.array("tags", item.getTags());
                                    builder.array("roles", item.getRoles());
                                    builder.array("fields", item.getFields());
                                    builder.endObject();
                                }
                                builder.endArray();
                            }

                            builder.endObject();
                            channel.sendResponse(new BytesRestResponse(OK, builder));
                        } catch (final IOException e) {
                            sendErrorResponse(channel, e);
                        }
                    }).error(t ->
                        sendErrorResponse(channel, t)
                );
            } catch (final SuggesterException e) {
                sendErrorResponse(channel, e);
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
