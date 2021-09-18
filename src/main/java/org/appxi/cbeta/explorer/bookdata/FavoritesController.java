package org.appxi.cbeta.explorer.bookdata;

import org.controlsfx.glyphfont.MaterialIcon;
import org.controlsfx.glyphfont.MaterialIconView;
import org.appxi.cbeta.explorer.dao.BookdataType;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.tome.model.Book;

public class FavoritesController extends BookdataController {

    public FavoritesController(WorkbenchApplication application) {
        this(application, null);
    }

    public FavoritesController(WorkbenchApplication application, Book filterByBook) {
        super("FAVORITES", application, BookdataType.favorite, filterByBook);
        this.setTitles("收藏");
        this.viewIcon.set(new MaterialIconView(MaterialIcon.STAR));
    }
}
