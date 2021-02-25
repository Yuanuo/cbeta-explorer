package org.appxi.cbeta.explorer.event;

import javafx.event.Event;
import javafx.event.EventType;

public class SearcherEvent extends Event {
    private static final long serialVersionUID = 248520055158248448L;

    public static final EventType<SearcherEvent> LOOKUP = new EventType<>(Event.ANY, "LOOKUP");

    public static final EventType<SearcherEvent> SEARCH = new EventType<>(Event.ANY, "SEARCH");

    public final String text;

    public SearcherEvent(EventType<SearcherEvent> eventType, String text) {
        super(eventType);
        this.text = text;
    }

    public static SearcherEvent ofLookup(String text) {
        return new SearcherEvent(LOOKUP, text);
    }

    public static SearcherEvent ofSearch(String text) {
        return new SearcherEvent(SEARCH, text);
    }
}
