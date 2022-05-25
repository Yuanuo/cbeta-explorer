package org.appxi.cbeta.explorer.bookdata;

import appxi.cbeta.Book;
import org.appxi.cbeta.explorer.dao.BookdataType;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;

public class BookmarksController extends BookdataController {

    public BookmarksController(WorkbenchPane workbench) {
        this(workbench, null);
    }

    public BookmarksController(WorkbenchPane workbench, Book filterByBook) {
        super("BOOKMARKS", workbench, BookdataType.bookmark, filterByBook);
        this.setTitles("书签");
        this.graphic.set(MaterialIcon.BOOKMARK.graphic());
    }
}
