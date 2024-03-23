package org.appxi.cbeta.app.reader;

import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.appxi.book.Chapter;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.app.DataApp;
import org.appxi.cbeta.app.dao.Bookdata;
import org.appxi.cbeta.app.dao.BookdataType;
import org.appxi.cbeta.app.event.BookEvent;
import org.appxi.cbeta.app.event.BookdataEvent;
import org.appxi.holder.BoolHolder;
import org.appxi.javafx.control.ListViewEx;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.util.DateHelper;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public abstract class UserDataController extends WorkbenchPartController.SideView {
    private static final int PAGE_SIZE = 30;
    private final BoolHolder activated = new BoolHolder();
    final DataApp dataApp;
    public final BookdataType dataType;
    public final Book filterByBook;
    Pagination pagination;
    ListView<Bookdata> listView;

    public UserDataController(String viewId, WorkbenchPane workbench,
                              DataApp dataApp, BookdataType dataType, Book filterByBook) {
        super(workbench);
        this.dataApp = dataApp;

        this.id.set(viewId);

        this.dataType = dataType;
        this.filterByBook = filterByBook;
    }

    @Override
    public void postConstruct() {
        // not internal
        if (null == this.filterByBook) {
            app.eventBus.addEventHandler(BookdataEvent.CREATED, event -> {
                if (event.data.type == dataType && activated.value) {
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
    protected void createViewport(BorderPane viewport) {
        super.createViewport(viewport);
        //
        if (null != filterByBook) {
            viewport.setTop(null);
        }
        //
        this.listView = new ListViewEx<>(this::handleOnEnterOrDoubleClickAction);
        this.listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
                    Book book = dataApp.dataContext.getBook(item.book);
                    bookLabel.setText(null == book ? null : dataApp.hanTextToShow(book.title));
                }
                setGraphic(cardBox);
            }
        });

        //
        final MenuItem refreshAll = new MenuItem("刷新");
        refreshAll.setOnAction(event -> updatePagination());

        final MenuItem removeSel = new MenuItem("删除选中");
        removeSel.setOnAction(event -> {
            try {
                Collection<Bookdata> list = listView.getSelectionModel().getSelectedItems();
                dataApp.daoService.getBookdataDao().delete(list);
                listView.getItems().removeAll(list);
            } catch (SQLException t) {
                app.toastError(t.getMessage());
            }
        });

        final MenuItem removeAll = new MenuItem("删除全部");
        removeAll.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "此操作不可恢复，是否继续删除全部数据？");
            alert.setTitle("删除全部数据！");
            alert.initOwner(app.getPrimaryStage());
            alert.showAndWait().filter(v -> v == ButtonType.OK).ifPresent(v -> {
                try {
                    startDeleteBuilder().delete();
                    updatePagination();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        this.listView.setContextMenu(new ContextMenu(refreshAll, removeSel, removeAll));

        //
        pagination = new Pagination();
        pagination.setPageFactory(pageIdx -> {
            try {
                updatePagination();
                //
                List<Bookdata> list = startQueryBuilder()
                        .orderBy("updateAt", false)
                        .offset((long) (pageIdx * PAGE_SIZE))
                        .limit((long) PAGE_SIZE)
                        .query();
                listView.getItems().setAll(list);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //
            return listView;
        });
        viewport.setCenter(pagination);
    }

    protected void handleOnEnterOrDoubleClickAction(InputEvent inputEvent, Bookdata item) {
        if (null == item)
            return;
        final Book book = dataApp.dataContext.getBook(item.book);
        final Chapter chapter = book.ofChapter();
        chapter.path = item.volume;
        chapter.anchor = item.anchor;
        if (null != item.anchor)
            chapter.attr("position.selector", item.anchor);
        app.eventBus.fireEvent(new BookEvent(BookEvent.OPEN, book, chapter));
    }

    @Override
    public void activeViewport(boolean firstTime) {
        if (firstTime) {
            activated.value = true;
            updatePagination();
        }
    }

    private void updatePagination() {
        try {
            int itemCount = (int) startQueryBuilder().countOf();
            int pageCount = itemCount / PAGE_SIZE;
            if (itemCount % (double) PAGE_SIZE > 0) {
                pageCount += 1;
            }
            pagination.setPageCount(pageCount > 0 ? pageCount : 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected QueryBuilder<Bookdata, Integer> startQueryBuilder() throws Exception {
        QueryBuilder<Bookdata, Integer> builder = dataApp.daoService.getBookdataDao().queryBuilder();
        builder.where().eq("dataType", this.dataType);
        if (null != this.filterByBook) {
            builder.where().eq("book", filterByBook.id);
        }
        return builder;
    }

    protected DeleteBuilder<Bookdata, Integer> startDeleteBuilder() throws Exception {
        DeleteBuilder<Bookdata, Integer> builder = dataApp.daoService.getBookdataDao().deleteBuilder();
        builder.where().eq("dataType", this.dataType);
        if (null != this.filterByBook) {
            builder.where().eq("book", filterByBook.id);
        }
        return builder;
    }

    public Bookdata findDataByAnchor(String anchor) {
        try {
            return startQueryBuilder().where().and().eq("anchor", anchor).queryForFirst();
        } catch (Exception e) {
            return null;
        }
    }

    public void createData(Bookdata data) {
        try {
            dataApp.daoService.getBookdataDao().create(data);
            this.listView.getItems().add(0, data);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void removeData(Bookdata data) {
        try {
            dataApp.daoService.getBookdataDao().delete(data);
            this.listView.getItems().remove(data);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
