package org.appxi.cbeta.explorer.bookdata;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.scene.Node;
import org.appxi.cbeta.explorer.dao.BookdataType;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.tome.model.Book;

public class BookmarksController extends BookdataController {

    public BookmarksController(WorkbenchApplication application) {
        this(application, null);
    }

    public BookmarksController(WorkbenchApplication application, Book filterByBook) {
        super("BOOKMARKS", "书签", application, BookdataType.bookmark, filterByBook);
    }

    @Override
    public Node createToolIconGraphic(Boolean placeInSideViews) {
        return new MaterialIconView(MaterialIcon.BOOKMARK);
    }
}
