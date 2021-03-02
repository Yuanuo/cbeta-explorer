package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.javafx.control.*;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.helper.ToastHelper;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BookViewController extends WorkbenchMainViewController {
    private final BookDocument bookDocument;
    public final CbetaBook book;
    Chapter initChapter;

    TabPane sideViews;

    BookBasicController bookBasic;
    BookmarksController bookmarks;
    FavoritesController favorites;

    WebViewer webViewer;
    ToolBarEx toolbar;
    WebViewFinder webViewFinder;

    public BookViewController(CbetaBook book, WorkbenchApplication application) {
        this(book, null, application);
    }

    public BookViewController(CbetaBook book, Chapter initChapter, WorkbenchApplication application) {
        super(book.id, book.title, application);
        this.book = book;
        this.bookDocument = new BookDocumentEx(book);
        this.initChapter = initChapter;
    }

    @Override
    public Node createToolIconGraphic(boolean sideToolOrElseViewTool) {
        return null;
    }

    @Override
    protected void onViewportInitOnce(StackPane viewport) {
        final BorderPane borderPane = new BorderPane();
        viewport.getChildren().add(borderPane);
        //
        this.webViewer = new WebViewer();
        borderPane.setCenter(this.webViewer);

        this.toolbar = new ToolBarEx();
        this.initToolbar();
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

    protected void initToolbar() {
        addTool_SideControl();
        addTool_Bookmark();
        addTool_Favorite();
        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
        addTool_FontSize();
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
        getEventBus().addEventHandler(GenericEvent.DISPLAY_HAN_CHANGED, this::handleDisplayHanChanged);
    }

    @Override
    public void onViewportShow(boolean firstTime) {
        if (firstTime) {
            getEventBus().addEventHandler(ApplicationEvent.STOPPING, this::handleApplicationEventStopping);

            // apply theme
            this.handleThemeChanged(null);
            this.webViewer.getEngine().setUserDataDirectory(UserPrefs.confDir().toFile());
            this.webViewer.setContextMenuBuilder(this::handleWebViewContextMenu);
            // init tree
            this.bookBasic.onViewportShow(true);
            this.handleChaptersTreeViewEnterOrDoubleClickAction(null, bookBasic.defaultTreeItem);
            this.bookmarks.onViewportShow(true);
            this.favorites.onViewportShow(true);
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
            getEventBus().removeEventHandler(GenericEvent.DISPLAY_HAN_CHANGED, this::handleDisplayHanChanged);
            setPrimaryTitle(null);
            getEventBus().fireEvent(new BookEvent(BookEvent.CLOSE, this.book, currentChapter));
        }
    }

    private void handleDisplayHanChanged(GenericEvent event) {
        saveUserExperienceData();
        Chapter temp = this.currentChapter;
        this.currentChapter = null;
        openChapter(null, temp);
    }

    Chapter currentChapter;

    private void setCurrentChapter(Chapter chapter) {
        if (null != chapter && null == chapter.id) {
            // detect real chapter from tree
            Chapter realChapter = bookBasic.findChapterByPath(chapter.path, (String) chapter.start);
            chapter = null != realChapter ? realChapter : chapter;
        }
        this.currentChapter = chapter;
    }

    void handleChaptersTreeViewEnterOrDoubleClickAction(final InputEvent event, final TreeItem<Chapter> treeItem) {
        if (null == treeItem || null != event && !treeItem.isLeaf()) return;
        openChapter(event, treeItem.getValue());
    }

    private final BlockingView blockingView = new BlockingView();

    void openChapter(InputEvent event, Chapter chapter) {
        if (null == chapter || Objects.equals(currentChapter, chapter)) return;

        // 避免在阅读视图时焦点仍然在TAB上（此时快捷键等不起作用）
        webViewer.getViewer().requestFocus();
        if (null != currentChapter && chapter.path.equals(currentChapter.path)) {
            setCurrentChapter(chapter);
            if (chapter.hasAttr("position.term")) {
                String posTerm = chapter.removeAttr("position.term");
                String posText = chapter.removeAttr("position.text");

                List<String> posParts = new ArrayList<>(List.of(posText.split("。")));
                posParts.sort(Comparator.comparingInt(String::length));
                String longText = posParts.get(posParts.size() - 1);
                if (!webViewer.findInPage(longText, true)) {
                    webViewFinder.search(posTerm);
                }
            } else if (chapter.hasAttr("position.selector")) {
                webViewer.executeScript(StringHelper.concat("setScrollTop1BySelectors(\"", chapter.removeAttr("position.selector"), "\")"));
            } else if (null != chapter.start) {
                webViewer.executeScript("setScrollTop1BySelectors(\"".concat(chapter.start.toString()).concat("\")"));
            }
            return;
        }

        getViewport().getChildren().add(blockingView);
        new Thread(new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (null != currentChapter && null != currentChapter.id) {
                    getEventBus().fireEvent(new BookEvent(BookEvent.CLOSE, book, currentChapter));
                }
                setCurrentChapter(chapter);

                final long st = System.currentTimeMillis();
                final HanLang displayHan = AppContext.getDisplayHanLang();
                final String htmlDoc = bookDocument.getVolumeHtmlDocument(chapter.path, displayHan,
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
                    if (null != initChapter && initChapter.hasAttr("position.term")) {
                        String posTerm = initChapter.removeAttr("position.term");
                        String posText = initChapter.removeAttr("position.text");

                        List<String> posParts = new ArrayList<>(List.of(posText.split("。")));
                        posParts.sort(Comparator.comparingInt(String::length));
                        String longText = posParts.get(posParts.size() - 1);
                        if (!webViewer.findInPage(longText, true)) {
                            webViewFinder.search(posTerm);
                        }
                    } else if (null != initChapter && initChapter.hasAttr("position.selector")) {
                        webViewer.executeScript(StringHelper.concat("setScrollTop1BySelectors(\"", initChapter.removeAttr("position.selector"), "\")"));
                        initChapter = null;//
                    } else if (null == event) {
                        final String selector = UserPrefs.recents.getString(book.id + ".selector", null);
                        final double percent = UserPrefs.recents.getDouble(book.id + ".percent", 0);
                        webViewer.executeScript(StringHelper.concat("setScrollTop1BySelectors(\"", selector, "\", ", percent, ")"));
                    } else if (null != chapter.start) {
                        webViewer.executeScript("setScrollTop1BySelectors(\"".concat(chapter.start.toString()).concat("\")"));
                    }
                    webViewer.removeEventHandler(KeyEvent.KEY_PRESSED, BookViewController.this::handleWebViewShortcuts);
                    webViewer.addEventHandler(KeyEvent.KEY_PRESSED, BookViewController.this::handleWebViewShortcuts);
                    webViewer.widthProperty().removeListener(BookViewController.this::handleWebViewBodyResize);
                    webViewer.widthProperty().addListener(BookViewController.this::handleWebViewBodyResize);
                    DevtoolHelper.LOG.info("load htmlDocFile used time: " + (System.currentTimeMillis() - st) + ", " + htmlDoc);
                    Platform.runLater(() -> getViewport().getChildren().remove(blockingView));
                });

                Platform.runLater(() -> webViewer.getEngine().load(Path.of(htmlDoc).toUri().toString()));
                UserPrefs.recents.setProperty(book.id + ".chapter", chapter.id);
                return null;
            }
        }).start();
//        getEventBus().fireEvent(new BookEvent(BookEvent.VIEW, book, currentChapter));
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
                data.volume = currentChapter.path;
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
