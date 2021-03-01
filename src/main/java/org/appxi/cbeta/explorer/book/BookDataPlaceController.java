package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.event.EventHandler;
import javafx.scene.Node;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;

public class BookDataPlaceController extends WorkbenchSideViewController {
    private static BookDataPlaceController instance;

    public BookDataPlaceController(WorkbenchApplication application) {
        super("BOOK-DATA", "在读", application);
        instance = null == instance ? this : instance;
    }

    public static BookDataPlaceController getInstance() {
        return instance;
    }

    @Override
    public Node createToolIconGraphic(boolean sideToolOrElseViewTool) {
        return new MaterialIconView(MaterialIcon.IMPORT_CONTACTS);
    }

    @Override
    protected void onViewportInitOnce() {
    }

    private BookViewController bookView;

    @Override
    public void setupInitialize() {
        getEventBus().addEventHandler(BookEvent.VIEW, event -> {
            bookView = null;// always reset
            final WorkbenchViewController mainView = getPrimaryViewport().getSelectedMainViewController();
            if (mainView instanceof BookViewController bookView) {
                if (null != this.viewportVBox) {
                    this.setViewTitle(bookView.viewName);
                    this.viewportVBox.getChildren().setAll(bookView.sideViews);
                } else this.bookView = bookView;
                //FIXME 程序启动后的第一次在此默认显示此视图？
            } else {
                setViewTitle(null);
            }
        });
        final EventHandler<BookEvent> handleOnBookViewHideOrClose = event -> {
            if (null != this.viewportVBox) {
                this.viewportVBox.getChildren().clear();
                setViewTitle(null);
            }
        };
        getEventBus().addEventHandler(BookEvent.HIDE, handleOnBookViewHideOrClose);
        getEventBus().addEventHandler(BookEvent.CLOSE, handleOnBookViewHideOrClose);
    }

    @Override
    public void onViewportShow(boolean firstTime) {
        if (null != bookView) {
            this.setViewTitle(bookView.viewName);
            this.viewportVBox.getChildren().setAll(bookView.sideViews);
            bookView = null;
        }
    }
}
