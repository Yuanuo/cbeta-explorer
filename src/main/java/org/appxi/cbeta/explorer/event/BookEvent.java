package org.appxi.cbeta.explorer.event;

import javafx.event.Event;
import javafx.event.EventType;
import org.appxi.tome.cbeta.CbetaBook;

public class BookEvent extends Event {
    private static final long serialVersionUID = 1L;

    public static final EventType<BookEvent> OPEN = new EventType<>(Event.ANY, "BOOK_OPEN");

    public static final EventType<BookEvent> CLOSE = new EventType<>(Event.ANY, "BOOK_CLOSE");

    public static final EventType<BookEvent> VIEW = new EventType<>(Event.ANY, "BOOK_VIEW");

    public static final EventType<BookEvent> HIDE = new EventType<>(Event.ANY, "BOOK_HIDE");

    public final CbetaBook book;
    public final Object data;

    public BookEvent(EventType<BookEvent> eventType, CbetaBook book) {
        this(eventType, book, null);
    }

    public BookEvent(EventType<BookEvent> eventType, CbetaBook book, Object data) {
        super(eventType);
        this.book = book;
        this.data = data;
    }

}
