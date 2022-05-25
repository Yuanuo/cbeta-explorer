package org.appxi.cbeta.explorer.book;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.javafx.settings.DefaultOption;
import org.appxi.javafx.settings.SettingsList;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;
import org.appxi.prefs.UserPrefs;

import java.util.Objects;

public class BookDataPlaceController extends WorkbenchSideViewController {
    private static final String PK_BOOK_DATA_SHOW = "book.data.show";

    private static BookDataPlaceController instance;

    public BookDataPlaceController(WorkbenchPane workbench) {
        super("BOOK-DATA", workbench);
        instance = null == instance ? this : instance;
        this.setTitles(null);
        this.graphic.set(MaterialIcon.IMPORT_CONTACTS.graphic());
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

                if (UserPrefs.prefs.getBoolean(PK_BOOK_DATA_SHOW, false)) {
                    workbench.selectSideTool(id.get());
                }
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
        //
        SettingsList.add(() -> {
            final BooleanProperty valueProperty = new SimpleBooleanProperty();
            valueProperty.set(UserPrefs.prefs.getBoolean(PK_BOOK_DATA_SHOW, false));
            valueProperty.addListener((o, ov, nv) -> {
                if (null == ov || Objects.equals(ov, nv)) return;
                UserPrefs.prefs.setProperty(PK_BOOK_DATA_SHOW, nv);
            });
            return new DefaultOption<Boolean>("自动显示在读本书数据", "开：自动显示；关：手动切换", "VIEWER", true)
                    .setValueProperty(valueProperty);
        });
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
