package org.appxi.cbeta.explorer.bookdata;

import appxi.cbeta.Book;
import org.appxi.cbeta.explorer.dao.BookdataType;
import org.appxi.javafx.iconfont.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchApplication;

public class BookmarksController extends BookdataController {

    public BookmarksController(WorkbenchApplication application) {
        this(application, null);
    }

    public BookmarksController(WorkbenchApplication application, Book filterByBook) {
        super("BOOKMARKS", application, BookdataType.bookmark, filterByBook);
        this.setTitles("书签");
        this.viewIcon.set(MaterialIcon.BOOKMARK.iconView());
    }
}
