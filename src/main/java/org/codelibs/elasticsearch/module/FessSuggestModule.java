package org.codelibs.elasticsearch.module;

import org.codelibs.elasticsearch.service.FessSuggestService;
import org.elasticsearch.common.inject.AbstractModule;

public class FessSuggestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(FessSuggestService.class).asEagerSingleton();
    }
}