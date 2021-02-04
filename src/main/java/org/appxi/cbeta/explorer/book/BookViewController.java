package org.appxi.cbeta.explorer.book;

import javafx.beans.Observable;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import netscape.javascript.JSObject;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.ChapterEvent;
import org.appxi.cbeta.explorer.event.DataEvent;
import org.appxi.cbeta.explorer.model.ChapterTree;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.theme.Theme;
import org.appxi.javafx.theme.ThemeEvent;
import org.appxi.javafx.theme.ThemeSet;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchMainViewController;
import org.appxi.prefs.UserPrefs;
import org.appxi.tome.cbeta.BookDocument;
import org.appxi.tome.cbeta.BookDocumentEx;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.model.Chapter;
import org.appxi.util.DevtoolHelper;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.HanLang;

import java.nio.file.Path;
import java.util.Objects;

public class BookViewController extends WorkbenchMainViewController {
    public final CbetaBook book;
    private final BookDocument bookDocument;

    private BookViewer bookViewer;
    private BookBasicView bookBasicView;
    private WebViewerEx webViewer;

    ContextMenu webContextMenu;
    double lastMouseScreenX, lastMouseScreenY;

    public BookViewController(CbetaBook book, WorkbenchApplication application) {
        this(book, null, application);
    }

    public BookViewController(CbetaBook book, Chapter chapter, WorkbenchApplication application) {
        super(book.id, book.title, application);
        this.book = book;
        this.bookDocument = new BookDocumentEx(book);
        this.currentChapter = chapter;
    }

    public final BookViewer getBookViewer() {
        return bookViewer;
    }

    @Override
    public Node createToolIconGraphic(Boolean placeInSideViews) {
        return null;
    }

    @Override
    protected void onViewportInitOnce() {
        this.bookViewer = new BookViewer(this);
        this.bookBasicView = this.bookViewer.bookBasicView;
        this.webViewer = this.bookViewer.webViewer;

        this.bookBasicView.tocsTree.setEnterOrDoubleClickAction(this::handleChaptersTreeViewEnterOrDoubleClickAction);
        this.bookBasicView.volsTree.setEnterOrDoubleClickAction(this::handleChaptersTreeViewEnterOrDoubleClickAction);
        //
        this.viewport = new BorderPane(this.bookViewer);
    }

//    public final TreeItem<Chapter> selectedChapterItem() {
//        final TitledPane expandedPane = this.bookBasicView.accordion.getExpandedPane();
//        if (expandedPane == this.bookBasicView.tocsPane)
//            return this.bookBasicView.tocsTree.getSelectionModel().getSelectedItem();
//        else if (expandedPane == this.bookBasicView.volsPane)
//            return this.bookBasicView.volsTree.getSelectionModel().getSelectedItem();
//        else
//            return null;
//    }
//
//    public final Chapter selectedChapter() {
//        final TreeItem<Chapter> treeItem = selectedChapterItem();
//        return null != treeItem ? treeItem.getValue() : null;
//    }

    private void handleThemeChanged(ThemeEvent event) {
        if (null == this.bookViewer)
            return;
        final Theme theme = null != event ? event.newTheme : this.getThemeProvider().getTheme();
        final ThemeSet themeSet = (ThemeSet) theme;
        bookViewer.applyThemeSet(themeSet);
    }

    @Override
    public void setupInitialize() {
        getEventBus().addEventHandler(ThemeEvent.CHANGED, this::handleThemeChanged);
        getEventBus().addEventHandler(DataEvent.DISPLAY_HAN, this::handleDisplayHanChanged);
    }

