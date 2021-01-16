package org.appxi.cbeta.explorer.event;

import javafx.event.Event;
import javafx.event.EventType;

public class DataEvent extends Event {
    private static final long serialVersionUID = 1L;

    public static final EventType<DataEvent> BOOKS_READY = new EventType<>(Event.ANY, "BOOKS_READY");

    public static final EventType<DataEvent> SEARCH_READY = new EventType<>(Event.ANY, "SEARCH_READY");

    public static final EventType<DataEvent> SEARCH_OPEN = new EventType<>(Event.ANY, "SEARCH_OPEN");

    public DataEvent(EventType<DataEvent> eventType) {
        super(eventType);
    }
}
