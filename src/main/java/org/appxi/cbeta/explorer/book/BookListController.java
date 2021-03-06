package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.SearcherEvent;
import org.appxi.cbeta.explorer.event.StatusEvent;
import org.appxi.cbeta.explorer.model.BookList;
import org.appxi.cbeta.explorer.model.BookTree;
import org.appxi.javafx.control.SeparatorMenuItemEx;
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
import org.appxi.util.DevtoolHelper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BookListController extends WorkbenchSideViewController {
    private final ToggleGroup treeViewModeGroup = new ToggleGroup();
    private TreeViewExt<CbetaBook> treeView;

    public BookListController(WorkbenchApplication application) {
        super("BOOKS", "典籍", application);
    }

    @Override
    public Node createToolIconGraphic(boolean sideToolOrElseViewTool) {
        return new MaterialIconView(MaterialIcon.LOCAL_LIBRARY);
    }

    @Override
    protected boolean isToolbarEnable() {
        return true;
    }

    @Override
    protected void onViewportInitOnce() {
        final Button btnSearch = new Button();
        btnSearch.setTooltip(new Tooltip("快速查找书籍（Ctrl+G）"));
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
        this.toolbar.addRight(btnSearch, btnLocate, btnMore);
        //
        this.treeView = new TreeViewExt<>((e, t) -> getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, t.getValue())));
        this.treeView.setShowRoot(false);
        this.viewportVBox.getChildren().add(this.treeView);
    }

    @Override
    public void setupInitialize() {
        getEventBus().addEventHandler(BookEvent.OPEN, event -> handleEventToOpenBook(event, event.book, event.chapter));
        //
        treeViewModeGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (null == nv) return;
            new Thread(() -> {
                final String mode = (String) nv.getUserData();
                final long st = System.currentTimeMillis();
                final BookTree bookTree = new BookTree(BookList.books, BookTreeMode.valueOf(mode));
                UserPrefs.prefs.setProperty("cbeta.nav", mode);
                //
                final TreeItem<CbetaBook> rootItem = bookTree.getDataTree();
                rootItem.setExpanded(true);
                FxHelper.runLater(() -> treeView.setRoot(rootItem));
                DevtoolHelper.LOG.info("load booklist views used times: " + (System.currentTimeMillis() - st));
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
        getEventBus().addEventHandler(ApplicationEvent.STARTED, event -> getEventBus().fireEvent(new StatusEvent(StatusEvent.BOOKS_READY)));
    }

    private void handleEventToOpenBook(Event event, CbetaBook book, Chapter chapter) {
        event.consume();
        final BookViewController viewController = (BookViewController) getPrimaryViewport().findMainViewController(book.id);
        if (null != viewController) {
            getPrimaryViewport().selectMainView(viewController.viewId);
            FxHelper.runLater(() -> viewController.openChapter(chapter));
            return;
        }
        FxHelper.runLater(() -> {
            final BookViewController controller = new BookViewController(book, getApplication());
            controller.attr(Chapter.class, chapter);
            getPrimaryViewport().addWorkbenchViewAsMainView(controller, false);
            controller.setupInitialize();
            getPrimaryViewport().selectMainView(controller.viewId);
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
    public void onViewportShow(boolean firstTime) {
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
