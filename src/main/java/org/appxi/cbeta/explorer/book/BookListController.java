package org.appxi.cbeta.explorer.book;

import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.DisplayHelper;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.GenericEvent;
import org.appxi.cbeta.explorer.event.SearcherEvent;
import org.appxi.cbeta.explorer.event.StatusEvent;
import org.appxi.cbeta.explorer.model.BookList;
import org.appxi.cbeta.explorer.model.BookTree;
import org.appxi.javafx.control.SeparatorMenuItemEx;
import org.appxi.javafx.control.ToolBarEx;
import org.appxi.javafx.control.TreeViewExt;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;
import org.appxi.prefs.UserPrefs;
import org.appxi.tome.cbeta.BookTreeMode;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.model.Chapter;
import org.appxi.util.StringHelper;
import org.appxi.javafx.glyphfont.MaterialIcon;
import org.appxi.javafx.glyphfont.MaterialIconView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookListController extends WorkbenchSideViewController {
    private final ToggleGroup treeViewModeGroup = new ToggleGroup();
    private TreeViewExt<CbetaBook> treeView;

    public BookListController(WorkbenchApplication application) {
        super("BOOKS", application);
        this.setTitles("典籍");
        this.viewIcon.set(new MaterialIconView(MaterialIcon.LOCAL_LIBRARY));
    }

    @Override
    protected void onViewportInitOnce() {
        final Button btnSearch = new Button();
        btnSearch.setTooltip(new Tooltip("快捷检索（Ctrl+G）"));
        btnSearch.setGraphic(new MaterialIconView(MaterialIcon.SEARCH));
        btnSearch.setOnAction(event -> getEventBus().fireEvent(SearcherEvent.ofLookup(null)));

        //
        final Button btnLocate = new Button();
        btnLocate.setTooltip(new Tooltip("定位到当前打开的书籍"));
        btnLocate.setGraphic(new MaterialIconView(MaterialIcon.GPS_FIXED));
        btnLocate.setOnAction(event -> handleLocateInTreeViewAction(null));

        //
        final MenuButton btnMore = new MenuButton();
        btnMore.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        btnMore.setGraphic(new MaterialIconView(MaterialIcon.MENU));

        final RadioMenuItem mCatalog = new RadioMenuItem("部类目录");
        mCatalog.setToggleGroup(treeViewModeGroup);
        mCatalog.setUserData("catalog");

        final RadioMenuItem mSimple = new RadioMenuItem("简易目录");
        mSimple.setToggleGroup(treeViewModeGroup);
        mSimple.setUserData("simple");

        final RadioMenuItem mAdvance = new RadioMenuItem("进阶目录");
        mAdvance.setToggleGroup(treeViewModeGroup);
        mAdvance.setUserData("advance");
        //
        btnMore.getItems().addAll(new SeparatorMenuItemEx("目录模式"), mCatalog, mSimple, mAdvance);
        //
        final ToolBarEx toolbar = new ToolBarEx();
        HBox.setHgrow(toolbar, Priority.ALWAYS);
        toolbar.addRight(btnSearch, btnLocate, btnMore);
        this.headBar.getChildren().add(toolbar);
        //
        this.treeView = new TreeViewExt<>((e, t) -> getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, t.getValue())));
        this.treeView.setShowRoot(false);
        this.treeView.getStyleClass().add("book-list");
        this.treeView.setCellFactory(v -> new TreeCell<>() {
            CbetaBook updatedItem;

            @Override
            protected void updateItem(CbetaBook item, boolean empty) {
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
                if (null != item.path && item.numberVols > 0) {
                    text = StringHelper.concat(text, "（", item.numberVols, "卷）");
                }
                this.setText(DisplayHelper.displayText(text));
                //
                if (null != item.path && StringHelper.isNotBlank(item.authorInfo))
                    this.setTooltip(new Tooltip(item.id.concat(" by ").concat(DisplayHelper.displayText(item.authorInfo))));
                else this.setTooltip(null);
                //
                this.setGraphic(new MaterialIconView(this.getTreeItem().isLeaf() ? MaterialIcon.ARTICLE
                        : (this.getTreeItem().isExpanded() ? MaterialIcon.FOLDER_OPEN : MaterialIcon.FOLDER)));
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
        final TreeItem<CbetaBook> selectedItem = this.treeView.getSelectionModel().getSelectedItem();
        final List<MenuItem> menuItems = new ArrayList<>(16);
        //
        if (null != selectedItem) {
            final CbetaBook book = selectedItem.getValue();

            // view
            MenuItem menuItem = new MenuItem("查看");
            menuItem.setGraphic(new MaterialIconView(MaterialIcon.VISIBILITY));
            menuItem.setOnAction(event1 -> getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, book)));
            menuItems.add(menuItem);

            // search in this
            menuItem = new MenuItem("从这里搜索");
            menuItem.setGraphic(new MaterialIconView(MaterialIcon.FIND_IN_PAGE));
            menuItem.setOnAction(event1 -> {
                if (null == book.id) {
                    final StringBuilder buff = new StringBuilder();
                    TreeHelper.walkParents(selectedItem, n -> {
                        if (n == null || n.getValue() == null) return true;
                        buff.insert(0, "/".concat(n.getValue().title));
                        return false;
                    });
                    buff.insert(0, "nav/".concat(UserPrefs.prefs.getString("cbeta.nav", "catalog")));
                    book.attr("path", buff.toString());
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
        getEventBus().addEventHandler(BookEvent.OPEN, event -> handleEventToOpenBook(event, event.book, event.chapter));
        //
        AppContext.bookTreeMode = BookTreeMode.valueOf(UserPrefs.prefs.getString("cbeta.nav", "catalog"));
        treeViewModeGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (null == nv) return;
            new Thread(() -> {
                final String mode = (String) nv.getUserData();
                final long st = System.currentTimeMillis();
                final BookTree bookTree = new BookTree(BookList.books, BookTreeMode.valueOf(mode));
                UserPrefs.prefs.setProperty("cbeta.nav", mode);
                //
                final TreeItem<CbetaBook> rootItem = bookTree.getDataTree();
                AppContext.bookTreeMode = bookTree.getMode();
                rootItem.setExpanded(true);
                FxHelper.runLater(() -> treeView.setRoot(rootItem));
//                DevtoolHelper.LOG.info("load booklist views used times: " + (System.currentTimeMillis() - st));
            }).start();
        });
        getEventBus().addEventHandler(StatusEvent.BOOKS_READY, event -> {
            if (firstTimeShowHandled) {
                // init now
                lazyInitTreeView();
            } else {
                // init later
                booksReadyHandled = true;
            }
        });
        //
        getEventBus().addEventHandler(ApplicationEvent.STARTED,
                event -> getEventBus().fireEvent(new StatusEvent(StatusEvent.BOOKS_READY)));
        getEventBus().addEventHandler(GenericEvent.DISPLAY_HAN_CHANGED,
                event -> Optional.ofNullable(this.treeView).ifPresent(TreeView::refresh));
    }

    private void handleEventToOpenBook(Event event, CbetaBook book, Chapter chapter) {
        event.consume();
        final BookViewController viewController = (BookViewController) getPrimaryViewport().findMainViewController(book.id);
        if (null != viewController) {
            getPrimaryViewport().selectMainView(viewController.viewId.get());
            FxHelper.runLater(() -> viewController.openChapter(chapter));
            return;
        }
        FxHelper.runLater(() -> {
            Optional.ofNullable(this.treeView).ifPresent(TreeView::refresh);
            final BookViewController controller = new BookViewController(book, getApplication());
            controller.attr(Chapter.class, chapter);
            getPrimaryViewport().addWorkbenchViewAsMainView(controller, false);
            controller.setupInitialize();
            getPrimaryViewport().selectMainView(controller.viewId.get());
        });
    }

    private void handleLocateInTreeViewAction(WorkbenchViewController controller) {
        if (null == controller)
            controller = getPrimaryViewport().getSelectedMainViewController();
        if (!(controller instanceof BookViewController bookView))
            return;
        final TreeItem<CbetaBook> treeItem = TreeHelper.findFirstByValue(treeView.getRoot(), bookView.book);
        if (null != treeItem) {
            treeView.getSelectionModel().select(treeItem);
            treeView.scrollToIfNotVisible(treeItem);
        }
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        if (firstTime && booksReadyHandled) {
            // init now
            lazyInitTreeView();
        } else if (firstTime) {
            firstTimeShowHandled = true;
            // init later
        } else {
            // do nothing
        }
    }

    @Override
    public void onViewportHiding() {
    }

    private boolean firstTimeShowHandled, booksReadyHandled;

    private void lazyInitTreeView() {
        if (null == this.treeView)
            this.getViewport();
        final String navMode = UserPrefs.prefs.getString("cbeta.nav", "catalog");
        final ObservableList<Toggle> toggles = treeViewModeGroup.getToggles();
        final List<Toggle> filtered = toggles.filtered(v -> navMode.equals(v.getUserData()));
        if (!toggles.isEmpty())
            treeViewModeGroup.selectToggle(!filtered.isEmpty() ? filtered.get(0) : toggles.get(0));
    }
}
