package org.appxi.cbeta.explorer;

import javafx.application.Platform;
import javafx.event.Event;
import org.appxi.cbeta.explorer.book.BookListController;
import org.appxi.cbeta.explorer.book.BookViewController;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.ChapterEvent;
import org.appxi.cbeta.explorer.home.AboutController;
import org.appxi.cbeta.explorer.prefs.PreferencesController;
import org.appxi.cbeta.explorer.recent.RecentController;
import org.appxi.cbeta.explorer.tool.EpubRenameController;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchPrimaryController;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.model.Chapter;

import java.util.ArrayList;
import java.util.List;

public class WorkbenchRootController extends WorkbenchPrimaryController {

    public WorkbenchRootController(WorkbenchApplication application) {
        super("ROOT-WORKBENCH", "Workbench", application);
    }

    @Override
    public void setupInitialize() {
        getEventBus().addEventHandler(BookEvent.OPEN, event -> handleOpenBookOrChapter(event, event.book, null));
        getEventBus().addEventHandler(ChapterEvent.OPEN, event -> handleOpenBookOrChapter(event, event.book, event.chapter));

        super.setupInitialize();

        getApplication().updateStartingProgress();
        createAboutLinkAtStatusBar();
    }

    private void handleOpenBookOrChapter(Event event, CbetaBook book, Chapter chapter) {
        final BookViewController viewController = (BookViewController) getViewport().findMainViewController(book.id);
        if (null != viewController) {
            getViewport().selectMainView(viewController.viewId);
            event.consume();
            Platform.runLater(() -> viewController.openChapter(null, chapter));
            return;
        }
        Platform.runLater(() -> {
            final BookViewController controller = new BookViewController(book, chapter, getApplication());
            getViewport().addWorkbenchViewAsMainView(controller, false);
            controller.setupInitialize();
            getViewport().selectMainView(controller.viewId);
        });
    }

    @Override
    protected List<WorkbenchViewController> createViewControllers() {
        final List<WorkbenchViewController> result = new ArrayList<>();
        result.add(new BookListController(getApplication()));
        result.add(new RecentController(getApplication()));
        result.add(new EpubRenameController(getApplication()));
//        result.add(new BookmarksController());
//        result.add(new BooknotesController());
        result.add(new PreferencesController(getApplication()));
        result.add(new AboutController(getApplication()));
        return result;
    }

    private void createAboutLinkAtStatusBar() {
//        final Hyperlink link = new Hyperlink(AppInfo.NAME + " " + AppInfo.VERSION);
//        link.setVisited(true);
//        link.setStyle("-fx-underline: false;");
//        getViewport().infotools.addRight(link);
    }
}
