package org.appxi.cbeta.explorer.book;

import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import netscape.javascript.JSObject;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.ChapterEvent;
import org.appxi.cbeta.explorer.event.DataEvent;
import org.appxi.cbeta.explorer.model.ChapterTree;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.holder.RawHolder;
import org.appxi.javafx.control.TreeViewExt;
import org.appxi.javafx.control.WebViewer;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.helper.TreeHelper;
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
import java.util.function.Predicate;

public class BookViewController extends WorkbenchMainViewController {
    public final CbetaBook book;
    private final BookDocument bookDocument;

    private BookViewer bookViewer;
    private TitledPane tocsPane, volsPane, infoPane;
    private TreeViewExt<Chapter> tocsTree, volsTree;
    private WebViewer webViewer;

    public BookViewController(CbetaBook book, WorkbenchApplication application) {
        this(book, null, application);
    }

    public BookViewController(CbetaBook book, Chapter chapter, WorkbenchApplication application) {
        super(book.id, book.title, application);
        this.book = book;
        this.bookDocument = new BookDocumentEx(book);
        this.currentChapter = chapter;
    }

    @Override
    public Node createToolIconGraphic(Boolean placeInSideViews) {
        return null;
    }

    @Override
    protected void initViewport() {
        this.tocsTree = new TreeViewExt<>(this::handleChaptersTreeViewEnterOrDoubleClickAction);
        this.tocsPane = new TitledPane("目次", this.tocsTree);

        this.volsTree = new TreeViewExt<>(this::handleChaptersTreeViewEnterOrDoubleClickAction);
        this.volsPane = new TitledPane("卷次", this.volsTree);

        this.infoPane = new TitledPane("信息", new Label("Coming soon..."));

        this.bookViewer = new BookViewer();
        this.bookViewer.accordion.getPanes().setAll(this.tocsPane, this.volsPane, this.infoPane);

        this.webViewer = this.bookViewer.webViewer;
        //
        this.viewport = new BorderPane(this.bookViewer);
    }

    public final TreeItem<Chapter> selectedChapterItem() {
        final TitledPane expandedPane = this.bookViewer.accordion.getExpandedPane();
        if (expandedPane == this.tocsPane)
            return this.tocsTree.getSelectionModel().getSelectedItem();
        else if (expandedPane == this.volsPane)
            return this.volsTree.getSelectionModel().getSelectedItem();
        else
            return null;
    }

    public final Chapter selectedChapter() {
        final TreeItem<Chapter> treeItem = selectedChapterItem();
        return null != treeItem ? treeItem.getValue() : null;
    }

    private void setupTheme(ThemeEvent event) {
        if (null == this.bookViewer)
            return;
        final Theme theme = null != event ? event.newTheme : this.getThemeProvider().getTheme();
        final ThemeSet themeSet = (ThemeSet) theme;
        bookViewer.applyThemeSet(themeSet);
    }

    @Override
    public void setupInitialize() {
        getEventBus().addEventHandler(ThemeEvent.CHANGED, this::setupTheme);
        getEventBus().addEventHandler(DataEvent.DISPLAY_HAN, this::handleDisplayHanChanged);
    }

    @Override
    public void hideViewport(boolean hideOrElseClose) {
        saveUserExperienceData();
        if (!hideOrElseClose) {
            getEventBus().removeEventHandler(ThemeEvent.CHANGED, this::setupTheme);
            getEventBus().removeEventHandler(ApplicationEvent.STOPPING, this::handleApplicationEventStopping);
            getEventBus().removeEventHandler(DataEvent.DISPLAY_HAN, this::handleDisplayHanChanged);
            setPrimaryTitle(null);
            getEventBus().fireEvent(new BookEvent(BookEvent.CLOSE, this.book));
        }
    }

