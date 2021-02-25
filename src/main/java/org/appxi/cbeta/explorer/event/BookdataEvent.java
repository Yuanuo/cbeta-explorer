package org.appxi.cbeta.explorer.event;

import javafx.event.Event;
import javafx.event.EventType;
import org.appxi.cbeta.explorer.dao.Bookdata;

public class BookdataEvent extends Event {
    private static final long serialVersionUID = 248520055158248448L;

    public static final EventType<BookdataEvent> REMOVED = new EventType<>(Event.ANY, "BOOKDATA-REMOVED");

    public static final EventType<BookdataEvent> CREATED = new EventType<>(Event.ANY, "BOOKDATA-CREATED");

    public final Bookdata data;

    public BookdataEvent(EventType<BookdataEvent> eventType, Bookdata data) {
        super(eventType);
        this.data = data;
    }
}
