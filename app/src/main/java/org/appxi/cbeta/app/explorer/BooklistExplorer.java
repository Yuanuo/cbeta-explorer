package org.appxi.cbeta.app.explorer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import org.appxi.book.Chapter;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.app.AppContext;
import org.appxi.cbeta.app.event.BookEvent;
import org.appxi.cbeta.app.event.GenericEvent;
import org.appxi.cbeta.app.event.ProgressEvent;
import org.appxi.cbeta.app.reader.BookXmlReader;
import org.appxi.event.Event;
import org.appxi.javafx.app.AppEvent;
import org.appxi.javafx.app.search.SearcherEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.settings.DefaultOption;
import org.appxi.javafx.settings.SettingsList;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPart;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.HanLang;

import java.util.Objects;
import java.util.Optional;

public class BooklistExplorer extends WorkbenchPartController.SideView {
    private BooklistTreeView treeView;

    public BooklistExplorer(WorkbenchPane workbench) {
        super(workbench);

        this.id.set("BOOKS");
        this.title.set("典籍");
        this.tooltip.set("典籍");
        this.graphic.set(MaterialIcon.LOCAL_LIBRARY.graphic());
    }

    @Override
    protected void createViewport(BorderPane viewport) {
        super.createViewport(viewport);
        //
        final Button btnProfile = MaterialIcon.PLAYLIST_ADD_CHECK.flatButton();
        btnProfile.setTooltip(new Tooltip("选择书单"));
        btnProfile.setOnAction(event -> BooksProfile.ONE.selectProfile(null));
        //
        final Button btnProfiles = MaterialIcon.EDIT_NOTE.flatButton();
        btnProfiles.setTooltip(new Tooltip("管理我的书单"));
        btnProfiles.setOnAction(event -> BooksProfile.ONE.manageProfiles());
        //
        app.eventBus.addEventHandler(ProgressEvent.INDEXING, event -> {
            if (event.isFinished()) {
                btnProfile.setDisable(false);
                btnProfiles.setDisable(false);
            } else {
                if (!btnProfile.isDisabled()) btnProfile.setDisable(true);
                if (!btnProfiles.isDisabled()) btnProfiles.setDisable(true);
            }
        });
        //
        final Button btnSearch = MaterialIcon.SEARCH.flatButton();
        btnSearch.setTooltip(new Tooltip("快捷检索（Shift x2 / Ctrl+G）"));
        btnSearch.setOnAction(event -> app.eventBus.fireEvent(SearcherEvent.ofLookup(null)));

        //
        final Button btnLocate = MaterialIcon.GPS_FIXED.flatButton();
        btnLocate.setTooltip(new Tooltip("定位到当前打开的书籍（F3）"));
        app.getPrimaryScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F3), btnLocate::fire);
        btnLocate.setOnAction(event -> {
            WorkbenchPart controller = workbench.getSelectedMainViewPart();
            if (!(controller instanceof BookXmlReader bookXmlReader))
                return;
            final TreeItem<Book> treeItem = TreeHelper.findFirstByValue(treeView.getRoot(), bookXmlReader.book);
            if (null != treeItem) {
                treeView.getSelectionModel().select(treeItem);
                treeView.scrollToIfNotVisible(treeItem);
            }
        });

        //
        this.topBar.addRight(btnProfile, btnProfiles, btnSearch, btnLocate);
        //
        this.treeView = new BooklistTreeView(this);
        viewport.setCenter(this.treeView);
    }

    @Override
    public void postConstruct() {
        app.eventBus.addEventHandler(BookEvent.OPEN, e -> handleEventToOpenBook(e, e.book, e.chapter));
        app.eventBus.addEventHandler(AppEvent.STARTING,
                event -> new Thread(() -> {
                    // 在启动过程中尝试加载booklistProfile，正常情况下会成功加载（如果过早加载成功，有的监听器可能不被执行！）
                    if (!BooksProfile.ONE.loadProfile()) {
                        FxHelper.sleepSilently(100);
                        // 如果未加载成功，此时则给予提示并让用户选择
                        FxHelper.runLater(() -> BooksProfile.ONE.selectProfile(BooksProfile.ONE.profile()));
                    }
                }).start());
        app.eventBus.addEventHandler(GenericEvent.PROFILE_READY, event -> {
            activeViewport(true);
            // 仅在用户未手动设置过时才提示
            if (StringHelper.isBlank(UserPrefs.prefs.getString("display.han", ""))) {
                FxHelper.runThread(2000, () -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                            "选择以 简体/繁体 显示经名标题、阅读视图等经藏数据。",
                            new ButtonType("简体"), new ButtonType("繁体"), new ButtonType("稍后在选项中设置"));
                    alert.setTitle("简繁体");
                    alert.setHeaderText("选择简/繁体");
                    alert.initOwner(app.getPrimaryStage());
                    alert.showAndWait().ifPresent(buttonType -> {
                        if ("稍后在选项中设置".equals(buttonType.getText())) return;

                        HanLang hanLang = "简体".equals(buttonType.getText()) ? HanLang.hans : HanLang.hantTW;
                        UserPrefs.prefs.setProperty("display.han", hanLang.lang);
                        app.eventBus.fireEvent(new GenericEvent(GenericEvent.HAN_LANG_CHANGED, hanLang));
                    });
                });
            }
        });
        // 当显示汉字类型改变时需要同步更新treeView
        app.eventBus.addEventHandler(GenericEvent.HAN_LANG_CHANGED,
                event -> Optional.ofNullable(this.treeView).ifPresent(TreeView::refresh));
        // 当书名显示风格改变时需要同步更新treeView
        app.eventBus.addEventHandler(GenericEvent.BOOK_LABEL_STYLED,
                event -> Optional.ofNullable(this.treeView).ifPresent(TreeView::refresh));
        //
        SettingsList.add(() -> {
            final ObjectProperty<BookLabelStyle> valueProperty = new SimpleObjectProperty<>(BookLabelStyle.value());
            valueProperty.addListener((o, ov, nv) -> {
                if (null == ov || Objects.equals(ov, nv)) return;
                BookLabelStyle.setValue(nv);
                app.eventBus.fireEvent(new GenericEvent(GenericEvent.BOOK_LABEL_STYLED, nv));
            });
            return new DefaultOption<BookLabelStyle>("书名显示风格", "仅典籍树、已读中有效", "显示", true)
                    .setValueProperty(valueProperty);
        });
    }

    private void handleEventToOpenBook(Event event, Book book, Chapter chapter) {
        if (null == book || book.id == null || book.path == null) return;
        event.consume();
        final BookXmlReader viewController = (BookXmlReader) workbench.findMainViewPart(book.id);
        if (null != viewController) {
            workbench.selectMainView(viewController.id.get());
            FxHelper.runLater(() -> viewController.navigate(chapter));
            return;
        }
        // 记录此book为已访问状态
        if (!AppContext.recentBooks.containsProperty(book.id)) {
            AppContext.recentBooks.setProperty(book.id, "");
        }
        FxHelper.runLater(() -> {
            // 刷新典籍视图，比如已访问过的典籍需求改变显示颜色等
            Optional.ofNullable(this.treeView).ifPresent(TreeView::refresh);
            final BookXmlReader controller = new BookXmlReader(book, workbench);
            workbench.addWorkbenchPartAsMainView(controller, false);
            if (null != chapter) {
                controller.setPosition(chapter);
            }
            workbench.selectMainView(controller.id.get());
        });
    }

    @Override
    public void activeViewport(boolean firstTime) {
        if (firstTime && null != treeView && null != BooksProfile.ONE.profile()) {
            final TreeItem<Book> rootItem = BooksProfile.ONE.booklist().tree();
            if (treeView.getRoot() == rootItem) return;
            rootItem.setExpanded(true);
            FxHelper.runLater(() -> treeView.setRoot(rootItem));
        }
    }
}
