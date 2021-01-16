package org.appxi.cbeta.explorer.event;

import javafx.event.Event;
import javafx.event.EventType;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.model.Chapter;

public class ChapterEvent extends Event {
    private static final long serialVersionUID = 1L;

    public static final EventType<ChapterEvent> OPEN = new EventType<>(Event.ANY, "CHAPTER_OPEN");

    public static final EventType<ChapterEvent> OPENED = new EventType<>(Event.ANY, "CHAPTER_OPENED");

    public static final EventType<ChapterEvent> CLOSED = new EventType<>(Event.ANY, "CHAPTER_CLOSED");

    public final CbetaBook book;
    public final Chapter chapter;

    public ChapterEvent(EventType<? extends Event> eventType, CbetaBook book, Chapter chapter) {
        super(eventType);
        this.book = book;
        this.chapter = chapter;
    }

}
