package org.appxi.cbeta.explorer.book;

import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import netscape.javascript.JSObject;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.ChapterEvent;
import org.appxi.cbeta.explorer.event.DataEvent;
import org.appxi.cbeta.explorer.model.ChapterTree;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.holder.RawHolder;
import org.appxi.javafx.control.TreeViewExt;
import org.appxi.javafx.control.WebPane;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.theme.Theme;
import org.appxi.javafx.theme.ThemeEvent;
import org.appxi.javafx.theme.ThemeSet;
import org.appxi.javafx.workbench.views.WorkbenchOpenpartController;
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

public class BookViewController extends WorkbenchOpenpartController {
    public final CbetaBook book;
    private final BookDocument bookDocument;

    private BookViewPane bookviewPane;
    private Accordion workViews;
    private TitledPane tocsPane, volsPane, metaPane;
    private TreeViewExt<Chapter> tocTree, volTree;
    private WebPane webPane;

    private final Chapter initChapter;

    public BookViewController(CbetaBook book) {
        this(book, null);
    }

    public BookViewController(CbetaBook book, Chapter initChapter) {
        super(book.id, book.title);
        this.book = book;
        this.bookDocument = new BookDocumentEx(book);

        this.initChapter = initChapter;
    }

    @Override
    public Label getViewpartInfo() {
        return new Label(this.viewName);
    }

    @Override
    public StackPane getViewport() {
        if (null != this.bookviewPane)
            return this.bookviewPane;

        this.tocTree = new TreeViewExt<>(this::handleChaptersTreeViewEnterOrDoubleClickAction);
        this.tocsPane = new TitledPane("目次", this.tocTree);

        this.volTree = new TreeViewExt<>(this::handleChaptersTreeViewEnterOrDoubleClickAction);
        this.volsPane = new TitledPane("卷次", this.volTree);

        this.metaPane = new TitledPane("信息", new Label("Coming soon..."));

        this.workViews = new Accordion(this.tocsPane, this.volsPane, this.metaPane);

        this.bookviewPane = new BookViewPane();
        this.bookviewPane.addWorkview(this.workViews);

        this.webPane = this.bookviewPane.webPane;
        //
        return this.bookviewPane;
    }

    public final TreeItem<Chapter> selectedChapterItem() {
        final TitledPane expandedPane = this.workViews.getExpandedPane();
        if (expandedPane == this.tocsPane)
            return this.tocTree.getSelectionModel().getSelectedItem();
        else if (expandedPane == this.volsPane)
            return this.volTree.getSelectionModel().getSelectedItem();
        else
            return null;
    }

    public final Chapter selectedChapter() {
        final TreeItem<Chapter> treeItem = selectedChapterItem();
        return null != treeItem ? treeItem.getValue() : null;
    }

    private void setupTheme(ThemeEvent event) {
        final Theme theme = null != event ? event.newTheme : this.getThemeProvider().getTheme();
        if (theme.name.endsWith("new"))
            return;

        final ThemeSet themeSet = (ThemeSet) theme;
        bookviewPane.applyTheme(themeSet.getTheme(null));
        bookviewPane.updateThemeTools(themeSet);
    }

    @Override
    public void setupInitialize() {
        getEventBus().addEventHandler(ThemeEvent.CHANGED, this::setupTheme);
        getEventBus().addEventHandler(DataEvent.DISPLAY_HAN, this::handleDisplayHanChanged);
    }

    @Override
    public void onViewportClosed(Event event) {
        getEventBus().removeEventHandler(ThemeEvent.CHANGED, this::setupTheme);
        getEventBus().removeEventHandler(ApplicationEvent.STOPPING, this::handleApplicationEventStopping);
        getEventBus().removeEventHandler(DataEvent.DISPLAY_HAN, this::handleDisplayHanChanged);
        setPrimaryTitle(null);
    }

    @Override
    public void onViewportSelected(boolean firstTime) {
        if (firstTime) {
            getEventBus().addEventHandler(ApplicationEvent.STOPPING, this::handleApplicationEventStopping);

            // apply theme
            this.setupTheme(null);
            this.webPane.getWebEngine().setUserDataDirectory(UserPrefs.confDir().toFile());

            // init nav-view
            ChapterTree.parseBookChaptersToTree(book, this.tocTree, this.volTree);

            RawHolder<TitledPane> targetPane = new RawHolder<>();
            RawHolder<TreeView<Chapter>> targetTree = new RawHolder<>();
            RawHolder<TreeItem<Chapter>> targetTreeItem = new RawHolder<>();

            if (null != this.initChapter && null != initChapter.path) {
                Predicate<TreeItem<Chapter>> findByPath = itm ->
                        initChapter.path.equals(itm.getValue().path)
                                && (null == initChapter.start || initChapter.start.equals(itm.getValue().start));
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
                if (this.tocTree.getRoot().getChildren().size() > 0) {
                    targetPane.value = this.tocsPane;
                    targetTree.value = this.tocTree;
                } else {
                    targetPane.value = this.volsPane;
                    targetTree.value = this.volTree;
                }
                ObservableList<TreeItem<Chapter>> tmpList = targetTree.value.getRoot().getChildren();
                targetTreeItem.value = tmpList.isEmpty() ? null : tmpList.get(0);
            }

            this.workViews.setExpandedPane(targetPane.value);
            targetTreeItem.value.setExpanded(true);
            targetTree.value.getSelectionModel().select(targetTreeItem.value);
            handleChaptersTreeViewEnterOrDoubleClickAction(null, targetTreeItem.value);
        }
        setPrimaryTitle(book.title);
        getEventBus().fireEvent(new BookEvent(BookEvent.VIEW, this.bookDocument.book));
    }

