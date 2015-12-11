package org.codelibs.elasticsearch.rest;

import com.google.common.base.Strings;
import org.codelibs.elasticsearch.service.FessSuggestService;
import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.popularwords.PopularWordsRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.rest.*;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.elasticsearch.rest.RestStatus.OK;

public class PopularWordsRestAction extends BaseRestHandler {

    public static final String PARAM_INDEX = "index";
    public static final String PARAM_WINDOW_SIZE = "window_size";
    public static final String PARAM_SIZE = "size";
    public static final String PARAM_TAGS = "tags";
    public static final String PARAM_ROLES = "roles";
    public static final String PARAM_FIELDS = "fields";
    public static final String PARAM_SEED = "seed";
    public static final String PARAM_EXCLUDES = "excludes";

    public static final String SETTINGS_EXCLUDE_WORDS = "fsuggest.pwords.excludes";
    public static final String SETTINGS_WINDOW_SIZE = "fsuggest.pwords.window_size";

    private static final String SEP_PARAM = ",";

    protected final ThreadPool threadPool;

    protected final FessSuggestService fessSuggestService;

    protected final Set<String> excludeWordSet;

    protected final int windowSize;

    @Inject
    public PopularWordsRestAction(final Settings settings, final Client client,
                                 final RestController controller, final ThreadPool threadPool, final FessSuggestService fessSuggestService) {
        super(settings, controller, client);

        this.threadPool = threadPool;

        this.fessSuggestService = fessSuggestService;

        controller.registerHandler(RestRequest.Method.GET,
            "/{index}/_fsuggest/pwords", this);
        controller.registerHandler(RestRequest.Method.GET,
            "/{index}/{type}/_fsuggest/pwords", this);

        excludeWordSet = getExcludeWordSet(settings);
        windowSize = settings.getAsInt(SETTINGS_WINDOW_SIZE, 20);
    }

    @Override
    protected void handleRequest(final RestRequest request,
                                 final RestChannel channel, final Client client) {
        threadPool.executor(ThreadPool.Names.SUGGEST).execute(() -> {
            try {
                final String index = request.param(PARAM_INDEX);
                final int size = request.paramAsInt(PARAM_SIZE, 10);
                final int windowSize = request.paramAsInt(PARAM_WINDOW_SIZE, this.windowSize);
                final String tags = request.param(PARAM_TAGS);
                final String roles = request.param(PARAM_ROLES);
                final String fields = request.param(PARAM_FIELDS);
                final String seed = request.param(PARAM_SEED);
                final String excludes = request.param(PARAM_EXCLUDES);

                final Suggester suggester = fessSuggestService.suggester(index);
                final PopularWordsRequestBuilder popularWordsRequestBuilder = suggester.popularWords().setSize(size).setWindowSize(windowSize);
                if (!Strings.isNullOrEmpty(tags)) {
                    final String[] tagsArray = tags.split(SEP_PARAM);
                    for (final String tag : tagsArray) {
                        popularWordsRequestBuilder.addTag(tag);
                    }
                }
                if (!Strings.isNullOrEmpty(roles)) {
                    final String[] rolesArray = roles.split(SEP_PARAM);
                    for (final String role : rolesArray) {
                        popularWordsRequestBuilder.addRole(role);
                    }
                }
                if (!Strings.isNullOrEmpty(fields)) {
                    final String[] fieldsArray = fields.split(SEP_PARAM);
                    for (final String field : fieldsArray) {
                        popularWordsRequestBuilder.addRole(field);
                    }
                }
                if (!Strings.isNullOrEmpty(seed)) {
                    popularWordsRequestBuilder.setSeed(seed);
                }
                if (!Strings.isNullOrEmpty(excludes)) {
                    final String[] excludesArray = excludes.split(SEP_PARAM);
                    for (final String excludeWord : excludesArray) {
                        popularWordsRequestBuilder.addExcludeWord(excludeWord);
                    }
                }
                if(!excludeWordSet.isEmpty()) {
                    for (final String excludeWord: excludeWordSet) {
                        popularWordsRequestBuilder.addExcludeWord(excludeWord);
                    }
                }

                popularWordsRequestBuilder.execute()
                    .then(r -> {
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

    protected Set<String> getExcludeWordSet(final Settings settings) {
        final Set<String> excludeWords = Collections.synchronizedSet(new HashSet<>());
        final String value = settings.get(SETTINGS_EXCLUDE_WORDS);
        if(Strings.isNullOrEmpty(value)) {
            return excludeWords;
        }

        final String[] values = value.split(",");
        for(final String excludeWord: values) {
            if(excludeWord.length() == 0) {
                continue;
            }
            excludeWords.add(excludeWord);
        }
        return excludeWords;
    }
}
