package org.appxi.cbeta.explorer.event;

import appxi.cbeta.Book;
import javafx.event.Event;
import javafx.event.EventType;

public class SearcherEvent extends Event {
    private static final long serialVersionUID = 248520055158248448L;

    public static final EventType<SearcherEvent> LOOKUP = new EventType<>(Event.ANY, "LOOKUP");

    public static final EventType<SearcherEvent> SEARCH = new EventType<>(Event.ANY, "SEARCH");

    public final String text;
    public final Book scope;

    public SearcherEvent(EventType<SearcherEvent> eventType, String text, Book scope) {
        super(eventType);
        this.text = text;
        this.scope = scope;
    }

    public static SearcherEvent ofLookup(String text) {
        return new SearcherEvent(LOOKUP, text, null);
    }

    public static SearcherEvent ofSearch(String text) {
        return new SearcherEvent(SEARCH, text, null);
    }

    public static SearcherEvent ofSearch(String text, Book scope) {
        return new SearcherEvent(SEARCH, text, scope);
    }
}
