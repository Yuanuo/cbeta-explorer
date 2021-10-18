package org.appxi.cbeta.explorer.event;

import javafx.event.Event;
import javafx.event.EventType;

public class GenericEvent extends Event {
    private static final long serialVersionUID = 1L;

    public static final EventType<GenericEvent> PROFILE_READY = new EventType<>(Event.ANY, "PROFILE_READY");

    public static final EventType<GenericEvent> BEANS_READY = new EventType<>(Event.ANY, "BEANS_READY");

    public static final EventType<GenericEvent> DISPLAY_HAN_CHANGED = new EventType<>(Event.ANY, "DISPLAY_HAN_CHANGED");

    public static final EventType<GenericEvent> DISPLAY_ZOOM_CHANGED = new EventType<>(Event.ANY, "DISPLAY_ZOOM_CHANGED");

    public final Object data;

    public GenericEvent(EventType<GenericEvent> eventType) {
        this(eventType, null);
    }

    public GenericEvent(EventType<GenericEvent> eventType, Object data) {
        super(eventType);
        this.data = data;
    }
}
