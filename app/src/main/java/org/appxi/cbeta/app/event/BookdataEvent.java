package org.appxi.cbeta.app.event;

import org.appxi.cbeta.app.dao.Bookdata;
import org.appxi.event.Event;
import org.appxi.event.EventType;

public class BookdataEvent extends Event {
    public static final EventType<BookdataEvent> REMOVED = new EventType<>(Event.ANY);

    public static final EventType<BookdataEvent> CREATED = new EventType<>(Event.ANY);

    public final Bookdata data;

    public BookdataEvent(EventType<BookdataEvent> eventType, Bookdata data) {
        super(eventType);
        this.data = data;
    }
}
