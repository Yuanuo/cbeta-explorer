package org.appxi.cbeta.app.reader;

import org.appxi.cbeta.Book;
import org.appxi.cbeta.app.dao.BookdataType;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;

public class BookmarksController extends BookDataController {

    public BookmarksController(WorkbenchPane workbench) {
        this(workbench, null);
    }

    public BookmarksController(WorkbenchPane workbench, Book filterByBook) {
        super("BOOKMARKS", workbench, BookdataType.bookmark, filterByBook);
        this.setTitles("书签");
        this.graphic.set(MaterialIcon.BOOKMARK.graphic());
    }
}
