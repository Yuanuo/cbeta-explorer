package org.appxi.cbeta.explorer.bookdata;

import appxi.cbeta.Book;
import org.appxi.cbeta.explorer.dao.BookdataType;
import org.appxi.javafx.iconfont.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchApplication;

public class FavoritesController extends BookdataController {

    public FavoritesController(WorkbenchApplication application) {
        this(application, null);
    }

    public FavoritesController(WorkbenchApplication application, Book filterByBook) {
        super("FAVORITES", application, BookdataType.favorite, filterByBook);
        this.setTitles("收藏");
        this.viewIcon.set(MaterialIcon.STAR.iconView());
    }
}
