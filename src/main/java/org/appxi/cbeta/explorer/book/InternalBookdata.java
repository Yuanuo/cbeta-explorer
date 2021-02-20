package org.appxi.cbeta.explorer.book;

import com.j256.ormlite.stmt.Where;
import javafx.beans.binding.Bindings;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.VBox;
import org.appxi.cbeta.explorer.dao.Bookdata;
import org.appxi.cbeta.explorer.dao.BookdataType;
import org.appxi.cbeta.explorer.dao.DaoService;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.javafx.control.ListViewExt;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.helper.ToastHelper;
import org.appxi.tome.model.Chapter;
import org.appxi.util.DateHelper;
import org.appxi.util.StringHelper;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

abstract class InternalBookdata extends InternalView {
    final BookdataType dataType;
    ListViewExt<Bookdata> listView;

    protected InternalBookdata(BookViewController bookView, BookdataType dataType) {
        super(bookView);
        this.dataType = dataType;
    }

    @Override
    protected void onViewportInitOnce() {
        this.listView = new ListViewExt<>(this::handleOnEnterOrDoubleClickAction);
        this.listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.viewport.setCenter(this.listView);
        //
        this.listView.setCellFactory(v -> new ListCell<>() {
            final VBox cardBox;
            final Label dataLabel = new Label();
            final Label timeLabel = new Label();

            {
                cardBox = new VBox(dataLabel, timeLabel);
                cardBox.setSpacing(10);
                cardBox.maxWidthProperty().bind(Bindings.createDoubleBinding(
                        () -> getWidth() - getPadding().getLeft() - getPadding().getRight() - 1,
                        widthProperty(), paddingProperty()));
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setStyle(getStyle().concat("-fx-padding: .8em"));

                dataLabel.setWrapText(true);
                timeLabel.setStyle(timeLabel.getStyle().concat(";-fx-font-size: 80%;-fx-opacity: .65;"));
            }

            @Override
            protected void updateItem(Bookdata item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    dataLabel.setText(buildLabelText(item));
                    timeLabel.setText("创建于 ".concat(DateHelper.format(item.updateAt)));
                    setGraphic(cardBox);
                }
            }
        });
        //
        final Consumer<Boolean> removeAction = sel -> {
            Collection<Bookdata> list = sel ? listView.getSelectionModel().getSelectedItems() : listView.getItems();
            try {
                DaoService.getBookdataDao().delete(list);
                listView.getItems().removeAll(list);
            } catch (SQLException t) {
                FxHelper.alertError(bookView.getApplication(), t);
            }
        };
        final MenuItem removeSel = new MenuItem("删除选中");
        removeSel.setOnAction(event -> removeAction.accept(true));
        final MenuItem removeAll = new MenuItem("删除全部");
        removeAll.setOnAction(event -> removeAction.accept(false));

        final ContextMenu contextMenu = new ContextMenu(removeSel, removeAll);
        this.listView.setContextMenu(contextMenu);
    }

    protected String buildLabelText(Bookdata item) {
        return item.data;
    }

    @Override
    public void onViewportInit(boolean firstTime) {
        if (firstTime) {
            try {
                List<Bookdata> list = queryWhere().and()
                        .eq("book", book.id)
                        .query();
                listView.getItems().setAll(list);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Where<Bookdata, Integer> queryWhere() throws Exception {
        return DaoService.getBookdataDao().queryBuilder().where()
                .eq("dataType", dataType);
    }

    final void handleOnAddAction() {
        try {
            final String anchorInfo = getBookdataAnchorInfo();
            if (StringHelper.isBlank(anchorInfo))
                throw new RuntimeException("未获取到有效定位信息，无法创建".concat(dataType.title));
            if (anchorInfo.length() > 1000)
                throw new RuntimeException("所选文字过多，无法创建".concat(dataType.title));

            final JSONObject json = new JSONObject(anchorInfo);
            final String anchor = json.getString("anchor");
            final String origData = json.getString("text");
            // check exists
            Bookdata data = findByAnchor(anchor);
            if (null != data) {
                Alert alert = new Alert(Alert.AlertType.NONE);
                alert.setTitle("重复");
                alert.setContentText("此处已存在".concat(dataType.title).concat("记录，是否删除已有").concat(dataType.title).concat("？"));
                alert.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
                if (ButtonType.YES == FxHelper.withTheme(bookView.getApplication(), alert).showAndWait().orElse(null)) {
                    DaoService.getBookdataDao().delete(data);
                    this.listView.getItems().remove(data);
                    ToastHelper.toast(bookView.getApplication(), "已删除".concat(dataType.title).concat("！"));
                    // TODO update html?
                }
                return;
            }
            //
            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.setTitle("添加".concat(dataType.title));
            final TextArea content = new TextArea();
            content.setWrapText(true);
            content.setText(origData);
            content.setEditable(isDataTextEditable());

            final DialogPane pane = new DialogPane();
            pane.setContent(content);
            alert.setDialogPane(pane);
            alert.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
            if (ButtonType.YES == FxHelper.withTheme(bookView.getApplication(), alert).showAndWait().orElse(null)) {
                String editData = content.getText().strip();
                if (editData.isBlank())
                    editData = origData;
                data = new Bookdata();
                data.createAt = data.updateAt = new Date();
                data.type = this.dataType;
                data.book = book.id;
                data.volume = bookView.currentChapter.path;
                data.location = book.title;// .concat("/").concat(currentChapter.title);
                data.anchor = anchor;
                data.data = editData.length() > 300 ? editData.substring(0, 300) : editData;
                data.extra = json.toString();
                //
                DaoService.getBookdataDao().create(data);
                this.listView.getItems().add(data);
                ToastHelper.toast(bookView.getApplication(), "已添加".concat(dataType.title).concat("！"));
            }
        } catch (Exception e) {
            FxHelper.alertError(bookView.getApplication(), e);
        }
    }

    protected boolean isDataTextEditable() {
        return true;
    }

    protected abstract String getBookdataAnchorInfo();

    protected void handleOnEnterOrDoubleClickAction(InputEvent inputEvent, Bookdata item) {
        if (null == item)
            return;
        // open as chapter
        final Chapter chapter = new Chapter();
        chapter.path = item.volume;
        chapter.start = item.anchor;
        bookView.getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, book, chapter));
    }

    Bookdata findByAnchor(String anchor) {
        return this.listView.getItems().stream().filter(v -> Objects.equals(v.anchor, anchor)).findFirst().orElse(null);
    }
}
