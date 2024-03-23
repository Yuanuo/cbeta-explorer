package org.appxi.cbeta.app.reader;

import org.appxi.cbeta.Book;
import org.appxi.cbeta.app.DataApp;
import org.appxi.cbeta.app.dao.BookdataType;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;

public class BookmarksController extends UserDataController {

    public BookmarksController(WorkbenchPane workbench, DataApp dataApp) {
        this(workbench, dataApp, null);
    }

    public BookmarksController(WorkbenchPane workbench, DataApp dataApp, Book filterByBook) {
        super("BOOKMARKS", workbench, dataApp, BookdataType.bookmark, filterByBook);

        this.title.set("书签");
        this.tooltip.set("书签");
        this.graphic.set(MaterialIcon.BOOKMARK.graphic());
    }
}
