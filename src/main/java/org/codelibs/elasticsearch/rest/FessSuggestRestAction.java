package org.codelibs.elasticsearch.rest;

import static org.elasticsearch.rest.RestStatus.OK;

import java.io.IOException;
import java.util.List;

import org.codelibs.fess.suggest.Suggester;
import org.codelibs.fess.suggest.entity.SuggestItem;
import org.codelibs.fess.suggest.exception.SuggesterException;
import org.codelibs.fess.suggest.request.suggest.SuggestRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.base.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;

public class FessSuggestRestAction extends BaseRestHandler {

    public static final String PARAM_INDEX = "index";
    public static final String PARAM_QUERY = "q";
    public static final String PARAM_SIZE = "size";
    public static final String PARAM_TAGS = "tags";
    public static final String PARAM_ROLES = "roles";
    public static final String PARAM_FIELDS = "fields";

    private static final String SEP_PARAM = ",";

    @Inject
    public FessSuggestRestAction(final Settings settings, final Client client,
            final RestController controller) {
        super(settings, controller, client);

        controller.registerHandler(RestRequest.Method.GET,
            "/{index}/_fsuggest", this);
        controller.registerHandler(RestRequest.Method.GET,
            "/{index}/{type}/_fsuggest", this);
    }

    @Override
    protected void handleRequest(final RestRequest request,
            final RestChannel channel, Client client) {
        try {
            final String index = request.param(PARAM_INDEX);
            final String query = request.param(PARAM_QUERY);
            final int size = request.paramAsInt(PARAM_SIZE, 10);
            final String tags = request.param(PARAM_TAGS);
            final String roles = request.param(PARAM_ROLES);
            final String fields = request.param(PARAM_FIELDS);

            final Suggester suggester = Suggester.builder().build(client, index);
            final SuggestRequestBuilder suggestRequestBuilder = suggester.suggest().setSize(size);
            if(!Strings.isNullOrEmpty(query)) {
                suggestRequestBuilder.setQuery(query);
            }
            if(!Strings.isNullOrEmpty(tags)) {
                final String[] tagsArray = tags.split(SEP_PARAM);
                for(final String tag: tagsArray) {
                    suggestRequestBuilder.addTag(tag);
                }
            }
            if(!Strings.isNullOrEmpty(roles)) {
                final String[] rolesArray = roles.split(SEP_PARAM);
                for(final String role: rolesArray) {
                    suggestRequestBuilder.addRole(role);
                }
            }
            if(!Strings.isNullOrEmpty(fields)) {
                final String[] fieldsArray = fields.split(SEP_PARAM);
                for(final String field: fieldsArray) {
                    suggestRequestBuilder.addRole(field);
                }
            }

            suggestRequestBuilder.execute()
                .done(r -> {
                    try {
                        final XContentBuilder builder = JsonXContent.contentBuilder();
                        builder.startObject();
                        builder.field("index", request.param("index"));
                        builder.field("took", r.getTookMs());
                        builder.field("total", r.getTotal());
                        List<SuggestItem> suggestItems = r.getItems();
                        if (suggestItems.size() > 0) {
                            builder.startArray("hits");
                            for (SuggestItem item : suggestItems) {
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
                    } catch (IOException e) {
                        try {
                            channel.sendResponse(new BytesRestResponse(channel, e));
                        } catch (final IOException e1) {
                            logger.error("Failed to send a failure response.", e1);
                        }
                    }
                }).error(t -> {
                try {
                    channel.sendResponse(new BytesRestResponse(channel, t));
                } catch (final IOException e1) {
                    logger.error("Failed to send a failure response.", e1);
                }
            });
            } catch (SuggesterException e) {
            try {
                channel.sendResponse(new BytesRestResponse(channel, e));
            } catch (final IOException e1) {
                logger.error("Failed to send a failure response.", e1);
            }
        }
    }

}
