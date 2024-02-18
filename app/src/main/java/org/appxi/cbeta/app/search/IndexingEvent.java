package org.appxi.cbeta.app.search;

import org.appxi.event.Event;
import org.appxi.event.EventType;

public class IndexingEvent extends Event {
    public static final EventType<IndexingEvent> START = new EventType<>(Event.ANY, "INDEX_START", true);
    public static final EventType<IndexingEvent> STATUS = new EventType<>(Event.ANY, "INDEX_STATUS", true);
    public static final EventType<IndexingEvent> STOP = new EventType<>(Event.ANY, "INDEX_STOP", true);

    public final int step, steps;
    public final String message;

    public IndexingEvent(EventType<IndexingEvent> eventType) {
        this(eventType, Integer.MAX_VALUE, Integer.MAX_VALUE, "");
    }

    public IndexingEvent(EventType<IndexingEvent> eventType, int step, int steps, String message) {
        super(eventType);
        this.step = step;
        this.steps = steps;
        this.message = message;
    }

}
