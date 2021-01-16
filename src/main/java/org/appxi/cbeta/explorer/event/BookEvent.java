package org.appxi.cbeta.explorer.event;

import javafx.event.Event;
import javafx.event.EventType;
import org.appxi.tome.cbeta.CbetaBook;

public class BookEvent extends Event {
    private static final long serialVersionUID = 1L;

    public static final EventType<BookEvent> OPEN = new EventType<>(Event.ANY, "BOOK_OPEN");

    public static final EventType<BookEvent> CLOSE = new EventType<>(Event.ANY, "BOOK_CLOSE");

    public static final EventType<BookEvent> VIEW = new EventType<>(Event.ANY, "BOOK_VIEW");

    public final CbetaBook book;

    public BookEvent(EventType<BookEvent> eventType, CbetaBook book) {
        super(eventType);
        this.book = book;
    }

}
