package org.appxi.cbeta.explorer;

import org.appxi.cbeta.explorer.book.BookDataPlaceController;
import org.appxi.cbeta.explorer.book.BookListController;
import org.appxi.cbeta.explorer.bookdata.BookmarksController;
import org.appxi.cbeta.explorer.bookdata.FavoritesController;
import org.appxi.cbeta.explorer.home.AboutController;
import org.appxi.cbeta.explorer.prefs.PreferencesController;
import org.appxi.cbeta.explorer.recent.RecentController;
import org.appxi.cbeta.explorer.search.LookupController;
import org.appxi.cbeta.explorer.search.SearchController;
import org.appxi.cbeta.explorer.widget.WidgetsController;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchPrimaryController;
import org.appxi.javafx.workbench.WorkbenchViewController;

import java.util.ArrayList;
import java.util.List;

public class WorkbenchRootController extends WorkbenchPrimaryController {

    public WorkbenchRootController(WorkbenchApplication application) {
        super("ROOT-WORKBENCH", application);
    }

    @Override
    protected List<WorkbenchViewController> createViewControllers() {
        final List<WorkbenchViewController> result = new ArrayList<>();
        result.add(new BookListController(getApplication()));
        result.add(new BookDataPlaceController(getApplication()));
        result.add(new RecentController(getApplication()));
        result.add(new FavoritesController(getApplication()));
        result.add(new BookmarksController(getApplication()));

        result.add(new LookupController(getApplication()));
        result.add(new SearchController(getApplication()));
        result.add(new WidgetsController(getApplication()));

        result.add(new PreferencesController(getApplication()));
        result.add(new AboutController(getApplication()));
        return result;
    }
}
