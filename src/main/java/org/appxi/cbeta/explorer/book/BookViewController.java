package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import netscape.javascript.JSObject;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.bookdata.BookdataController;
import org.appxi.cbeta.explorer.bookdata.BookmarksController;
import org.appxi.cbeta.explorer.bookdata.FavoritesController;
import org.appxi.cbeta.explorer.dao.Bookdata;
import org.appxi.cbeta.explorer.dao.BookdataType;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.BookdataEvent;
import org.appxi.cbeta.explorer.event.GenericEvent;
import org.appxi.cbeta.explorer.event.SearcherEvent;
import org.appxi.cbeta.explorer.search.LookupViewEx;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.holder.IntHolder;
import org.appxi.javafx.control.*;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.helper.ToastHelper;
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
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BookViewController extends WorkbenchMainViewController {
    private final EventHandler<ThemeEvent> handleThemeChanged = this::handleThemeChanged;
    private final EventHandler<ApplicationEvent> handleApplicationEventStopping = this::handleApplicationEventStopping;
    private final EventHandler<GenericEvent> handleDisplayHanChanged = this::handleDisplayHanChanged;
    private final EventHandler<GenericEvent> handleDisplayZoomChanged = this::handleDisplayZoomChanged;
    private final InvalidationListener handleWebViewBodyResize = this::handleWebViewBodyResize;
    private final BookDocument bookDocument;
    public final CbetaBook book;

    TabPane sideViews;

    BookBasicController bookBasic;
    BookmarksController bookmarks;
    FavoritesController favorites;

    WebViewer webViewer;
    ToolBarEx toolbar;
    WebViewFinder webViewFinder;

    private final BlockingView blockingView = new BlockingView();

    public BookViewController(CbetaBook book, WorkbenchApplication application) {
        super(book.id, book.title, application);
        this.book = book;
        this.bookDocument = new BookDocumentEx(book);
    }

    @Override
    public String createToolTooltipText() {
        if (StringHelper.isBlank(book.authorInfo))
            return book.title;
        return book.title.concat("\n").concat(book.authorInfo);
    }

    @Override
    public Node createToolIconGraphic(boolean sideToolOrElseViewTool) {
        return null;
    }

    @Override
    protected void onViewportInitOnce(StackPane viewport) {
        viewport.getStyleClass().add("book-viewer");

        final BorderPane borderPane = new BorderPane();
        viewport.getChildren().add(borderPane);
        //
        this.webViewer = new WebViewer();
        this.webViewer.addEventHandler(KeyEvent.KEY_PRESSED, this::handleWebViewShortcuts);
        borderPane.setCenter(this.webViewer);

        this.toolbar = new ToolBarEx();
        this.initToolbar(viewport);
        borderPane.setTop(this.toolbar);
        //
        this.sideViews = new TabPaneExt();
        this.sideViews.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(this.sideViews, Priority.ALWAYS);
        //
        this.bookBasic = new BookBasicController(getApplication(), this);
        this.bookmarks = new BookmarksController(getApplication(), this.book);
        this.favorites = new FavoritesController(getApplication(), this.book);
        //
        final Tab tab1 = new Tab("基本", this.bookBasic.getViewport());
        final Tab tab2 = new Tab("书签", this.bookmarks.getViewport());
        final Tab tab3 = new Tab("收藏", this.favorites.getViewport());
        //
        this.sideViews.getTabs().setAll(tab1, tab2, tab3);
    }

    protected void initToolbar(StackPane viewport) {
        addTool_SideControl();
        this.toolbar.addLeft(new Separator(Orientation.VERTICAL));
        addTool_Goto_PrevNext(viewport);
        addTool_Bookmark();
        addTool_Favorite();
//        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
//        addTool_Themes();
        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
        addTool_WrapLines();
        addTool_WrapPages();
        addTool_FirstLetterIndent();
        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
        addTool_EditorMark();
        //
        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
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

    private Button gotoPrev, gotoNext, gotoMenu;

    private void addTool_Goto_PrevNext(StackPane viewport) {
        gotoPrev = new Button();
        gotoPrev.getStyleClass().add("goto-prev");
        gotoPrev.setGraphic(new MaterialIconView(MaterialIcon.ARROW_BACK));
        gotoPrev.setTooltip(new Tooltip("上一卷 (Ctrl+左方向键)"));
        gotoPrev.setDisable(true);
        gotoPrev.setOnAction(event -> openChapter((Chapter) gotoPrev.getUserData()));

        gotoNext = new Button();
        gotoNext.getStyleClass().add("goto-next");
        gotoNext.setGraphic(new MaterialIconView(MaterialIcon.ARROW_FORWARD));
        gotoNext.setTooltip(new Tooltip("下一卷 (Ctrl+右方向键)"));
        gotoNext.setDisable(true);
        gotoNext.setOnAction(event -> openChapter((Chapter) gotoNext.getUserData()));

        gotoMenu = new Button();
        gotoMenu.getStyleClass().add("goto-menu");
        gotoMenu.setGraphic(new MaterialIconView(MaterialIcon.NEAR_ME));
        gotoMenu.setTooltip(new Tooltip("转到 (Ctrl+T)"));
        final LookupViewEx<Chapter> lookupView = new LookupViewEx<>(viewport) {
            private final Object AK_INDEX = new Object();

            @Override
            protected int getPrefWidth() {
                return 1000;
            }

            @Override
            protected int getPrefHeight() {
                return 680;
            }

            @Override
            protected String getHeaderText() {
                return "快速跳转本书目录";
            }

            @Override
            protected String getUsagesText() {
                return """
                        >> 不分简繁任意汉字匹配（以感叹号起始则强制区分）；逗号分隔任意字/词/短语匹配；卷编号/卷名/章节名匹配；
                        >> 斜杠'/'匹配经卷目录；#号起始开启本书卷/行定位；
                        >> 快捷键：Ctrl+T 在阅读视图中开启；ESC 或 点击透明区 退出此界面；上下方向键选择列表项；回车键打开；
                        """;
            }

            @Override
            protected void updateItemOnce(Labeled labeled, Chapter item) {
                labeled.setText(item.hasAttr(AK_INDEX)
                        ? StringHelper.concat(item.attrStr(AK_INDEX), " / ", item.title)
                        : item.title);
            }

            @Override
            protected Collection<Chapter> search(String searchText, String[] searchWords, int resultLimit) {
                final Collection<Chapter> result = new ArrayList<>(resultLimit);

                TreeHelper.walkLeafs(bookBasic.tocsTree.getRoot(), itm -> {
                    String content = itm.getValue().title;
                    if (null != content && (searchText.isEmpty() || content.contains(searchText))) {
                        result.add(itm.getValue());
                    }

                    return result.size() > resultLimit;
                });
                if (result.size() < resultLimit) {
                    final IntHolder index = new IntHolder(0);
                    TreeHelper.walkLeafs(bookBasic.volsTree.getRoot(), itm -> {
                        index.value++;

                        String indexStr = StringHelper.padLeft(index.value, 3, '0');
                        itm.getValue().attr(AK_INDEX, indexStr);

                        String content = StringHelper.concat(indexStr, " / ", itm.getValue().title);
                        if (searchText.isEmpty() || content.contains(searchText)) {
                            result.add(itm.getValue());
                        }

                        return result.size() > resultLimit;
                    });
                }
                return result;
            }

            private final String specialMarker = "#";

            @Override
            protected void convertSearchTermToCommands(String searchTerm, Collection<Chapter> result) {
                String chapter = null;
                boolean lineOrVolume = true;
                Matcher matcher;
                if (searchTerm.isBlank()) {
                    if (!result.isEmpty())
                        return;
                } else if ((matcher = Pattern.compile("p(.*)").matcher(searchTerm)).matches()) {
                    chapter = "p".concat(matcher.group(1));
                } else if ((matcher = Pattern.compile("p\\. (.*)").matcher(searchTerm)).matches()) {
                    String str = matcher.group(1);
                    if (str.matches(".*[a-z]\\d$"))
                        str = str.substring(0, str.length() - 1).concat("0").concat(str.substring(str.length() - 1));
                    str = StringHelper.padLeft(str, 7, '0');
                    chapter = "p".concat(str);
                } else if ((matcher = Pattern.compile("(\\d+)").matcher(searchTerm)).matches()) {
                    chapter = matcher.group(1);
                    lineOrVolume = false;
                } else {
                    chapter = "p".concat(searchTerm);
                }
                if (null != chapter) {
                    String title = "《".concat(book.title).concat("》");
                    if (!lineOrVolume) {
                        chapter = chapter.length() >= 3 ? chapter.substring(0, 3) : StringHelper.padLeft(chapter, 3, '0');
                    }
                    title = StringHelper.concat("转到 >>> 本经：", book.id, " ", title,
                            (lineOrVolume ? "，行号：" : "，卷号：").concat(chapter));
                    result.add(new Chapter(specialMarker, chapter, title, null, null));
                } else {
                    result.add(new Chapter(specialMarker, null, "??? 使用说明：请使用以下几种格式", null, null));
                    result.add(new Chapter(specialMarker, null, "格式1：#p0001a01  表示：转到本经行号p0001a01处", null, null));
                    result.add(new Chapter(specialMarker, null, "格式2：#p. 1a1  表示：转到本经行号p0001a01处", null, null));
                    result.add(new Chapter(specialMarker, null, "格式3：#1  表示：转到本经第001卷", null, null));
                    result.add(new Chapter(specialMarker, null, "格式4：#001  表示：转到本经第001卷", null, null));
                    result.add(new Chapter(specialMarker, null, "格式5：#0001a01  表示：转到本经行号p0001a01处", null, null));
                }
            }

            @Override
            protected void handleEnterOrDoubleClickActionOnSearchResultList(InputEvent event, Chapter item) {
                if (null == item.id)
                    return;
                Chapter chapter = item;
                if (item.type == specialMarker) {
                    chapter = new Chapter();
                    chapter.id = "#";
                    chapter.path = item.id;
                }

                hide();
                openChapter(chapter);
            }

            @Override
            public void hide() {
                super.hide();
                webViewer.getViewer().requestFocus();
            }
        };
        gotoMenu.setOnAction(event -> lookupView.show());

        //
        this.toolbar.addLeft(gotoPrev, gotoNext, gotoMenu);
    }

    private void update_Goto_PrevNext() {
        final List<Chapter> list = new ArrayList<>();
        TreeHelper.walkLeafs(bookBasic.volsTree.getRoot(), v -> {
            list.add(v.getValue());
            return false;
        });
        Chapter prev = null, next = null;
        for (int i = 0; i < list.size(); i++) {
            Chapter item = list.get(i);
            if (null == item) continue;
            if (Objects.equals(item.path, openedChapter.path)) {
                prev = i - 1 < 0 ? null : list.get(i - 1);
                next = i + 1 >= list.size() ? null : list.get(i + 1);
                break;
            }
        }
        gotoPrev.setUserData(prev);
        gotoPrev.setDisable(null == prev);

        gotoNext.setUserData(next);
        gotoNext.setDisable(null == next);
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

    private void addTool_WrapPages() {
        final ToggleButton button = new ToggleButton();
        button.setGraphic(new MaterialIconView(MaterialIcon.VIEW_DAY));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("折页显示"));
        button.setOnAction(event -> webViewer.executeScript("handleOnWrapPages()"));
        this.toolbar.addRight(button);
    }

    private void addTool_FirstLetterIndent() {
        final ToggleButton button = new ToggleButton();
        button.setGraphic(new MaterialIconView(MaterialIcon.FORMAT_INDENT_INCREASE));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("首行标点对齐"));
        button.setSelected(true);
        button.setOnAction(event -> webViewer.executeScript("handleOnPrettyIndent()"));
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
        button.setOnAction(event -> this.handleEventToCreateBookdata(BookdataType.bookmark));
        this.toolbar.addRight(button);
    }

    private void addTool_Favorite() {
        final Button button = new Button();
        button.setGraphic(new MaterialIconView(MaterialIcon.STAR_BORDER));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("添加收藏"));
        button.setOnAction(event -> this.handleEventToCreateBookdata(BookdataType.favorite));
        this.toolbar.addRight(button);
    }

    private void addTool_SearchInPage() {
        webViewFinder = new WebViewFinder(this.webViewer, inputText -> {
            // 允许输入简繁体汉字
            if (!inputText.isEmpty() && (inputText.charAt(0) == '!' || inputText.charAt(0) == '！')) {
                // 为避免自动转换失误导致检索失败，此处特殊处理，允许以感叹号开始的字符串不自动转换简繁体
                inputText = inputText.substring(1).strip();
            } else {
                // 页内查找以显示文字为准，此处转换以匹配目标文字
                inputText = ChineseConvertors.convert(inputText.strip(), null, AppContext.getDisplayHanLang());
            }
            return inputText;
        });

        webViewFinder.prev.setGraphic(new MaterialIconView(MaterialIcon.ARROW_UPWARD));
        webViewFinder.prev.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        webViewFinder.next.setGraphic(new MaterialIconView(MaterialIcon.ARROW_DOWNWARD));
        webViewFinder.next.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        this.toolbar.addRight(webViewFinder);
    }


    /* //////////////////////////////////////////////////////////////////// */
    private void applyWebTheme(Theme webTheme) {
        if (null == this.webViewer)
            return;

        byte[] allBytes = (":root {--zoom: " + AppContext.getDisplayZoomLevel() + " !important;}").getBytes();
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
        getEventBus().addEventHandler(ThemeEvent.CHANGED, handleThemeChanged);
        getEventBus().addEventHandler(GenericEvent.DISPLAY_HAN_CHANGED, handleDisplayHanChanged);
        getEventBus().addEventHandler(GenericEvent.DISPLAY_ZOOM_CHANGED, handleDisplayZoomChanged);
    }

    @Override
    protected String getMainTitle() {
        return StringHelper.isBlank(book.authorInfo) ? book.title : book.title.concat(" -- ").concat(book.authorInfo);
    }

    @Override
    protected void onViewportShowing(boolean firstTime) {
        if (firstTime) {
            // 此处逻辑没问题，但首次加载时会卡，暂时没有有次的办法
            getViewport().getChildren().add(blockingView);
            getEventBus().addEventHandler(ApplicationEvent.STOPPING, handleApplicationEventStopping);
            // 在线程中执行，使不造成一个空白的阻塞
            new Thread(() -> FxHelper.runLater(() -> {
                this.webViewer.getEngine().setUserDataDirectory(UserPrefs.confDir().toFile());
                // apply theme
                this.handleThemeChanged(null);
                this.webViewer.setContextMenuBuilder(this::handleWebViewContextMenu);
                // init tree
                this.bookBasic.onViewportShow(true);
                openChapter(null);

                this.bookmarks.onViewportShow(true);
                this.favorites.onViewportShow(true);
            })).start();
        } else {
            getEventBus().fireEvent(new BookEvent(BookEvent.VIEW, book, openedChapter));
        }
    }

    @Override
    protected void onViewportHiding() {
        super.onViewportHiding();
        saveUserExperienceData();
        getEventBus().fireEvent(new BookEvent(BookEvent.HIDE, this.book, openedChapter));
    }

    @Override
    protected void onViewportClosing() {
        super.onViewportClosing();
        saveUserExperienceData();
        getEventBus().removeEventHandler(ThemeEvent.CHANGED, handleThemeChanged);
        getEventBus().removeEventHandler(ApplicationEvent.STOPPING, handleApplicationEventStopping);
        getEventBus().removeEventHandler(GenericEvent.DISPLAY_HAN_CHANGED, handleDisplayHanChanged);
        getEventBus().removeEventHandler(GenericEvent.DISPLAY_ZOOM_CHANGED, handleDisplayZoomChanged);
        getEventBus().fireEvent(new BookEvent(BookEvent.CLOSE, this.book, openedChapter));
    }

    private void handleDisplayHanChanged(GenericEvent event) {
        if (null == this.webViewer)
            return;
        saveUserExperienceData();
        this.openedChapter = null;
        openChapter(null);
    }

    private void handleDisplayZoomChanged(GenericEvent event) {
        if (null == this.webViewer)
            return;
        saveUserExperienceData();
        this.handleThemeChanged(null);
        openChapter(null);
    }

    private Chapter openedChapter;

    void openChapter(Chapter chapter) {
        final Chapter posChapter = null != chapter ? chapter : removeAttr(Chapter.class);
        if (null == chapter) {
            chapter = bookBasic.selectChapter(this.book, posChapter);
        } else {
            chapter = bookBasic.selectChapter(this.book, chapter);
        }
        if (null == chapter) return;

        if (null != openedChapter && Objects.equals(chapter.path, openedChapter.path)) {
            openedChapter = chapter;
            gotoChapter(posChapter);
            return;
        }

        if (!getViewport().getChildren().contains(blockingView))
            getViewport().getChildren().add(blockingView);
        openedChapter = chapter;
        final long st = System.currentTimeMillis();
        final HanLang displayHan = AppContext.getDisplayHanLang();
        final String htmlDoc = bookDocument.getVolumeHtmlDocument(openedChapter.path, displayHan,
                body -> ChineseConvertors.convert(
                        StringHelper.concat("<body><article>", body.html(), "</article></body>"),
                        HanLang.hantTW,
                        displayHan
                ),
                WebIncl.getIncludePaths()
        );
        webViewer.setOnLoadSucceedAction(we -> {
            // set an interface object named 'dataApi' in the web engine's page
            final JSObject window = webViewer.executeScript("window");
            window.setMember("dataApi", dataApi);
            //
            gotoChapter(posChapter);
            //
            webViewer.widthProperty().removeListener(handleWebViewBodyResize);
            webViewer.widthProperty().addListener(handleWebViewBodyResize);
            DevtoolHelper.LOG.info("load htmlDocFile used time: " + (System.currentTimeMillis() - st) + ", " + htmlDoc);
            getViewport().getChildren().remove(blockingView);
            getEventBus().fireEvent(new BookEvent(BookEvent.VIEW, book));
        });

        webViewer.getEngine().load(Path.of(htmlDoc).toUri().toString());
        UserPrefs.recents.setProperty(book.id + ".chapter", openedChapter.id);
    }

    private void gotoChapter(Chapter posChapter) {
        update_Goto_PrevNext();

        try {
            if (null != posChapter && posChapter.hasAttr("position.term")) {
                String posTerm = posChapter.removeAttr("position.term");
                String posText = posChapter.removeAttr("position.text");

                List<String> posParts = new ArrayList<>(List.of(posText.split("。")));
                posParts.sort(Comparator.comparingInt(String::length));
                String longText = posParts.get(posParts.size() - 1);
                if (!webViewer.findInPage(longText, true)) {
                    webViewFinder.search(posTerm);
                }
            } else if (null != posChapter && posChapter.hasAttr("position.selector")) {
                webViewer.executeScript(StringHelper.concat("setScrollTop1BySelectors(\"", posChapter.removeAttr("position.selector"), "\")"));
            } else if (null != posChapter && null != posChapter.start) {
                webViewer.executeScript("setScrollTop1BySelectors(\"".concat(posChapter.start.toString()).concat("\")"));
            } else {
                final String selector = UserPrefs.recents.getString(book.id + ".selector", null);
                final double percent = UserPrefs.recents.getDouble(book.id + ".percent", 0);
                if (null != selector) {
                    webViewer.executeScript(StringHelper.concat("setScrollTop1BySelectors(\"", selector, "\")"));
                } else if (null != openedChapter.start) {
                    webViewer.executeScript("setScrollTop1BySelectors(\"".concat(openedChapter.start.toString()).concat("\")"));
                }
            }
        } catch (Throwable ignored) {
            ignored.printStackTrace();
        }
        // 避免在阅读视图时焦点仍然在TAB上（此时快捷键等不起作用）
        webViewer.getViewer().requestFocus();
    }

    private void handleWebViewShortcuts(KeyEvent event) {
        if (event.isShortcutDown()) {
            // Ctrl + F
            if (event.getCode() == KeyCode.F) {
                // 如果有选中文字，则按查找选中文字处理
                String selText = webViewer.executeScript("getValidSelectionText()");
                selText = null == selText ? null : selText.strip().replace('\n', ' ');
                webViewFinder.search(StringHelper.isBlank(selText) ? null : selText);
                event.consume();
            }
            // Ctrl + G, Ctrl + H
            else if (event.getCode() == KeyCode.G || event.getCode() == KeyCode.H) {
                // 如果有选中文字，则按查找选中文字处理
                final String selText = webViewer.executeScript("getValidSelectionText()");
                final String textToSearch = null == selText ? null :
                        AppContext.getDisplayHanLang() != HanLang.hantTW ? selText : "!".concat(selText);
                getEventBus().fireEvent(event.getCode() == KeyCode.G
                        ? SearcherEvent.ofLookup(textToSearch) // LOOKUP
                        : SearcherEvent.ofSearch(textToSearch) // SEARCH
                );
                event.consume();
            }
            // Ctrl + LEFT
            else if (event.getCode() == KeyCode.LEFT) {
                gotoPrev.fire();
                event.consume();
            }
            // Ctrl + RIGHT
            else if (event.getCode() == KeyCode.RIGHT) {
                System.out.println("Ctrl + RIGHT");
                gotoNext.fire();
                event.consume();
            }
            // Ctrl + T
            else if (event.getCode() == KeyCode.T) {
                gotoMenu.fire();
                event.consume();
            }
        }
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

    private ContextMenu handleWebViewContextMenu() {
        String selText = webViewer.executeScript("getValidSelectionText()");
        selText = null == selText ? null : selText.strip().replace('\n', ' ');
        final String validText = StringHelper.isBlank(selText) ? null : selText;
        //
        MenuItem copy = new MenuItem("复制");
        copy.setDisable(null == validText);
        copy.setOnAction(event -> Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, validText)));
        //
        String textTip = null == validText ? "" : "：".concat(StringHelper.trimChars(validText, 8));
        String textForSearch = null == validText ? null : AppContext.getDisplayHanLang() != HanLang.hantTW ? validText : "!".concat(validText);

        MenuItem search = new MenuItem("全文检索".concat(textTip));
        search.setOnAction(event -> getEventBus().fireEvent(SearcherEvent.ofSearch(textForSearch)));

        MenuItem lookup = new MenuItem("快速检索".concat(textTip));
        lookup.setOnAction(event -> getEventBus().fireEvent(SearcherEvent.ofLookup(textForSearch)));

        MenuItem finder = new MenuItem("页内查找".concat(textTip));
        finder.setOnAction(event -> webViewFinder.search(validText));
        //
        MenuItem dictionary = new MenuItem("查词典");
        dictionary.setDisable(true);
        //
        MenuItem bookmark = new MenuItem("添加书签");
        bookmark.setOnAction(event -> this.handleEventToCreateBookdata(BookdataType.bookmark));

        MenuItem favorite = new MenuItem("添加收藏");
        favorite.setOnAction(event -> this.handleEventToCreateBookdata(BookdataType.favorite));


        //
        return new ContextMenu(copy,
                new SeparatorMenuItem(),
                search, lookup, finder,
                new SeparatorMenuItem(),
                dictionary,
                new SeparatorMenuItem(),
                bookmark, favorite
        );
    }

    private void handleEventToCreateBookdata(BookdataType dataType) {
        try {
            final String anchorInfo = webViewer.executeScript("getFavoriteAnchorInfo()");
            BookdataController dataHandle = null;
            if (dataType == BookdataType.bookmark) {
                dataHandle = this.bookmarks;
            } else if (dataType == BookdataType.favorite) {
                dataHandle = this.favorites;
            }

            if (StringHelper.isBlank(anchorInfo))
                throw new RuntimeException("未获取到有效定位信息，无法创建".concat(dataType.title));
            if (anchorInfo.length() > 500)
                throw new RuntimeException("所选文字过多，无法创建".concat(dataType.title));

            final JSONObject json = new JSONObject(anchorInfo);
            final String anchor = json.getString("anchor");
            final String origData = json.getString("text");
            // check exists
            Bookdata data = dataHandle.findDataByAnchor(anchor);
            if (null != data) {
                Alert alert = new Alert(Alert.AlertType.NONE);
                alert.setTitle("重复");
                alert.setContentText("此处已存在".concat(dataType.title).concat("记录，是否删除已有").concat(dataType.title).concat("？"));
                alert.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
                if (ButtonType.YES == FxHelper.withTheme(getApplication(), alert).showAndWait().orElse(null)) {
                    dataHandle.removeData(data);
                    getEventBus().fireEvent(new BookdataEvent(BookdataEvent.REMOVED, data));
                    ToastHelper.toast(getApplication(), "已删除".concat(dataType.title).concat("！"));
                    // TODO update html?
                }
                return;
            }
            //
            Alert alert = new Alert(Alert.AlertType.NONE);
            alert.setTitle("添加".concat(dataType.title));
            final TextArea content = new TextArea();
            content.setWrapText(true);
            content.setText(origData);
            content.setEditable(true);

            final DialogPane pane = new DialogPane();
            pane.setContent(content);
            alert.setDialogPane(pane);
            alert.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
            if (ButtonType.YES == FxHelper.withTheme(getApplication(), alert).showAndWait().orElse(null)) {
                String editData = content.getText().strip();
                if (editData.isBlank())
                    editData = origData;
                data = new Bookdata();
                data.createAt = data.updateAt = new Date();
                data.type = dataType;
                data.book = book.id;
                data.volume = openedChapter.path;
                data.location = book.title;// .concat("/").concat(currentChapter.title);
                data.anchor = anchor;
                data.data = editData.length() > 300 ? editData.substring(0, 300) : editData;
                data.extra = json.toString();
                //
                dataHandle.createData(data);
                getEventBus().fireEvent(new BookdataEvent(BookdataEvent.CREATED, data));
                ToastHelper.toast(getApplication(), "已添加".concat(dataType.title).concat("！"));
            }
        } catch (Exception e) {
            FxHelper.alertError(getApplication(), e);
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
            return ChineseConvertors.convert(input, null, AppContext.getDisplayHanLang());
        }
    }

    private static class WebIncl {
        private static final String[] includeNames = {
                "jquery.min.js",
                "jquery.ext.js",
                "jquery.isinviewport.js",
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
                "app.js"
        };
        private static final String[] includePaths = new String[includeNames.length];

        private static String[] getIncludePaths() {
            if (null != includePaths[0])
                return includePaths;

            synchronized (includePaths) {
                if (null != includePaths[0])
                    return includePaths;

                final Path dir = FxHelper.appDir().resolve("template/web-incl");
                for (int i = 0; i < includeNames.length; i++) {
                    includePaths[i] = dir.resolve(includeNames[i]).toUri().toString();
                }
            }
            return includePaths;
        }
    }
}