    @Override
    public void onViewportShow(boolean firstTime) {
        if (firstTime) {
            getEventBus().addEventHandler(ApplicationEvent.STOPPING, this::handleApplicationEventStopping);

            // apply theme
            this.handleThemeChanged(null);
            this.webViewer.getEngine().setUserDataDirectory(UserPrefs.confDir().toFile());
            // init nav-view
            ChapterTree.parseBookChaptersToTree(book, this.bookBasicView.tocsTree, this.bookBasicView.volsTree);
            // init default selection in basic view
            TreeItem<Chapter> treeItem = this.bookBasicView.prepareDefaultSelection(this.book, this.currentChapter);
            handleChaptersTreeViewEnterOrDoubleClickAction(null, treeItem);
            //
            this.webViewer.getViewer().addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
                lastMouseScreenX = event.getScreenX();
                lastMouseScreenY = event.getScreenY();
            });
            this.webViewer.setContextMenu(this.webContextMenu = new ContextMenu());
        }
        // update app title
        setPrimaryTitle(book.title);
        //
        getEventBus().fireEvent(new BookEvent(BookEvent.VIEW, this.book));
//        //FIXME 是否要同时显示左侧视图？
    }

    @Override
    public void onViewportHide(boolean hideOrElseClose) {
        saveUserExperienceData();
        if (hideOrElseClose) {
            getEventBus().fireEvent(new BookEvent(BookEvent.HIDE, this.book));
        } else {
            getEventBus().removeEventHandler(ThemeEvent.CHANGED, this::handleThemeChanged);
            getEventBus().removeEventHandler(ApplicationEvent.STOPPING, this::handleApplicationEventStopping);
            getEventBus().removeEventHandler(DataEvent.DISPLAY_HAN, this::handleDisplayHanChanged);
            setPrimaryTitle(null);
            getEventBus().fireEvent(new BookEvent(BookEvent.CLOSE, this.book));
        }
    }

    private void handleDisplayHanChanged(DataEvent event) {
        saveUserExperienceData();
        Chapter temp = this.currentChapter;
        this.currentChapter = null;
        openChapter(null, temp);
    }

    private Chapter currentChapter;
    private HanLang displayHan;

    private void handleChaptersTreeViewEnterOrDoubleClickAction(final InputEvent event, final TreeItem<Chapter> treeItem) {
        if (null == treeItem || null != event && !treeItem.isLeaf()) return;
        openChapter(event, treeItem.getValue());
    }

    public void openChapter(InputEvent event, Chapter chapter) {
        if (null == chapter || Objects.equals(currentChapter, chapter)) return;

        if (null != currentChapter && chapter.path.equals(currentChapter.path)) {
            currentChapter = chapter;
            webViewer.scrollTo("#" + chapter.start);
            return;
        }

        if (null != currentChapter) {
            getEventBus().fireEvent(new ChapterEvent(ChapterEvent.CLOSED, book, currentChapter));
        }
        currentChapter = chapter;

        final long st = System.currentTimeMillis();
        displayHan = HanLang.valueBy(UserPrefs.prefs.getString("display.han", HanLang.hant.lang));
        final String htmlDoc = this.bookDocument.getVolumeHtmlDocument(currentChapter.path, displayHan,
                body -> ChineseConvertors.convert(StringHelper.concat(
                        "<body data-finder-wrapper data-finder-scroll-offset=\"175\">\n",
                        "  <a data-finder-activator style=\"display:none\"></a>\n",
                        "  <article data-finder-content>", body.html(), "</article>\n",
                        "</body>"), HanLang.hant, displayHan),
                WebIncl.getIncludePaths()
        );
        this.webViewer.setOnLoadSucceedAction(we -> {
            // set an interface object named 'dataApi' in the web engine's page
            final JSObject window = webViewer.executeScript("window");
            window.setMember("dataApi", dataApi);
            //
            if (null == event) {
                final String selector = UserPrefs.recents.getString(book.id + ".selector", null);
                final double percent = UserPrefs.recents.getDouble(book.id + ".percent", 0);
//                DevtoolHelper.LOG.info("setScrollTop1BySelectors... selector=" + selector);
                webViewer.executeScript(StringHelper.concat("setScrollTop1BySelectors(\"", selector, "\", ", percent, ")"));
                webViewer.addEventHandler(KeyEvent.KEY_PRESSED, event1 -> {
                    if (event1.isControlDown() && event1.getCode() == KeyCode.F) {
                        webViewer.executeScript("handleOnOpenFinder()");
                    }
                });
            } else {
                webViewer.scrollTo("#" + currentChapter.start);
            }
            this.webViewer.widthProperty().removeListener(this::handleWebViewBodyResize);
            this.webViewer.widthProperty().addListener(this::handleWebViewBodyResize);
            DevtoolHelper.LOG.info("load htmlDocFile used time: " + (System.currentTimeMillis() - st) + ", " + htmlDoc);
        });

        this.webViewer.getEngine().load(Path.of(htmlDoc).toUri().toString());
        UserPrefs.recents.setProperty(book.id + ".chapter", currentChapter.id);
        getEventBus().fireEvent(new ChapterEvent(ChapterEvent.OPENED, book, currentChapter));
    }

    private void handleWebViewBodyResize(Observable o) {
        webViewer.executeScript("beforeOnResizeBody()");
        // for debug only
//      final String markedSelector = webPane.executeScript("markedScrollTop1Selector");
//      DevtoolHelper.LOG.info("marked... markedSelector=" + markedSelector);
    }

    private void handleApplicationEventStopping(ApplicationEvent event) {
        saveUserExperienceData();
    }

    public void saveUserExperienceData() {
        try {
            final double scrollTopPercentage = webViewer.getScrollTopPercentage();
            UserPrefs.recents.setProperty(book.id + ".percent", scrollTopPercentage);

            final String selector = webViewer.executeScript("scrollTop1Selector()");
            UserPrefs.recents.setProperty(book.id + ".selector", selector);
        } catch (Exception ignore) {
        }
//        DevtoolHelper.LOG.info("save... selector=" + selector + ", atPercentage = " + scrollTopPercentage);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * for communication from the Javascript engine.
     */
    private final DataApi dataApi = new DataApi();

    public class DataApi {
        /**
         * 用于将输入文字转换成与实际显示的相同，以方便页内查找
         */
        public String convertToDisplayHan(String input) {
            return ChineseConvertors.convert(input, null, displayHan);
        }

        public void onSelectionChanged(String text) {
            if (null == text) {
                return;
            }
            // build contextMenu
            webContextMenu.getItems().setAll(new MenuItem("Copy"), new MenuItem("Bookmark"), new MenuItem("Favorite"));
            webContextMenu.show(webViewer, lastMouseScreenX, lastMouseScreenY);
            System.out.println("Selection\t" + text);
        }

        public void onBookmarkChanged(boolean addOrElseRemove, String data) {
            System.out.println("Bookmark-data\t" + data);
        }

        public void onFavoriteChanged(boolean addOrElseRemove, String data) {
            System.out.println("Favorite-data\t" + data);
        }
    }

    private static class WebIncl {
        private static final String[] includeNames = {
                "jquery.min.js",
                "jquery.ext.js",
                "jquery.isinviewport.js",
                "jquery.highlight.js",
                "jquery.scrollto.js",
                "rangy-core.js",
                "rangy-classapplier.js",
                "rangy-textrange.js",
                "rangy-highlighter.js",
                "rangy-selectionsaverestore.js",
                "rangy-serializer.js",
                "app.css",
                "app.js",
                "jquery.finder.js"
        };
        private static final String[] includePaths = new String[includeNames.length];

        private static String[] getIncludePaths() {
            if (null != includePaths[0])
                return includePaths;

            synchronized (includePaths) {
                if (null != includePaths[0])
                    return includePaths;

                final Path dir = UserPrefs.appDir().resolve("web-incl");
                for (int i = 0; i < includeNames.length; i++) {
                    includePaths[i] = dir.resolve(includeNames[i]).toUri().toString();
                }
            }
            return includePaths;
        }
    }
}
