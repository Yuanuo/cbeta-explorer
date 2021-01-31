package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.*;
import org.appxi.cbeta.explorer.CbetaxHelper;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.ChapterEvent;
import org.appxi.cbeta.explorer.event.DataEvent;
import org.appxi.cbeta.explorer.model.BookTree;
import org.appxi.javafx.control.SeparatorMenuItemEx;
import org.appxi.javafx.control.TreeViewExt;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;
import org.appxi.prefs.UserPrefs;
import org.appxi.tome.cbeta.BookTreeMode;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.model.Chapter;
import org.appxi.util.DevtoolHelper;

import java.util.Objects;

public class BookListController extends WorkbenchSideViewController {
    private final ToggleGroup treeViewModeGroup = new ToggleGroup();
    private TreeViewExt<CbetaBook> treeView;

    public BookListController(WorkbenchApplication application) {
        super("BOOKS", "典籍", application);
    }

    @Override
    public Node createToolIconGraphic(Boolean placeInSideViews) {
        return new MaterialIconView(MaterialIcon.LOCAL_LIBRARY);
    }

    @Override
    protected void onViewportInitOnce() {
        final Button btnSearch = new Button();
        btnSearch.setTooltip(new Tooltip("快速查找书籍（Ctrl+O）"));
        btnSearch.setGraphic(new MaterialIconView(MaterialIcon.SEARCH));
        btnSearch.setOnAction(event -> getEventBus().fireEvent(new DataEvent(DataEvent.SEARCH_OPEN)));

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
        mCatalog.setOnAction(this::handleTreeViewModeAction);

        final RadioMenuItem mSimple = new RadioMenuItem("简易目录");
        mSimple.setToggleGroup(treeViewModeGroup);
        mSimple.setUserData("simple");
        mSimple.setOnAction(this::handleTreeViewModeAction);

        final RadioMenuItem mAdvance = new RadioMenuItem("进阶目录");
        mAdvance.setToggleGroup(treeViewModeGroup);
        mAdvance.setUserData("advance");
        mAdvance.setOnAction(this::handleTreeViewModeAction);
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
        final String navMode = UserPrefs.prefs.getString("cbeta.nav", "catalog");
        Toggle navToggle = null;
        for (Toggle toggle : treeViewModeGroup.getToggles()) {
            if (Objects.equals(navMode, toggle.getUserData())) {
                navToggle = toggle;
                break;
            }
        }
        if (null != navToggle)
            navToggle.setSelected(true);

        getEventBus().addEventHandler(BookEvent.OPEN, event -> handleOpenBookOrChapter(event, event.book, null));
        getEventBus().addEventHandler(ChapterEvent.OPEN, event -> handleOpenBookOrChapter(event, event.book, event.chapter));
        getEventBus().addEventHandler(ApplicationEvent.STARTED, event -> InternalHelper.initHtmlIncludes());
        //
        getEventBus().addEventHandler(DataEvent.BOOKS_READY, event -> handleTreeViewModeAction(null));
    }

    private void handleOpenBookOrChapter(Event event, CbetaBook book, Chapter chapter) {
        final BookViewController viewController = (BookViewController) getPrimaryViewport().findMainViewController(book.id);
        if (null != viewController) {
            getPrimaryViewport().selectMainView(viewController.viewId);
            event.consume();
            Platform.runLater(() -> {
                viewController.openChapter(null, chapter);
                // FIXME 是否需要每次打开书籍后同时在左侧目录树中定位？
//                handleLocateInTreeViewAction(viewController);
            });
            return;
        }
        Platform.runLater(() -> {
            final BookViewController controller = new BookViewController(book, chapter, getApplication());
            getPrimaryViewport().addWorkbenchViewAsMainView(controller, false);
            controller.setupInitialize();
            getPrimaryViewport().selectMainView(controller.viewId);
            // FIXME 是否需要每次打开书籍后同时在左侧目录树中定位？
//            handleLocateInTreeViewAction(controller);
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

    private void handleTreeViewModeAction(ActionEvent event) {
        final String mode = null != event
                ? String.valueOf(((RadioMenuItem) event.getSource()).getUserData())
                : UserPrefs.prefs.getString("cbeta.nav", "catalog");

        if (Objects.equals(mode, treeViewModeGroup.getUserData()))
            return;

        final long st = System.currentTimeMillis();
        final BookTree bookTree = new BookTree(CbetaxHelper.books, BookTreeMode.valueOf(mode));
        treeViewModeGroup.setUserData(mode);
        UserPrefs.prefs.setProperty("cbeta.nav", mode);
        if (null == treeViewModeGroup.getSelectedToggle()) {
            for (Toggle toggle : treeViewModeGroup.getToggles()) {
                if (mode.equals(toggle.getUserData())) {
                    toggle.setSelected(true);
                    break;
                }
            }
        }

        //
        final TreeItem<CbetaBook> rootItem = bookTree.getDataTree();
        rootItem.setExpanded(true);
        Platform.runLater(() -> treeView.setRoot(rootItem));
        DevtoolHelper.LOG.info("load booklist views used times: " + (System.currentTimeMillis() - st));
    }

    @Override
    public void onViewportShow(boolean firstTime) {
    }
}
