package org.appxi.cbeta.explorer.model;

import org.appxi.tome.cbeta.BookMap;
import org.appxi.tome.cbeta.CbetaBook;

public class BookList extends BookMap {
    public static final BookList books = new BookList();

    private BookList() {
        super();
    }

    public static CbetaBook getById(String id) {
        return books.getDataMap().get(id);
    }
}
