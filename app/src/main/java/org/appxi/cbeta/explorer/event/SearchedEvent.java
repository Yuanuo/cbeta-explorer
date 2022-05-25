package org.appxi.cbeta.explorer.event;

import javafx.event.Event;
import javafx.event.EventType;
import org.appxi.search.solr.Piece;

public class SearchedEvent extends Event {
    private static final long serialVersionUID = 4935195001127503170L;

    public static final EventType<SearchedEvent> OPEN = new EventType<>(Event.ANY, "SEARCHED-OPEN");

    public final Piece piece;
    public final String highlightTerm, highlightSnippet;

    public SearchedEvent(Piece piece) {
        this(piece, null, null);
    }

    public SearchedEvent(Piece piece, String highlightTerm, String highlightSnippet) {
        super(OPEN);
        this.piece = piece;
        this.highlightTerm = highlightTerm;
        this.highlightSnippet = highlightSnippet;
    }
}
