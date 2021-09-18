package org.appxi.cbeta.explorer.bookdata;

import org.controlsfx.glyphfont.MaterialIcon;
import org.controlsfx.glyphfont.MaterialIconView;
import org.appxi.cbeta.explorer.dao.BookdataType;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.tome.model.Book;

public class BookmarksController extends BookdataController {

    public BookmarksController(WorkbenchApplication application) {
        this(application, null);
    }

    public BookmarksController(WorkbenchApplication application, Book filterByBook) {
        super("BOOKMARKS", application, BookdataType.bookmark, filterByBook);
        this.setTitles("书签");
        this.viewIcon.set(new MaterialIconView(MaterialIcon.BOOKMARK));
    }
}
