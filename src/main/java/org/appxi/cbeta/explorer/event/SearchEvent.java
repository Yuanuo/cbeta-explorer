package org.appxi.cbeta.explorer.event;

import javafx.event.Event;
import javafx.event.EventType;

public class SearchEvent extends Event {
    private static final long serialVersionUID = 248520055158248448L;

    public static final EventType<SearchEvent> LOOKUP = new EventType<>(Event.ANY, "LOOKUP");

    public static final EventType<SearchEvent> SEARCH = new EventType<>(Event.ANY, "SEARCH");

    public final String text;

    public SearchEvent(EventType<SearchEvent> eventType, String text) {
        super(eventType);
        this.text = text;
    }
}
