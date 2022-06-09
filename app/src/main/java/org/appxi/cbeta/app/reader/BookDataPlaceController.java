package org.appxi.cbeta.app.reader;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.appxi.cbeta.app.AppContext;
import org.appxi.cbeta.app.event.BookEvent;
import org.appxi.event.EventHandler;
import org.appxi.javafx.settings.DefaultOption;
import org.appxi.javafx.settings.SettingsList;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPart;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.prefs.UserPrefs;

import java.util.Objects;

public class BookDataPlaceController extends WorkbenchPartController.SideView {
    private static final String PK_BOOK_DATA_SHOW = "book.data.show";

    private static BookDataPlaceController instance;

    public BookDataPlaceController(WorkbenchPane workbench) {
        super(workbench);
        instance = null == instance ? this : instance;

        this.id.set("BOOK-DATA");
        this.setTitles(null);
        this.graphic.set(MaterialIcon.IMPORT_CONTACTS.graphic());
    }

    protected void setTitles(String appendText) {
        String title = "在读";
        if (null != appendText)
            title = title.concat(" ").concat(appendText);
        this.title.set(title);
        this.tooltip.set(title);
    }

    public static BookDataPlaceController getInstance() {
        return instance;
    }

    private BookXmlReader bookXmlReader;

    @Override
    public void initialize() {
        app.eventBus.addEventHandler(BookEvent.VIEW, event -> {
            this.bookXmlReader = null;// always reset
            final WorkbenchPart mainView = workbench.getSelectedMainViewPart();
            if (mainView instanceof BookXmlReader bookXmlReader1) {
                setTitles(AppContext.hanText(bookXmlReader1.book.title));
                if (null != this.getViewport()) {
                    this.getViewport().setCenter(bookXmlReader1.sideViews);
                } else this.bookXmlReader = bookXmlReader1;

                if (UserPrefs.prefs.getBoolean(PK_BOOK_DATA_SHOW, false)) {
                    workbench.selectSideTool(id.get());
                }
            } else {
                setTitles(null);
            }
        });
        final EventHandler<BookEvent> handleOnBookViewHideOrClose = event -> {
            bookXmlReader = null;// always reset
            setTitles(null);
            if (null != this.getViewport()) {
                this.getViewport().setCenter(null);
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
    public void activeViewport(boolean firstTime) {
        if (null != bookXmlReader) {
            setTitles(AppContext.hanText(bookXmlReader.book.title));
            this.getViewport().setCenter(bookXmlReader.sideViews);
            bookXmlReader = null;
        }
    }
}
