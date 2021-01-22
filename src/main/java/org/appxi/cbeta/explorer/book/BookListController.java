package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.appxi.cbeta.explorer.CbetaxHelper;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.DataEvent;
import org.appxi.cbeta.explorer.model.BookTree;
import org.appxi.cbeta.explorer.workbench.WorkbenchWorkpartControllerExt;
import org.appxi.javafx.control.SeparatorMenuItemEx;
import org.appxi.javafx.control.TreeViewExt;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.workbench.views.WorkbenchOpenpartController;
import org.appxi.prefs.UserPrefs;
import org.appxi.tome.cbeta.BookTreeMode;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.util.DevtoolHelper;

import java.util.Objects;

public class BookListController extends WorkbenchWorkpartControllerExt {
    private final ToggleGroup treeViewModeGroup = new ToggleGroup();
    private final TreeViewExt<CbetaBook> treeView;

    public BookListController() {
        super("BOOKS", "典籍");

        this.treeView = new TreeViewExt<>((e, t) -> getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, t.getValue())));
        this.treeView.setShowRoot(false);
    }

    @Override
    public Label getViewpartInfo() {
        return new Label(this.viewName, new FontAwesomeIconView(FontAwesomeIcon.MAP));
    }

    @Override
    public StackPane getViewport() {
        //
        final Button btnSearch = new Button();
        btnSearch.setTooltip(new Tooltip("快速查找书籍（Ctrl+O）"));
        btnSearch.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.SEARCH));
        btnSearch.setOnAction(event -> getEventBus().fireEvent(new DataEvent(DataEvent.SEARCH_OPEN)));

        //
        final Button btnLocate = new Button();
        btnLocate.setTooltip(new Tooltip("选择打开的书籍"));
        btnLocate.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.SUPPORT));
        btnLocate.setOnAction(this::handleLocateInTreeViewAction);

        //
        final MenuButton btnMore = new MenuButton();
        btnMore.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        btnMore.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.ELLIPSIS_H));

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
        this.viewpartVbox.getChildren().add(this.treeView);
        //
        return super.getViewport();
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

        getEventBus().addEventHandler(ApplicationEvent.STARTED, event -> InternalHelper.initHtmlIncludes());
        //
        getEventBus().addEventHandler(DataEvent.BOOKS_READY, event -> handleTreeViewModeAction(null));
    }

    private void handleLocateInTreeViewAction(ActionEvent event) {
        final WorkbenchOpenpartController openpart = getWorkbenchViewport().selectedOpenpart();
        if (!(openpart instanceof BookViewController controller))
            return;

        final TreeItem<CbetaBook> treeItem = TreeHelper.findFirstByValue(treeView.getRoot(), controller.book);
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

        //
        final TreeItem<CbetaBook> rootItem = bookTree.getDataTree();
        rootItem.setExpanded(true);
        Platform.runLater(() -> treeView.setRoot(rootItem));
        DevtoolHelper.LOG.info("load booklist views used times: " + (System.currentTimeMillis() - st));
    }
}
