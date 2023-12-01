package org.appxi.cbeta.app.reader;

import org.appxi.cbeta.Book;
import org.appxi.cbeta.app.DataApp;
import org.appxi.cbeta.app.dao.BookdataType;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;

public class FavoritesController extends BookDataController {

    public FavoritesController(WorkbenchPane workbench, DataApp dataApp) {
        this(workbench, dataApp, null);
    }

    public FavoritesController(WorkbenchPane workbench, DataApp dataApp, Book filterByBook) {
        super("FAVORITES", workbench, dataApp, BookdataType.favorite, filterByBook);

        this.tooltip.set("收藏");
        this.graphic.set(MaterialIcon.STAR.graphic());
    }
}
