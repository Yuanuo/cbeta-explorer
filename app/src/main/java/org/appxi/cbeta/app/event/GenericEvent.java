package org.appxi.cbeta.app.event;

import org.appxi.event.Event;
import org.appxi.event.EventType;

public class GenericEvent extends Event {
    public static final EventType<GenericEvent> PROFILE_READY = new EventType<>(Event.ANY, "PROFILE_READY");

    public static final EventType<GenericEvent> BEANS_READY = new EventType<>(Event.ANY, "BEANS_READY", true);

    public static final EventType<GenericEvent> BOOK_LABEL_STYLED = new EventType<>(Event.ANY, "BOOK_LABEL_STYLED");

    public GenericEvent(EventType<GenericEvent> eventType) {
        this(eventType, null);
    }

    public GenericEvent(EventType<GenericEvent> eventType, Object data) {
        super(eventType, data);
    }
}
