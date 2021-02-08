package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import netscape.javascript.JSObject;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.ChapterEvent;
import org.appxi.cbeta.explorer.event.DataEvent;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.javafx.control.TabPaneExt;
import org.appxi.javafx.control.ToolBarEx;
import org.appxi.javafx.control.WebViewer;
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

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BookViewController extends WorkbenchMainViewController {
    public final CbetaBook book;
    private final BookDocument bookDocument;

    TabPane sideViews;

    InternalBookBasic bookBasic;
    InternalBookmarks bookmarks;
    InternalFavorites favorites;

    WebViewer webViewer;
    ToolBarEx toolbar;

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
    protected void onViewportInitOnce() {
        this.toolbar = new ToolBarEx();
        this.initToolbar();
        this.viewport.setTop(this.toolbar);
        //
        this.webViewer = new WebViewer();
        this.viewport.setCenter(this.webViewer);
        //
        this.sideViews = new TabPaneExt();
        this.sideViews.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(this.sideViews, Priority.ALWAYS);
        //
        this.bookBasic = new InternalBookBasic(this);
        this.bookmarks = new InternalBookmarks(this);
        this.favorites = new InternalFavorites(this);
        //
        final Tab tab1 = new Tab("基本", this.bookBasic.getViewport());
        final Tab tab2 = new Tab("书签", this.bookmarks.getViewport());
        final Tab tab3 = new Tab("收藏", this.favorites.getViewport());
        //
        this.sideViews.getTabs().setAll(tab1, tab2, tab3);
    }

    protected void initToolbar() {
        addTool_SideControl();
        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
        addTool_FontSize();
//        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
//        addTool_Themes();
        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
        addTool_WrapLines();
        addTool_FirstLetterIndent();
        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
        addTool_EditorMark();
        //
        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
        addTool_Bookmark();
        addTool_Favorite();
        addTool_SearchInPage();
    }

    private void addTool_SideControl() {
        final Button button = new Button();
        button.setGraphic(new MaterialIconView(MaterialIcon.IMPORT_CONTACTS));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("显示本书相关数据（目录、书签等）"));
        button.setOnAction(event -> this.getPrimaryViewport().selectSideTool(BookDataPlaceController.getInstance().viewId));
        this.toolbar.addLeft(button);
    }

    private void addTool_FontSize() {
        //TODO 是否使用全局默认值，并可保存为默认？
        final Consumer<Boolean> fSizeAction = state -> {
            if (null == state) {// reset ?
                webViewer.getViewer().setFontScale(1.0);
            } else if (state) { //
                webViewer.getViewer().setFontScale(webViewer.getViewer().getFontScale() + 0.1);
            } else {
                webViewer.getViewer().setFontScale(webViewer.getViewer().getFontScale() - 0.1);
            }
        };

        Button fSizeSubBtn = new Button(null, new MaterialIconView(MaterialIcon.ZOOM_OUT));
        fSizeSubBtn.setTooltip(new Tooltip("减小字号"));
        fSizeSubBtn.setOnAction(event -> fSizeAction.accept(false));

        Button fSizeSupBtn = new Button(null, new MaterialIconView(MaterialIcon.ZOOM_IN));
        fSizeSupBtn.setTooltip(new Tooltip("增大字号"));
        fSizeSupBtn.setOnAction(event -> fSizeAction.accept(true));
        this.toolbar.addRight(fSizeSubBtn, fSizeSupBtn);
    }

    private void addTool_Themes() {
        final Node themeMarker = new MaterialIconView(MaterialIcon.PALETTE);
        themeMarker.setId("web-theme-marker");
        themeMarker.getStyleClass().add("label");
        this.toolbar.addRight(themeMarker);
    }

    private void addTool_WrapLines() {
        final ToggleButton button = new ToggleButton();
        button.setGraphic(new MaterialIconView(MaterialIcon.WRAP_TEXT));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("折行显示"));
        button.setOnAction(event -> webViewer.executeScript("handleOnWrapLines()"));
        this.toolbar.addRight(button);
    }

    private void addTool_FirstLetterIndent() {
        final ToggleButton button = new ToggleButton();
        button.setGraphic(new MaterialIconView(MaterialIcon.FORMAT_INDENT_INCREASE));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("首行标点对齐"));
        button.setOnAction(event -> webViewer.executeScript("handleOnFirstLetterIndent()"));
        this.toolbar.addRight(button);
    }

    private void addTool_EditorMark() {
        final ToggleGroup marksGroup = new ToggleGroup();

        final ToggleButton markOrigInline = new ToggleButton("原编注");
        markOrigInline.setTooltip(new Tooltip("在编注点直接嵌入原编注内容"));
        markOrigInline.setUserData(0);

        final ToggleButton markModSharp = new ToggleButton("标记");
        markModSharp.setTooltip(new Tooltip("在编注点插入#号并划动鼠标查看CBETA编注内容"));
        markModSharp.setUserData(1);

        final ToggleButton markModColor = new ToggleButton("着色");
        markModColor.setTooltip(new Tooltip("在编注点着色被改变的文字并划动鼠标查看CBETA编注内容"));
        markModColor.setUserData(2);

        final ToggleButton markModPopover = new ToggleButton("着色+原编注");
        markModPopover.setTooltip(new Tooltip("在编注点着色被改变的文字并划动鼠标查看原编注+CBETA编注内容"));
        markModPopover.setUserData(3);

        final ToggleButton markModInline = new ToggleButton("CB编注");
        markModInline.setTooltip(new Tooltip("在编注点直接嵌入CBETA编注内容"));
        markModInline.setUserData(4);

        marksGroup.getToggles().setAll(markOrigInline, markModInline, markModSharp, markModColor, markModPopover);
        marksGroup.selectedToggleProperty().addListener((o, ov, nv) ->
                webViewer.executeScript("handleOnEditMark(" + (null == nv ? -1 : nv.getUserData()) + ")"));

        this.toolbar.addRight(markOrigInline, markModInline, markModSharp, markModColor, markModPopover);
    }


    private void addTool_Bookmark() {
        final Button button = new Button();
        button.setGraphic(new MaterialIconView(MaterialIcon.BOOKMARK_BORDER));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("添加书签"));
        button.setOnAction(event -> bookmarks.handleOnAddAction());
        this.toolbar.addRight(button);
    }

    private void addTool_Favorite() {
        final Button button = new Button();
        button.setGraphic(new MaterialIconView(MaterialIcon.STAR_BORDER));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("添加收藏"));
        button.setOnAction(event -> favorites.handleOnAddAction());
        this.toolbar.addRight(button);
    }

    private void addTool_SearchInPage() {
        final Button button = new Button();
        button.setGraphic(new MaterialIconView(MaterialIcon.SEARCH));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("查找"));
        button.setOnAction(event -> webViewer.executeScript("handleOnSearchInPage()"));
        this.toolbar.addRight(button);
    }


    /* //////////////////////////////////////////////////////////////////// */
    private void applyWebTheme(Theme webTheme) {
        if (null == this.webViewer)
            return;

        byte[] allBytes = new byte[0];
        if (null != webTheme && !webTheme.stylesheets.isEmpty()) {
            for (String webStyle : webTheme.stylesheets) {
                try {
                    URLConnection conn = new URL(webStyle).openConnection();
                    conn.connect();

                    BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                    int pos = allBytes.length;
                    byte[] tmpBytes = new byte[pos + in.available()];
                    System.arraycopy(allBytes, 0, tmpBytes, 0, pos);
                    allBytes = tmpBytes;
                    in.read(allBytes, pos, in.available());
                } catch (Exception ignore) {
                    ignore.printStackTrace();
                }
            }
        }
        String cssData = "data:text/css;charset=utf-8;base64," + Base64.getMimeEncoder().encodeToString(allBytes);
        this.webViewer.getEngine().setUserStyleSheetLocation(cssData);
    }

    private void handleThemeChanged(ThemeEvent event) {
        if (null == this.webViewer)
            return;
        final Theme theme = null != event ? event.newTheme : this.getThemeProvider().getTheme();
        final ThemeSet themeSet = (ThemeSet) theme;

        if (null == themeSet)
            return;

        final Node themeMarker = this.toolbar.lookup("#web-theme-marker");
        if (null == themeMarker) {
            applyWebTheme(themeSet.themes.isEmpty() ? null : themeSet.themes.iterator().next());
            return;
        }
        ObservableList<Node> toolbarItems = this.toolbar.getAlignedItems();
        int themeToolsIdx = toolbarItems.indexOf(themeMarker) + 1;

        // clean old
        for (int i = themeToolsIdx; i < toolbarItems.size(); i++) {
            if (toolbarItems.get(i).getProperties().containsKey(themeMarker))
                toolbarItems.remove(i--);
        }
        themeMarker.setUserData(null);
        //
        final ToggleGroup themeToolsGroup = new ToggleGroup();
        themeToolsGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (null == nv) return;
            final Theme webTheme = (Theme) nv.getUserData();
            if (Objects.equals(webTheme, themeMarker.getUserData()))
                return;
            themeMarker.setUserData(webTheme);
            this.applyWebTheme(webTheme);
        });
        final List<RadioButton> themeToolsList = themeSet.themes.stream().map(v -> {
            final RadioButton themeTool = new RadioButton();
            themeTool.setToggleGroup(themeToolsGroup);
            themeTool.setUserData(v);
            themeTool.setTooltip(new Tooltip(v.title));
            themeTool.getProperties().put(themeMarker, true);

            final MaterialIconView icon = new MaterialIconView(MaterialIcon.LENS);
            icon.setFill(Color.valueOf(v.accentColor));
            themeTool.setGraphic(icon);

            return themeTool;
        }).collect(Collectors.toList());
        toolbarItems.addAll(themeToolsIdx, themeToolsList);

        if (!themeToolsList.isEmpty())
            themeToolsList.get(0).fire();
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
            // init tree
            this.bookBasic.onViewportInit();
            this.handleChaptersTreeViewEnterOrDoubleClickAction(null, bookBasic.defaultTreeItem);
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

    Chapter currentChapter;
    HanLang displayHan;

    void handleChaptersTreeViewEnterOrDoubleClickAction(final InputEvent event, final TreeItem<Chapter> treeItem) {
        if (null == treeItem || null != event && !treeItem.isLeaf()) return;
        openChapter(event, treeItem.getValue());
    }

    public void openChapter(InputEvent event, Chapter chapter) {
        if (null == chapter || Objects.equals(currentChapter, chapter)) return;

        if (null != currentChapter && chapter.path.equals(currentChapter.path)) {
            currentChapter = chapter;
            if (null != chapter.start) {
                webViewer.executeScript("setScrollTop1BySelectors(\"".concat(chapter.start.toString()).concat("\")"));
            }
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
                webViewer.executeScript(StringHelper.concat("setScrollTop1BySelectors(\"", selector, "\", ", percent, ")"));
                webViewer.addEventHandler(KeyEvent.KEY_PRESSED, event1 -> {
                    if (event1.isControlDown() && event1.getCode() == KeyCode.F) {
                        webViewer.executeScript("handleOnSearchInPage()");
                    }
                });
            } else if (null != chapter.start) {
                webViewer.executeScript("setScrollTop1BySelectors(\"".concat(currentChapter.start.toString()).concat("\")"));
            }
            this.webViewer.widthProperty().removeListener(this::handleWebViewBodyResize);
            this.webViewer.widthProperty().addListener(this::handleWebViewBodyResize);
            DevtoolHelper.LOG.info("load htmlDocFile used time: " + (System.currentTimeMillis() - st) + ", " + htmlDoc);
            //
            this.bookmarks.onViewportInit();
            this.favorites.onViewportInit();
        });

        this.webViewer.getEngine().load(Path.of(htmlDoc).toUri().toString());
        UserPrefs.recents.setProperty(book.id + ".chapter", currentChapter.id);
        getEventBus().fireEvent(new ChapterEvent(ChapterEvent.OPENED, book, currentChapter));
    }

    private void handleWebViewBodyResize(Observable o) {
        webViewer.executeScript("beforeOnResizeBody()");
    }

    private void handleApplicationEventStopping(ApplicationEvent event) {
        saveUserExperienceData();
    }

    public void saveUserExperienceData() {
        try {
            final double scrollTopPercentage = webViewer.getScrollTopPercentage();
            UserPrefs.recents.setProperty(book.id + ".percent", scrollTopPercentage);

            final String selector = webViewer.executeScript("getScrollTop1Selector()");
            UserPrefs.recents.setProperty(book.id + ".selector", selector);
        } catch (Exception ignore) {
        }
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
    }

    private static class WebIncl {
        private static final String[] includeNames = {
                "jquery.min.js",
                "jquery.ext.js",
                "jquery.isinviewport.js",
                "jquery.highlight.js",
                "jquery.scrollto.js",
                "popper.min.js",
                "tippy-bundle.umd.min.js",
                "rangy-core.js",
                "rangy-serializer.js",
//                "rangy-classapplier.js",
//                "rangy-textrange.js",
//                "rangy-highlighter.js",
//                "rangy-selectionsaverestore.js",
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
