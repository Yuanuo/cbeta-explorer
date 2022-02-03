package org.appxi.cbeta.explorer.bookdata;

import appxi.cbeta.Book;
import org.appxi.cbeta.explorer.dao.BookdataType;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;

public class FavoritesController extends BookdataController {

    public FavoritesController(WorkbenchPane workbench) {
        this(workbench, null);
    }

    public FavoritesController(WorkbenchPane workbench, Book filterByBook) {
        super("FAVORITES", workbench, BookdataType.favorite, filterByBook);
        this.setTitles("收藏");
        this.graphic.set(MaterialIcon.STAR.graphic());
    }
}
