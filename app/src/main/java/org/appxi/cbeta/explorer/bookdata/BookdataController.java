package org.appxi.cbeta.explorer.bookdata;

import appxi.cbeta.Book;
import appxi.cbeta.Chapter;
import com.j256.ormlite.stmt.Where;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.book.BooklistProfile;
import org.appxi.cbeta.explorer.dao.Bookdata;
import org.appxi.cbeta.explorer.dao.BookdataType;
import org.appxi.cbeta.explorer.dao.DaoService;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.BookdataEvent;
import org.appxi.javafx.control.ListViewEx;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;
import org.appxi.util.DateHelper;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class BookdataController extends WorkbenchSideViewController {
    static final Object AK_FIRST_TIME = new Object();

    public final BookdataType dataType;
    public final Book filterByBook;
    protected ListView<Bookdata> listView;

    public BookdataController(String viewId, WorkbenchPane workbench,
                              BookdataType dataType, Book filterByBook) {
        super(viewId, workbench);
        this.dataType = dataType;
        this.filterByBook = filterByBook;
    }

    @Override
    public void initialize() {
        // not internal
        if (null == this.filterByBook) {
            app.eventBus.addEventHandler(BookdataEvent.CREATED, event -> {
                if (event.data.type == dataType && hasAttr(AK_FIRST_TIME)) {
                    FxHelper.runLater(() -> listView.getItems().add(0, event.data));
                }
            });

            app.eventBus.addEventHandler(BookdataEvent.REMOVED, event -> {
                if (event.data.type == dataType) {
                    FxHelper.runLater(() -> listView.getItems().remove(event.data));
                }
            });
        }
    }

    @Override
    protected void onViewportInitOnce() {
        if (null != this.filterByBook)
            this.viewport.setTop(null);

        this.listView = new ListViewEx<>(this::handleOnEnterOrDoubleClickAction);
        this.listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.viewport.setCenter(this.listView);
        //
        this.listView.setCellFactory(v -> new ListCell<>() {
            final VBox cardBox;
            final Label textLabel, timeLabel, bookLabel;

            {
                textLabel = new Label();
                textLabel.setWrapText(true);

                timeLabel = new Label(null, MaterialIcon.ACCESS_TIME.graphic());
                timeLabel.setStyle(timeLabel.getStyle().concat(";-fx-font-size: 80%;-fx-opacity: .65;"));

                // global mode
                if (null == filterByBook) {
                    bookLabel = new Label(null, MaterialIcon.LOCATION_ON.graphic());
                    bookLabel.setStyle(bookLabel.getStyle().concat("-fx-opacity:.75;"));
                    HBox hBox = new HBox(timeLabel, bookLabel);
                    hBox.setStyle(hBox.getStyle().concat("-fx-spacing:.5em;"));
                    cardBox = new VBox(textLabel, hBox);
                } else {
                    bookLabel = null;
                    cardBox = new VBox(textLabel, timeLabel);
                }

                cardBox.setStyle(cardBox.getStyle().concat("-fx-spacing:.85em;-fx-padding:.5em;"));
                cardBox.maxWidthProperty().bind(Bindings.createDoubleBinding(
                        () -> getWidth() - getPadding().getLeft() - getPadding().getRight() - 1,
                        widthProperty(), paddingProperty()));

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                getStyleClass().add("bookdata-item");
            }

            Bookdata updatedItem;

            @Override
            protected void updateItem(Bookdata item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    updatedItem = null;
                    setText(null);
                    setGraphic(null);
                    return;
                }
                if (item == updatedItem)
                    return;//
                updatedItem = item;
                textLabel.setText(item.data);
                timeLabel.setText(DateHelper.format(item.updateAt));
                if (null == filterByBook && null != bookLabel) {
                    Book book = BooklistProfile.ONE.getBook(item.book);
                    bookLabel.setText(null == book ? null : AppContext.displayText(book.title));
                }
                setGraphic(cardBox);
            }
        });

        //
        final Consumer<Boolean> removeAction = sel -> {
            Collection<Bookdata> list = sel ? listView.getSelectionModel().getSelectedItems() : listView.getItems();
            try {
                DaoService.getBookdataDao().delete(list);
                listView.getItems().removeAll(list);
            } catch (SQLException t) {
                app.toastError(t.getMessage());
            }
        };
        final MenuItem removeSel = new MenuItem("删除选中");
        removeSel.setOnAction(event -> removeAction.accept(true));
        final MenuItem removeAll = new MenuItem("删除全部");
        removeAll.setOnAction(event -> removeAction.accept(false));

        this.listView.setContextMenu(new ContextMenu(
                removeSel, removeAll
        ));
    }

    protected void handleOnEnterOrDoubleClickAction(InputEvent inputEvent, Bookdata item) {
        if (null == item)
            return;
        final Book book = BooklistProfile.ONE.getBook(item.book);
        final Chapter chapter = new Chapter();
        chapter.path = item.volume;
        chapter.anchor = item.anchor;
        if (null != item.anchor)
            chapter.attr("position.selector", item.anchor);
        app.eventBus.fireEvent(new BookEvent(BookEvent.OPEN, book, chapter));
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        if (firstTime) {
            attr(AK_FIRST_TIME, true);
            refreshListView();
        }
    }

    @Override
    public void onViewportHiding() {
    }

    private void refreshListView() {
        try {
            List<Bookdata> list = startQueryWhere()
                    .queryBuilder().orderBy("updateAt", false)
                    .query();
            listView.getItems().setAll(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Where<Bookdata, Integer> startQueryWhere() throws Exception {
        Where<Bookdata, Integer> where = DaoService.getBookdataDao().queryBuilder()
                .where().eq("dataType", this.dataType);
        if (null != this.filterByBook)
            where = where.and().eq("book", filterByBook.id);
        return where;
    }

    public Bookdata findDataByAnchor(String anchor) {
        return this.listView.getItems().stream().filter(v -> Objects.equals(v.anchor, anchor)).findFirst().orElse(null);
    }

    public void createData(Bookdata data) {
        try {
            DaoService.getBookdataDao().create(data);
            this.listView.getItems().add(0, data);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void removeData(Bookdata data) {
        try {
            DaoService.getBookdataDao().delete(data);
            this.listView.getItems().remove(data);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