    private void detectAvailTarget(RawHolder<TitledPane> targetPane,
                                   RawHolder<TreeView<Chapter>> targetTree,
                                   RawHolder<TreeItem<Chapter>> targetTreeItem,
                                   Predicate<TreeItem<Chapter>> findByExpr) {
        targetTreeItem.value = TreeHelper.findFirst(tocTree.getRoot(), findByExpr);
        if (null != targetTreeItem.value) {
            targetPane.value = this.tocsPane;
            targetTree.value = this.tocTree;
        } else {
            targetTreeItem.value = TreeHelper.findFirst(volTree.getRoot(), findByExpr);
            if (null != targetTreeItem.value) {
                targetPane.value = this.volsPane;
                targetTree.value = this.volTree;
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

    private void handleChaptersTreeViewEnterOrDoubleClickAction(final InputEvent event, final TreeItem<Chapter> treeItem) {
        if (null == treeItem || null != event && !treeItem.isLeaf()) return;
        openChapter(event, treeItem.getValue());
    }

    public void openChapter(InputEvent event, Chapter chapter) {
        if (null == chapter || Objects.equals(currentChapter, chapter)) return;

        if (null != currentChapter && chapter.path.equals(currentChapter.path)) {
            currentChapter = chapter;
            webPane.scrollTo("#" + chapter.start);
            return;
        }

        if (null != currentChapter) {
            getEventBus().fireEvent(new ChapterEvent(ChapterEvent.CLOSED, book, currentChapter));
        }
        currentChapter = chapter;

        final long st = System.currentTimeMillis();
        final HanLang targetHan = HanLang.valueBy(UserPrefs.prefs.getString("display.han", HanLang.hant.lang));
        final String htmlDoc = this.bookDocument.getVolumeHtmlDocument(currentChapter.path, targetHan,
                body -> ChineseConvertors.convert(StringHelper.concat("<body data-finder-wrapper data-finder-scroll-offset=\"175\">\n",
                        "  <a data-finder-activator style=\"display:none\"></a>\n",
                        "  <div data-finder-content>", body.html(), "</div>\n",
                        "</body>"), HanLang.hant, targetHan),
                InternalHelper.htmlIncludes
        );
        this.webPane.setOnLoadSucceedAction(we -> {
            // set an interface object named 'javaConnector' in the web engine's page
            final JSObject window = webPane.executeScript("window");
            window.setMember("javaConnector", javaConnector);
            //
            if (null == event) {
                final String selector = UserPrefs.recents.getString(book.id + ".selector", null);
                final double percent = UserPrefs.recents.getDouble(book.id + ".percent", 0);
//                DevtoolHelper.LOG.info("scrollTop1BySelectors... selector=" + selector);
                webPane.executeScript(StringHelper.concat("scrollTop1BySelectors(\"", selector, "\", ", percent, ")"));
                webPane.addEventHandler(KeyEvent.KEY_PRESSED, event1 -> {
                    if (event1.isControlDown() && event1.getCode() == KeyCode.F) {
                        webPane.executeScript("handleOnOpenFinder()");
                    }
                });
            } else {
                webPane.scrollTo("#" + currentChapter.start);
            }
            this.webPane.widthProperty().removeListener(this::handleWebViewBodyResize);
            this.webPane.widthProperty().addListener(this::handleWebViewBodyResize);
            DevtoolHelper.LOG.info("load htmlDocFile used time: " + (System.currentTimeMillis() - st) + ", " + htmlDoc);
        });

        this.webPane.getWebEngine().load(Path.of(htmlDoc).toUri().toString());
        UserPrefs.recents.setProperty(book.id + ".chapter", currentChapter.id);
        getEventBus().fireEvent(new ChapterEvent(ChapterEvent.OPENED, book, currentChapter));
    }

    private void handleWebViewBodyResize(Observable o) {
        webPane.executeScript("beforeOnResizeBody()");
        // for debug only
//      final String markedSelector = webPane.executeScript("markedScrollTop1Selector");
//      DevtoolHelper.LOG.info("marked... markedSelector=" + markedSelector);
    }

    @Override
    public void onViewportUnselected() {
        saveUserExperienceData();
    }

    private void handleApplicationEventStopping(ApplicationEvent event) {
        saveUserExperienceData();
    }

    public void saveUserExperienceData() {
        try {
            final double scrollTopPercentage = webPane.getScrollTopPercentage();
            UserPrefs.recents.setProperty(book.id + ".percent", scrollTopPercentage);

            final String selector = webPane.executeScript("scrollTop1Selector()");
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
            return ChineseConvertors.toHant(input);
        }

        public String[] getBookmarks() {
            return null;
        }

        public boolean setBookmark(String id, boolean state) {
            return false;
        }
    }
}
