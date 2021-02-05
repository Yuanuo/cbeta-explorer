package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.event.EventHandler;
import javafx.scene.Node;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;

public class BookDataController extends WorkbenchSideViewController {
    private static BookDataController instance;

    public BookDataController(WorkbenchApplication application) {
        super("BOOK-DATA", "在读", application);
        instance = this;
    }

    public static BookDataController getInstance() {
        return instance;
    }

    @Override
    public Node createToolIconGraphic(Boolean placeInSideViews) {
        return new MaterialIconView(MaterialIcon.IMPORT_CONTACTS);
    }

    @Override
    protected void onViewportInitOnce() {
    }

    Node bookViews;

    @Override
    public void setupInitialize() {
        getEventBus().addEventHandler(BookEvent.VIEW, event -> {
            bookViews = null;// always reset
            final WorkbenchViewController mainView = getPrimaryViewport().getSelectedMainViewController();
            if (mainView instanceof BookViewController bookView) {
                if (null != this.viewportVBox)
                    this.viewportVBox.getChildren().setAll(bookView.sideViews);
                else bookViews = bookView.sideViews;
                //FIXME 程序启动后的第一次在此默认显示此视图？
            }
        });
        final EventHandler<BookEvent> handleOnBookViewHideOrClose = event -> {
            if (null != this.viewportVBox) {
                this.viewportVBox.getChildren().clear();
            }
        };
        getEventBus().addEventHandler(BookEvent.HIDE, handleOnBookViewHideOrClose);
        getEventBus().addEventHandler(BookEvent.CLOSE, handleOnBookViewHideOrClose);
    }

    @Override
    public void onViewportShow(boolean firstTime) {
        if (null != bookViews) {
            this.viewportVBox.getChildren().setAll(bookViews);
            bookViews = null;
        }
    }
}
