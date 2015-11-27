package org.codelibs.elasticsearch.rest;

import static org.elasticsearch.rest.RestStatus.OK;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import org.codelibs.elasticsearch.ElasticsearchFessSuggestException;
import org.codelibs.elasticsearch.service.FessSuggestService;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.core.lang.StringUtil;
import org.codelibs.fess.suggest.index.SuggestIndexResponse;
import org.elasticsearch.client.Client;
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
import org.elasticsearch.threadpool.ThreadPool;

public class FessSuggestUpdateRestAction extends BaseRestHandler {

    protected final ThreadPool threadPool;

    protected final FessSuggestService fessSuggestService;


    @Inject
    public FessSuggestUpdateRestAction(final Settings settings, final Client client,
                                       final RestController controller, final ThreadPool threadPool, final FessSuggestService fessSuggestService) {
        super(settings, controller, client);

        this.threadPool = threadPool;

        this.fessSuggestService = fessSuggestService;

        controller.registerHandler(RestRequest.Method.POST,
            "/{index}/_fsuggest/update/{update_type}", this);
        controller.registerHandler(RestRequest.Method.PUT,
            "/{index}/_fsuggest/update/{update_type}", this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleRequest(final RestRequest restRequest, final RestChannel restChannel, final Client client) throws Exception {
        threadPool.executor(ThreadPool.Names.SUGGEST).execute(() -> {
            try {
                final String indexId = restRequest.param("index");
                final String updateType = restRequest.param("update_type");

                final Suggester suggester = Suggester.builder().build(client, indexId);

                final String source = restRequest.content().toUtf8();
                final Map<String, Object> requestMap = XContentFactory
                    .xContent(source)
                    .createParser(source).map();

                final String[] fields;
                final String[] tags;
                final String[] roles;

                final Object fieldsObj = requestMap.get("fields");
                if (fieldsObj instanceof List) {
                    fields = ((List<String>) fieldsObj).stream().toArray(n -> new String[n]);
                } else {
                    fields = StringUtil.EMPTY_STRINGS;
                }

                final Object tagsObj = requestMap.get("tags");
                if (tagsObj instanceof List) {
                    tags = ((List<String>) tagsObj).stream().toArray(n -> new String[n]);
                } else {
                    tags = StringUtil.EMPTY_STRINGS;
                }

                final Object rolesObj = requestMap.get("roles");
                if (rolesObj instanceof List) {
                    roles = ((List<String>) rolesObj).stream().toArray(n -> new String[n]);
                } else {
                    roles = StringUtil.EMPTY_STRINGS;
                }


                final Consumer<SuggestIndexResponse> success = suggestIndexResponse -> {
                        try {
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
                        } catch (Exception e) {
                            sendErrorResponse(restChannel, e);
                        }
                    };

                final Consumer<Throwable> error = t -> sendErrorResponse(restChannel, t);

                if(updateType.equals("searchword")) {
                    updateFromSearchWord(suggester, fields, tags, roles, requestMap, success, error);
                } else if (updateType.equals("document")) {
                    updateFromDocument(suggester, fields, tags, roles, requestMap, success, error);
                } else {
                    throw new ElasticsearchFessSuggestException("Unexpected update type: " + updateType);
                }
            } catch (final Throwable t) {
                sendErrorResponse(restChannel, t);
            }
        });
    }

    private void updateFromSearchWord(final Suggester suggester, final String[] fields, final String[] tags, final String roles[],
                                    final Map<String, Object> requestMap, final Consumer<SuggestIndexResponse> success, final Consumer<Throwable> error) {
        final Object keyword = requestMap.getOrDefault("keyword", "");
        if (Strings.isNullOrEmpty(keyword.toString())) {
            error.accept(new ElasticsearchFessSuggestException("keyword is null."));
            return;
        }

        try {
            final SuggestIndexResponse suggestIndexResponse = suggester.indexer()
                .indexFromSearchWord(
                    keyword.toString(),
                    fields,
                    tags,
                    roles,
                    1);

            success.accept(suggestIndexResponse);
        } catch (Exception e) {
            error.accept(e);
        }
    }

    private void updateFromDocument(final Suggester suggester, final String[] fields, final String[] tags, final String roles[],
                                      final Map<String, Object> requestMap, final Consumer<SuggestIndexResponse> success, final Consumer<Throwable> error) {
        final Object document = requestMap.get("document");
        if (Strings.isNullOrEmpty(document.toString())) {
            error.accept(new ElasticsearchFessSuggestException("document is null."));
            return;
        }

        try {
            final Map<String, Object> doc = new HashMap<>();
            final String[] supportedFields = suggester.settings().array().get("supportedFields");
            final List<String> supportedFieldList = Arrays.asList(supportedFields);
            Stream.of(fields)
                .filter(field -> !supportedFieldList.contains(field))
                .forEach(field -> suggester.settings().array().add("supportedFields", field));

            Stream.of(fields).forEach(field -> doc.put(field, document));

            //TODO tags & role
            final SuggestIndexResponse suggestIndexResponse = suggester.indexer().indexFromDocument(new Map[]{doc});
            success.accept(suggestIndexResponse);
        } catch (Exception e) {
            error.accept(e);
        }
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
