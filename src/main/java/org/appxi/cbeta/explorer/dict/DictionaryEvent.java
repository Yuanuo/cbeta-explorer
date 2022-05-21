package org.appxi.cbeta.explorer.dict;

import javafx.event.Event;
import javafx.event.EventType;

public class DictionaryEvent extends Event {
    public static final EventType<DictionaryEvent> SEARCH = new EventType<>(Event.ANY, "SEARCH_DICT");
    public static final EventType<DictionaryEvent> SEARCH_EXACT = new EventType<>(Event.ANY, "SEARCH_DICT_EXACT");

    public final String dictionary, text;

    public DictionaryEvent(EventType<DictionaryEvent> eventType, String dictionary, String text) {
        super(eventType);
        this.dictionary = dictionary;
        this.text = text;
    }

    public static DictionaryEvent ofSearch(String text) {
        return new DictionaryEvent(SEARCH, null, text);
    }

    public static DictionaryEvent ofSearchExact(String dictionary, String text) {
        return new DictionaryEvent(SEARCH_EXACT, dictionary, text);
    }
}
