package org.codelibs.elasticsearch;

public class ElasticsearchFessSuggestException extends Exception {
    public ElasticsearchFessSuggestException() {
        super();
    }

    public ElasticsearchFessSuggestException(final String msg) {
        super(msg);
    }

    public ElasticsearchFessSuggestException(final Throwable t) {
        super(t);
    }

    public ElasticsearchFessSuggestException(final String msg, final Throwable t) {
        super(msg, t);
    }
}
