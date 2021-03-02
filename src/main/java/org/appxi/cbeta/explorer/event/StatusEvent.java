package org.appxi.cbeta.explorer.event;

import javafx.event.Event;
import javafx.event.EventType;

public class StatusEvent extends Event {
    private static final long serialVersionUID = 1L;

    public static final EventType<StatusEvent> BOOKS_READY = new EventType<>(Event.ANY, "BOOKS_READY");

    public static final EventType<StatusEvent> BEANS_READY = new EventType<>(Event.ANY, "BEANS_READY");

    public StatusEvent(EventType<StatusEvent> eventType) {
        super(eventType);
    }
}
