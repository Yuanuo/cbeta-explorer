package org.appxi.cbeta.explorer.event;

import javafx.event.Event;
import javafx.event.EventType;
import org.appxi.cbeta.explorer.dao.Bookdata;

public class BookdataEvent extends Event {
    private static final long serialVersionUID = 248520055158248448L;

    public static final EventType<BookdataEvent> REMOVE = new EventType<>(Event.ANY, "BOOKDATA-REMOVE");

    public static final EventType<BookdataEvent> CREATE = new EventType<>(Event.ANY, "BOOKDATA-CREATE");

    public final Bookdata data;

    public BookdataEvent(EventType<BookdataEvent> eventType, Bookdata data) {
        super(eventType);
        this.data = data;
    }
}
