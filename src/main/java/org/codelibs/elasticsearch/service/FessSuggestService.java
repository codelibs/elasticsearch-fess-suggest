package org.codelibs.elasticsearch.service;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

public class FessSuggestService extends AbstractLifecycleComponent<FessSuggestService> {

    @Inject
    public FessSuggestService(final Settings settings) {
        super(settings);
        logger.info("CREATE FessSuggestService");

        // TODO Your code..
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        logger.info("START FessSuggestService");

        // TODO Your code..
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        logger.info("STOP FessSuggestService");

        // TODO Your code..
    }

    @Override
    protected void doClose() throws ElasticsearchException {
        logger.info("CLOSE FessSuggestService");

        // TODO Your code..
    }

}
