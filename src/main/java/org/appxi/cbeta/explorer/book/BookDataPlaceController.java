package org.appxi.cbeta.explorer.book;

import javafx.event.EventHandler;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;

public class BookDataPlaceController extends WorkbenchSideViewController {
    private static BookDataPlaceController instance;

    public BookDataPlaceController(WorkbenchPane workbench) {
        super("BOOK-DATA", workbench);
        instance = null == instance ? this : instance;
        this.setTitles(null);
        this.viewGraphic.set(MaterialIcon.IMPORT_CONTACTS.graphic());
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

    private BookXmlViewer bookView;

    @Override
    public void initialize() {
        app.eventBus.addEventHandler(BookEvent.VIEW, event -> {
            bookView = null;// always reset
            final WorkbenchViewController mainView = workbench.getSelectedMainViewController();
            if (mainView instanceof BookXmlViewer bookView) {
                setTitles(AppContext.displayText(bookView.book.title));
                if (null != this.viewport) {
                    this.viewport.setCenter(bookView.sideViews);
                } else this.bookView = bookView;
                //FIXME 程序启动后的第一次在此默认显示此视图？
            } else {
                setTitles(null);
            }
        });
        final EventHandler<BookEvent> handleOnBookViewHideOrClose = event -> {
            bookView = null;// always reset
            setTitles(null);
            if (null != this.viewport) {
                this.viewport.setCenter(null);
            }
        };
        app.eventBus.addEventHandler(BookEvent.HIDE, handleOnBookViewHideOrClose);
        app.eventBus.addEventHandler(BookEvent.CLOSE, handleOnBookViewHideOrClose);
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        if (null != bookView) {
            setTitles(AppContext.displayText(bookView.book.title));
            this.viewport.setCenter(bookView.sideViews);
            bookView = null;
        }
    }

    @Override
    public void onViewportHiding() {
    }
}
