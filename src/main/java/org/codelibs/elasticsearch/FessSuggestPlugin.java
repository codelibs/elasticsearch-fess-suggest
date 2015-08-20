package org.codelibs.elasticsearch;

import org.codelibs.elasticsearch.rest.FessSuggestCreateRestAction;
import org.codelibs.elasticsearch.rest.FessSuggestRestAction;
import org.codelibs.elasticsearch.rest.FessSuggestUpdateRestAction;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;

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
    }

}
