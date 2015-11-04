package org.codelibs.elasticsearch;

import org.codelibs.elasticsearch.module.FessSuggestModule;
import org.codelibs.elasticsearch.rest.PopularWordsRestAction;
import org.codelibs.elasticsearch.rest.FessSuggestCreateRestAction;
import org.codelibs.elasticsearch.rest.FessSuggestRestAction;
import org.codelibs.elasticsearch.rest.FessSuggestUpdateRestAction;
import org.codelibs.elasticsearch.service.FessSuggestService;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;

import java.util.Collection;

public class FessSuggestPlugin extends AbstractPlugin {
    @Override
    public String name() {
        return "FessSuggestPlugin";
    }

    @Override
    public String description() {
        return "This is a elasticsearch-fess-suggest plugin.";
    }

    // for Rest API
    public void onModule(final RestModule module) {
        module.addRestAction(FessSuggestRestAction.class);
        module.addRestAction(FessSuggestCreateRestAction.class);
        module.addRestAction(FessSuggestUpdateRestAction.class);
        module.addRestAction(PopularWordsRestAction.class);
    }

    // for Service
    @Override
    public Collection<Class<? extends Module>> modules() {
        final Collection<Class<? extends Module>> modules = Lists
            .newArrayList();
        modules.add(FessSuggestModule.class);
        return modules;
    }

    // for Service
    @SuppressWarnings("rawtypes")
    @Override
    public Collection<Class<? extends LifecycleComponent>> services() {
        final Collection<Class<? extends LifecycleComponent>> services = Lists
            .newArrayList();
        services.add(FessSuggestService.class);
        return services;
    }

}
