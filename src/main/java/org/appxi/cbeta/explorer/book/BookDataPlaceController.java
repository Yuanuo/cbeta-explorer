package org.appxi.cbeta.explorer.book;

import org.controlsfx.glyphfont.MaterialIcon;
import org.controlsfx.glyphfont.MaterialIconView;
import javafx.event.EventHandler;
import org.appxi.cbeta.explorer.DisplayHelper;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;

public class BookDataPlaceController extends WorkbenchSideViewController {
    private static BookDataPlaceController instance;

    public BookDataPlaceController(WorkbenchApplication application) {
        super("BOOK-DATA", application);
        instance = null == instance ? this : instance;
        this.setTitles(null);
        this.viewIcon.set(new MaterialIconView(MaterialIcon.IMPORT_CONTACTS));
    }

    @Override
    protected void setTitles(String appendText) {
        String title = "在读";
        if (null != appendText)
            title = title.concat(" ").concat(appendText);
        super.setTitles(title);
    }

    public static BookDataPlaceController getInstance() {
        return instance;
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
                setTitles(DisplayHelper.displayText(bookView.book.title));
                if (null != this.viewportVBox) {
                    this.viewportVBox.getChildren().setAll(bookView.sideViews);
                } else this.bookView = bookView;
                //FIXME 程序启动后的第一次在此默认显示此视图？
            } else {
                setTitles(null);
            }
        });
        final EventHandler<BookEvent> handleOnBookViewHideOrClose = event -> {
            bookView = null;// always reset
            setTitles(null);
            if (null != this.viewportVBox) {
                this.viewportVBox.getChildren().clear();
            }
        };
        getEventBus().addEventHandler(BookEvent.HIDE, handleOnBookViewHideOrClose);
        getEventBus().addEventHandler(BookEvent.CLOSE, handleOnBookViewHideOrClose);
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        if (null != bookView) {
            setTitles(DisplayHelper.displayText(bookView.book.title));
            this.viewportVBox.getChildren().setAll(bookView.sideViews);
            bookView = null;
        }
    }

    @Override
    public void onViewportHiding() {
    }
}
