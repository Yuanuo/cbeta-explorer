package org.appxi.cbeta.explorer.event;

import appxi.cbeta.Book;
import appxi.cbeta.Chapter;
import javafx.event.Event;
import javafx.event.EventType;

public class BookEvent extends Event {
    private static final long serialVersionUID = 1L;

    public static final EventType<BookEvent> OPEN = new EventType<>(Event.ANY, "BOOK_OPEN");

    public static final EventType<BookEvent> CLOSE = new EventType<>(Event.ANY, "BOOK_CLOSE");

    public static final EventType<BookEvent> VIEW = new EventType<>(Event.ANY, "BOOK_VIEW");

    public static final EventType<BookEvent> HIDE = new EventType<>(Event.ANY, "BOOK_HIDE");

    public final Book book;
    public final Chapter chapter;

    public BookEvent(EventType<BookEvent> eventType, Book book) {
        this(eventType, book, null);
    }

    public BookEvent(EventType<BookEvent> eventType, Book book, Chapter chapter) {
        super(eventType);
        this.book = book;
        this.chapter = chapter;
    }
}
