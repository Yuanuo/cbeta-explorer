package org.appxi.cbeta.explorer.book;

import appxi.cbeta.Book;
import appxi.cbeta.Chapter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.GenericEvent;
import org.appxi.cbeta.explorer.event.ProgressEvent;
import org.appxi.cbeta.explorer.event.SearcherEvent;
import org.appxi.javafx.app.AppEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.settings.DefaultOption;
import org.appxi.javafx.settings.SettingsList;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;

import java.util.Objects;
import java.util.Optional;

public class BooklistExplorer extends WorkbenchSideViewController {
    private BooklistTreeView treeView;

    public BooklistExplorer(WorkbenchPane workbench) {
        super("BOOKS", workbench);
        this.setTitles("典籍");
        this.graphic.set(MaterialIcon.LOCAL_LIBRARY.graphic());
    }

    @Override
    protected void onViewportInitOnce() {
        //
        final Button btnProfile = MaterialIcon.PLAYLIST_ADD_CHECK.flatButton();
        btnProfile.setTooltip(new Tooltip("选择书单"));
        btnProfile.setOnAction(event -> BooklistProfile.ONE.selectProfile(null));
        //
        final Button btnProfiles = MaterialIcon.EDIT_NOTE.flatButton();
        btnProfiles.setTooltip(new Tooltip("管理我的书单"));
        btnProfiles.setOnAction(event -> BooklistProfile.ONE.manageProfiles());
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
            WorkbenchViewController controller = workbench.getSelectedMainViewController();
            if (!(controller instanceof BookXmlViewer bookView))
                return;
            final TreeItem<Book> treeItem = TreeHelper.findFirstByValue(treeView.getRoot(), bookView.book);
            if (null != treeItem) {
                treeView.getSelectionModel().select(treeItem);
                treeView.scrollToIfNotVisible(treeItem);
            }
        });

        //
        this.topBar.addRight(btnProfile, btnProfiles, btnSearch, btnLocate);
        //
        this.treeView = new BooklistTreeView(this);
        this.viewport.setCenter(this.treeView);
    }

    @Override
    public void initialize() {
        app.eventBus.addEventHandler(BookEvent.OPEN, e -> handleEventToOpenBook(e, e.book, e.chapter));
        app.eventBus.addEventHandler(AppEvent.STARTING,
                event -> new Thread(() -> {
                    // 在启动过程中尝试加载booklistProfile，正常情况下会成功加载（如果过早加载成功，有的监听器可能不被执行！）
                    if (!BooklistProfile.ONE.loadProfile()) {
                        FxHelper.sleepSilently(100);
                        // 如果未加载成功，此时则给予提示并让用户选择
                        FxHelper.runLater(() -> BooklistProfile.ONE.selectProfile(BooklistProfile.ONE.profile()));
                    }
                }).start());
        app.eventBus.addEventHandler(GenericEvent.PROFILE_READY, event -> onViewportShowing(true));
        // 当显示汉字类型改变时需要同步更新treeView
        app.eventBus.addEventHandler(GenericEvent.DISPLAY_HAN_CHANGED,
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
        final BookXmlViewer viewController = (BookXmlViewer) workbench.findMainViewController(book.id);
        if (null != viewController) {
            workbench.selectMainView(viewController.id.get());
            FxHelper.runLater(() -> viewController.navigate(chapter));
            return;
        }
        FxHelper.runLater(() -> {
            Optional.ofNullable(this.treeView).ifPresent(TreeView::refresh);
            final BookXmlViewer controller = new BookXmlViewer(book, workbench);
            controller.attr(Chapter.class, chapter);
            workbench.addWorkbenchViewAsMainView(controller, false);
            controller.initialize();
            workbench.selectMainView(controller.id.get());
        });
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        if (firstTime && null != treeView && null != BooklistProfile.ONE.profile()) {
            final TreeItem<Book> rootItem = BooklistProfile.ONE.booklist().tree();
            if (treeView.getRoot() == rootItem) return;
            rootItem.setExpanded(true);
            FxHelper.runLater(() -> treeView.setRoot(rootItem));
        }
    }

    @Override
    public void onViewportHiding() {
    }
}
