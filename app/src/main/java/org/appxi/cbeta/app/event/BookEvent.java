package org.appxi.cbeta.app.event;

import org.appxi.book.Chapter;
import org.appxi.cbeta.Book;
import org.appxi.event.Event;
import org.appxi.event.EventType;

public class BookEvent extends Event {
    public static final EventType<BookEvent> OPEN = new EventType<>(Event.ANY);

    public static final EventType<BookEvent> CLOSE = new EventType<>(Event.ANY);

    public static final EventType<BookEvent> VIEW = new EventType<>(Event.ANY);

    public static final EventType<BookEvent> HIDE = new EventType<>(Event.ANY);

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