    @Override
    public void showViewport(boolean firstTime) {
        if (firstTime) {
            getEventBus().addEventHandler(ApplicationEvent.STOPPING, this::handleApplicationEventStopping);

            // apply theme
            this.setupTheme(null);
            this.webViewer.getEngine().setUserDataDirectory(UserPrefs.confDir().toFile());
//            webView.addEventFilter(ScrollEvent.SCROLL, (ScrollEvent e) -> {
//                double deltaY = e.getDeltaY();
//                if (e.isControlDown() && deltaY > 0) {
//                    webView.setZoom(webView.getZoom() * 1.1);
//                    e.consume();
//                } else if (e.isControlDown() && deltaY < 0) {
//                    webView.setZoom(webView.getZoom() / 1.1);
//                    e.consume();
//                }
//            });
            // init nav-view
            ChapterTree.parseBookChaptersToTree(book, this.tocsTree, this.volsTree);

            RawHolder<TitledPane> targetPane = new RawHolder<>();
            RawHolder<TreeView<Chapter>> targetTree = new RawHolder<>();
            RawHolder<TreeItem<Chapter>> targetTreeItem = new RawHolder<>();

            if (null != this.currentChapter && null != currentChapter.path) {
                Predicate<TreeItem<Chapter>> findByPath = itm ->
                        currentChapter.path.equals(itm.getValue().path)
                                && (null == currentChapter.start || currentChapter.start.equals(itm.getValue().start));
                detectAvailTarget(targetPane, targetTree, targetTreeItem, findByPath);
            }
            if (null == targetTreeItem.value) {
                final String lastChapterId = UserPrefs.recents.getString(book.id + ".chapter", null);
                if (StringHelper.isNotBlank(lastChapterId)) {
                    Predicate<TreeItem<Chapter>> findById = itm -> Objects.equals(lastChapterId, itm.getValue().id);
                    detectAvailTarget(targetPane, targetTree, targetTreeItem, findById);
                }
            }
            if (null == targetTreeItem.value) {
                if (this.tocsTree.getRoot().getChildren().size() > 0) {
                    targetPane.value = this.tocsPane;
                    targetTree.value = this.tocsTree;
                } else {
                    targetPane.value = this.volsPane;
                    targetTree.value = this.volsTree;
                }
                ObservableList<TreeItem<Chapter>> tmpList = targetTree.value.getRoot().getChildren();
                targetTreeItem.value = tmpList.isEmpty() ? null : tmpList.get(0);
            }

            this.bookViewer.accordion.setExpandedPane(targetPane.value);
            targetTreeItem.value.setExpanded(true);
            targetTree.value.getSelectionModel().select(targetTreeItem.value);
            handleChaptersTreeViewEnterOrDoubleClickAction(null, targetTreeItem.value);
        }
        setPrimaryTitle(book.title);
        getEventBus().fireEvent(new BookEvent(BookEvent.VIEW, this.book));
    }

    private void detectAvailTarget(RawHolder<TitledPane> targetPane,
                                   RawHolder<TreeView<Chapter>> targetTree,
                                   RawHolder<TreeItem<Chapter>> targetTreeItem,
                                   Predicate<TreeItem<Chapter>> findByExpr) {
        targetTreeItem.value = TreeHelper.findFirst(tocsTree.getRoot(), findByExpr);
        if (null != targetTreeItem.value) {
            targetPane.value = this.tocsPane;
            targetTree.value = this.tocsTree;
        } else {
            targetTreeItem.value = TreeHelper.findFirst(volsTree.getRoot(), findByExpr);
            if (null != targetTreeItem.value) {
                targetPane.value = this.volsPane;
                targetTree.value = this.volsTree;
            }
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
                body -> ChineseConvertors.convert(StringHelper.concat("<body data-finder-wrapper data-finder-scroll-offset=\"175\">\n",
                        "  <a data-finder-activator style=\"display:none\"></a>\n",
                        "  <div data-finder-content>", body.html(), "</div>\n",
                        "</body>"), HanLang.hant, displayHan),
                InternalHelper.htmlIncludes
        );
        this.webViewer.setOnLoadSucceedAction(we -> {
            // set an interface object named 'javaConnector' in the web engine's page
            final JSObject window = webViewer.executeScript("window");
            window.setMember("javaConnector", javaConnector);
            //
            if (null == event) {
                final String selector = UserPrefs.recents.getString(book.id + ".selector", null);
                final double percent = UserPrefs.recents.getDouble(book.id + ".percent", 0);
//                DevtoolHelper.LOG.info("scrollTop1BySelectors... selector=" + selector);
                webViewer.executeScript(StringHelper.concat("scrollTop1BySelectors(\"", selector, "\", ", percent, ")"));
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
    private final JavaConnector javaConnector = new JavaConnector();

    public class JavaConnector {
        public String convertInput(String input) {
            return ChineseConvertors.convert(input, null, displayHan);
        }

        public String[] getBookmarks() {
            return null;
        }

        public boolean setBookmark(String id, boolean state) {
            return false;
        }
    }
}
