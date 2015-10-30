package org.codelibs.elasticsearch.service;

import org.codelibs.fess.suggest.Suggester;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FessSuggestService extends AbstractLifecycleComponent<FessSuggestService> {
    protected Client client;
    protected Map<String, Suggester> suggesters = new ConcurrentHashMap<>();

    @Inject
    public FessSuggestService(final Settings settings, final Client client) {
        super(settings);
        logger.info("CREATE AbTestService");
        this.client = client;
    }

    @Override
    protected void doStart() throws ElasticsearchException {

    }

    @Override
    protected void doStop() throws ElasticsearchException {

    }

    @Override
    protected void doClose() throws ElasticsearchException {

    }

    public Suggester suggester(final String id) {
        Suggester suggester = suggesters.get(id);
        if(suggester != null) {
            return suggester;
        }

        synchronized (this) {
            if(suggesters.get(id) == null) {
                suggesters.put(id, Suggester.builder().build(client, id));
            }
        }
        return suggesters.get(id);
    }

    public synchronized void deleteSuggester(final String id) {
        suggesters.remove(id);
    }
}
