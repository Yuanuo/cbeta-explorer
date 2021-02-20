package org.appxi.cbeta.explorer.event;

import javafx.event.Event;
import javafx.event.EventType;

public class ProgressEvent extends Event {
    private static final long serialVersionUID = 248520055158248448L;

    public static final EventType<ProgressEvent> INDEXING = new EventType<>(Event.ANY, "INDEXING");

    public final int step, steps;
    public final String message;

    public ProgressEvent(EventType<ProgressEvent> eventType, int step, int steps, String message) {
        super(eventType);
        this.step = step;
        this.steps = steps;
        this.message = message;
    }

    public boolean isFinished() {
        return this.step == steps;
    }
}
