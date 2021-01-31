package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.scene.Node;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;

public class BookDataController extends WorkbenchSideViewController {
    static final String VIEW_ID = "BOOK-DATA";

    public BookDataController(WorkbenchApplication application) {
        super(VIEW_ID, "在读", application);
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
            if (event.data instanceof Node view) {
                if (null != this.viewportVBox)
                    this.viewportVBox.getChildren().setAll(view);
                else bookViews = view;
                //FIXME 程序启动后的第一次在此默认显示此视图？
            }
        });
        getEventBus().addEventHandler(BookEvent.HIDE, event -> {
            if (null != this.viewportVBox) {
                this.viewportVBox.getChildren().clear();
            }
        });
    }

    @Override
    public void onViewportShow(boolean firstTime) {
        if (null != bookViews) {
            this.viewportVBox.getChildren().setAll(bookViews);
            bookViews = null;
        }
    }
}
