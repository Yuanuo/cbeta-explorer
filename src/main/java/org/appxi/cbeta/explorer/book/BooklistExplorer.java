package org.appxi.cbeta.explorer.book;

import appxi.cbeta.Book;
import appxi.cbeta.Chapter;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.DisplayHelper;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.GenericEvent;
import org.appxi.cbeta.explorer.event.ProgressEvent;
import org.appxi.cbeta.explorer.event.SearcherEvent;
import org.appxi.javafx.control.ToolBarEx;
import org.appxi.javafx.control.TreeViewExt;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.iconfont.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;
import org.appxi.util.StringHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BooklistExplorer extends WorkbenchSideViewController {
    private TreeViewExt<Book> treeView;

    public BooklistExplorer(WorkbenchApplication application) {
        super("BOOKS", application);
        this.setTitles("典籍");
        this.viewIcon.set(MaterialIcon.LOCAL_LIBRARY.iconView());
    }

    @Override
    protected void onViewportInitOnce() {
        //
        final Button btnProfile = new Button();
        btnProfile.setTooltip(new Tooltip("选择书单"));
        btnProfile.setGraphic(MaterialIcon.PLAYLIST_ADD_CHECK.iconView());
        btnProfile.setOnAction(event -> AppContext.booklistProfile.selectProfile(null));
        //
        final Button btnProfiles = new Button();
        btnProfiles.setTooltip(new Tooltip("管理我的书单"));
        btnProfiles.setGraphic(MaterialIcon.EDIT_NOTE.iconView());
        btnProfiles.setOnAction(event -> AppContext.booklistProfile.manageProfiles());
        //
        getEventBus().addEventHandler(ProgressEvent.INDEXING, event -> {
            if (event.isFinished()) {
                btnProfile.setDisable(false);
                btnProfiles.setDisable(false);
            } else {
                if (!btnProfile.isDisabled()) btnProfile.setDisable(true);
                if (!btnProfiles.isDisabled()) btnProfiles.setDisable(true);
            }
        });
        //
        final Button btnSearch = new Button();
        btnSearch.setTooltip(new Tooltip("快捷检索（Shift x2 / Ctrl+G）"));
        btnSearch.setGraphic(MaterialIcon.SEARCH.iconView());
        btnSearch.setOnAction(event -> getEventBus().fireEvent(SearcherEvent.ofLookup(null)));

        //
        final Button btnLocate = new Button();
        btnLocate.setTooltip(new Tooltip("定位到当前打开的书籍（F3）"));
        btnLocate.setGraphic(MaterialIcon.GPS_FIXED.iconView());
        getPrimaryScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F3), btnLocate::fire);
        btnLocate.setOnAction(event -> {
            WorkbenchViewController controller = getPrimaryViewport().getSelectedMainViewController();
            if (!(controller instanceof BookXmlViewer bookView))
                return;
            final TreeItem<Book> treeItem = TreeHelper.findFirstByValue(treeView.getRoot(), bookView.book);
            if (null != treeItem) {
                treeView.getSelectionModel().select(treeItem);
                treeView.scrollToIfNotVisible(treeItem);
            }
        });

        //
        final ToolBarEx toolbar = new ToolBarEx();
        HBox.setHgrow(toolbar, Priority.ALWAYS);
        toolbar.addRight(btnProfile, btnProfiles, btnSearch, btnLocate);
        this.headBar.getChildren().add(toolbar);
        //
        this.treeView = new TreeViewExt<>((e, t) -> getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, t.getValue())));
        this.treeView.setShowRoot(false);
        this.treeView.getStyleClass().add("book-list");
        this.treeView.setCellFactory(v -> new TreeCell<>() {
            Book updatedItem;

            @Override
            protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    updatedItem = null;
                    this.setText(null);
                    this.setTooltip(null);
                    this.setGraphic(null);
                    return;
                }
                if (item == updatedItem)
                    return;//
                updatedItem = item;
                String text = item.title;
                if (null != item.path && item.volumes.size() > 0) {
                    text = StringHelper.concat(text, "（", item.volumes.size(), "卷）");
                }
                this.setText(DisplayHelper.displayText(text));
                //
                this.setTooltip(new Tooltip(this.getText().concat(StringHelper.isBlank(item.authorInfo) ? ""
                        : "\n".concat(item.id).concat(" by ").concat(DisplayHelper.displayText(item.authorInfo))
                )));
                //
                this.setGraphic((this.getTreeItem().isLeaf() ? MaterialIcon.ARTICLE
                        : (this.getTreeItem().isExpanded() ? MaterialIcon.FOLDER_OPEN : MaterialIcon.FOLDER)).iconView());
                //
                this.getStyleClass().remove("visited");
                if (null != item.path && null != AppContext.recentBooks.getProperty(item.id)) {
                    this.getStyleClass().add("visited");
                }
            }
        });

        // dynamic context-menu
        this.treeView.setOnContextMenuRequested(this::handleEventOnContextMenuRequested);
        // hide context-menu
        this.treeView.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.SECONDARY) {
                if (null != contextMenu && contextMenu.isShowing())
                    contextMenu.hide();
            }
        });
        //
        this.viewportVBox.getChildren().add(this.treeView);
    }

    private ContextMenu contextMenu;

    private void handleEventOnContextMenuRequested(ContextMenuEvent event) {
        final TreeItem<Book> selectedItem = this.treeView.getSelectionModel().getSelectedItem();
        final List<MenuItem> menuItems = new ArrayList<>(16);
        //
        if (null != selectedItem) {
            final Book book = selectedItem.getValue();
            MenuItem menuItem;
            // view
            if (book.id != null && book.path != null) {
                menuItem = new MenuItem("查看");
                menuItem.setGraphic(MaterialIcon.VISIBILITY.iconView());
                menuItem.setOnAction(event1 -> getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, book)));
                menuItems.add(menuItem);
            }

            // search in this
            menuItem = new MenuItem("从这里搜索");
            menuItem.setGraphic(MaterialIcon.FIND_IN_PAGE.iconView());
            menuItem.setOnAction(event1 -> {
                if (null == book.id) {
                    book.attr("scope", "nav/".concat(AppContext.profile().template().name())
                            .concat("/").concat(TreeHelper.path(selectedItem)));
                }
                getEventBus().fireEvent(SearcherEvent.ofSearch(null, book));
            });
            menuItems.add(menuItem);
        }

        //
        event.consume();
        if (null != contextMenu && contextMenu.isShowing())
            contextMenu.hide();
        contextMenu = new ContextMenu(menuItems.toArray(new MenuItem[0]));
        contextMenu.show(this.treeView, event.getScreenX(), event.getScreenY());
    }

    @Override
    public void setupInitialize() {
        getEventBus().addEventHandler(BookEvent.OPEN,
                event -> handleEventToOpenBook(event, event.book, event.chapter));
        getEventBus().addEventHandler(ApplicationEvent.STARTING,
                event -> new Thread(() -> {
                    // 在启动过程中尝试加载booklistProfile，正常情况下会成功加载（如果过早加载成功，有的监听器可能不被执行！）
                    if (!AppContext.booklistProfile.loadProfile()) {
                        // 如果未加载成功，此时则给予提示并让用户选择
                        Platform.runLater(() -> AppContext.booklistProfile.selectProfile(AppContext.profile()));
                    }
                }).start());
        getEventBus().addEventHandler(GenericEvent.PROFILE_READY, event -> onViewportShowing(true));
        // 当显示汉字类型改变时需要同步更新treeView
        getEventBus().addEventHandler(GenericEvent.DISPLAY_HAN_CHANGED,
                event -> Optional.ofNullable(this.treeView).ifPresent(TreeView::refresh));
    }

    private void handleEventToOpenBook(Event event, Book book, Chapter chapter) {
        if (book.id == null || book.path == null) return;
        event.consume();
        final BookXmlViewer viewController = (BookXmlViewer) getPrimaryViewport().findMainViewController(book.id);
        if (null != viewController) {
            getPrimaryViewport().selectMainView(viewController.viewId.get());
            FxHelper.runLater(() -> viewController.openChapter(chapter));
            return;
        }
        FxHelper.runLater(() -> {
            Optional.ofNullable(this.treeView).ifPresent(TreeView::refresh);
            final BookXmlViewer controller = new BookXmlViewer(book, getApplication());
            controller.attr(Chapter.class, chapter);
            getPrimaryViewport().addWorkbenchViewAsMainView(controller, false);
            controller.setupInitialize();
            getPrimaryViewport().selectMainView(controller.viewId.get());
        });
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        if (firstTime && null != treeView && null != AppContext.profile()) {
            final TreeItem<Book> rootItem = AppContext.booklistProfile.booklist().tree();
            if (treeView.getRoot() == rootItem) return;
            rootItem.setExpanded(true);
            FxHelper.runLater(() -> treeView.setRoot(rootItem));
        }
    }

    @Override
    public void onViewportHiding() {
    }
}
