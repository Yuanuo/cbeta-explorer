package org.appxi.cbeta.app.reader;

import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.BookDocument;
import org.appxi.cbeta.BookDocumentEx;
import org.appxi.cbeta.BookHelper;
import org.appxi.cbeta.Chapter;
import org.appxi.cbeta.app.AppContext;
import org.appxi.cbeta.app.dao.Bookdata;
import org.appxi.cbeta.app.dao.BookdataType;
import org.appxi.cbeta.app.event.BookEvent;
import org.appxi.cbeta.app.event.BookdataEvent;
import org.appxi.cbeta.app.event.GenericEvent;
import org.appxi.cbeta.app.search.LookupLayerEx;
import org.appxi.event.EventHandler;
import org.appxi.holder.IntHolder;
import org.appxi.javafx.app.search.SearcherEvent;
import org.appxi.javafx.app.web.WebViewer;
import org.appxi.javafx.control.TabPaneEx;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.prefs.UserPrefs;
import org.appxi.smartcn.convert.ChineseConvertors;
import org.appxi.smartcn.pinyin.PinyinHelper;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.Attributes;
import org.appxi.util.ext.HanLang;
import org.appxi.util.ext.LookupExpression;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BookXmlReader extends HtmlBasedViewer {
    private final EventHandler<GenericEvent> _onHanLangChanged = event -> {
        saveUserData();
        chapter = null;
        navigate(null);
    };

    private final BookDocument bookDocument;
    public final Book book;

    TabPane sideViews;
    BookBasicController bookBasic;
    BookmarksController bookmarks;
    FavoritesController favorites;

    Chapter chapter;

    public BookXmlReader(Book book, WorkbenchPane workbench) {
        super(workbench);

        this.book = book;
        this.bookDocument = new BookDocumentEx(AppContext.bookcase(), book);

        this.id.set(book.id);
        this.appTitle.unbind();
        this.setTitles(null);
    }

    void setTitles(Chapter chapter) {
        String viewTitle = AppContext.hanText(book.title);
        String viewTooltip = viewTitle;
        String mainTitle = viewTitle;
        if (StringHelper.isNotBlank(book.id)) {
            viewTooltip = book.id.concat(" ").concat(viewTooltip);
            mainTitle = book.id.concat(" ").concat(mainTitle);
        }

        if (book.volumes.size() > 0) {
            short vol = BookHelper.getVolume(chapter);
            String volInfo = vol > 0
                    ? StringHelper.concat('（', vol, '/', book.volumes.size(), "卷）")
                    : StringHelper.concat('（', book.volumes.size(), "卷）");
            viewTooltip = viewTooltip.concat(volInfo);
            mainTitle = mainTitle.concat(volInfo);
        }

        if (StringHelper.isNotBlank(book.authorInfo)) {
            String authorInfo = AppContext.hanText(book.authorInfo);
            viewTooltip = viewTooltip.concat("\n").concat(authorInfo);
            mainTitle = mainTitle.concat(" by ").concat(authorInfo);
        }

        this.title.set(StringHelper.trimChars(viewTitle, 20));
        this.tooltip.set(viewTooltip);
        this.appTitle.set(mainTitle);
    }

    @Override
    public void postConstruct() {
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void activeViewport(boolean firstTime) {
        if (firstTime) {
            super.activeViewport(true);
        } else {
            app.eventBus.fireEvent(new BookEvent(BookEvent.VIEW, book, chapter));
        }
    }

    @Override
    public void inactiveViewport(boolean closing) {
        super.inactiveViewport(closing);

        app.eventBus.fireEvent(new BookEvent(closing ? BookEvent.CLOSE : BookEvent.HIDE, this.book, chapter));
    }

    @Override
    protected Chapter location() {
        return chapter;
    }

    @Override
    protected String locationId() {
        return book.id + "." + BookHelper.getVolume(chapter);
    }

    @Override
    public void deinitialize() {
        app.eventBus.removeEventHandler(GenericEvent.HAN_LANG_CHANGED, _onHanLangChanged);
        super.deinitialize();
    }

    @Override
    public void initialize() {
        super.initialize();
        app.eventBus.addEventHandler(GenericEvent.HAN_LANG_CHANGED, _onHanLangChanged);
        //
        this.addTools();
        //
        this.sideViews = new TabPaneEx();
        this.sideViews.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(this.sideViews, Priority.ALWAYS);
        //
        this.bookBasic = new BookBasicController(workbench, this);
        this.bookmarks = new BookmarksController(workbench, this.book);
        this.favorites = new FavoritesController(workbench, this.book);
        //
        final Tab tab1 = new Tab("目录", this.bookBasic.getViewport());
        final Tab tab2 = new Tab("书签", this.bookmarks.getViewport());
        final Tab tab3 = new Tab("收藏", this.favorites.getViewport());
        //
        this.sideViews.getTabs().setAll(tab1, tab2, tab3);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    protected void addTools() {
        addTool_SideControl();
        webPane.getTopBar().addLeft(new Separator(Orientation.VERTICAL));
        addTool_Goto();
        addTool_Bookmark();
        addTool_Favorite();
        webPane.getTopBar().addRight(new Separator(Orientation.VERTICAL));
        addTool_WrapLines();
        addTool_WrapPages();
        addTool_FirstLetterIndent();
        webPane.getTopBar().addRight(new Separator(Orientation.VERTICAL));
        addTool_EditingMarks();
        //
        webPane.getTopBar().addRight(new Separator(Orientation.VERTICAL));
        addTool_FindInPage();
    }

    private void addTool_SideControl() {
        final Button button = MaterialIcon.IMPORT_CONTACTS.flatButton();
        button.setTooltip(new Tooltip("显示本书相关数据（目录、书签等）"));
        button.setOnAction(event -> workbench.selectSideTool(BookDataPlaceController.getInstance().id.get()));
        webPane.getTopBar().addLeft(button);
    }

    private Button gotoPrev, gotoNext, gotoMenu;
    private LookupLayerEx gotoMenuLayer;

    protected void addTool_Goto() {
        gotoPrev = MaterialIcon.ARROW_BACK.flatButton();
        gotoPrev.setTooltip(new Tooltip("上一卷 (Ctrl+左方向键)"));
        gotoPrev.setDisable(true);
        gotoPrev.setOnAction(event -> navigate((Chapter) gotoPrev.getUserData()));

        gotoNext = MaterialIcon.ARROW_FORWARD.flatButton();
        gotoNext.setTooltip(new Tooltip("下一卷 (Ctrl+右方向键)"));
        gotoNext.setDisable(true);
        gotoNext.setOnAction(event -> navigate((Chapter) gotoNext.getUserData()));

        gotoMenu = MaterialIcon.NEAR_ME.flatButton();
        gotoMenu.setTooltip(new Tooltip("转到 (Ctrl+T)"));
        gotoMenu.setOnAction(event -> {
            if (null == gotoMenuLayer) {
                gotoMenuLayer = new LookupLayerImpl(viewport);
            }
            gotoMenuLayer.show(null);
        });

        //
        webPane.getTopBar().addLeft(gotoPrev, gotoNext, gotoMenu);
    }

    private void updateTool_Goto() {
        final List<Chapter> list = new ArrayList<>();
        TreeHelper.walkLeafs(bookBasic.volTree.getRoot(), (treeItem, itemValue) -> list.add(itemValue));
        Chapter prev = null, next = null;
        for (int i = 0; i < list.size(); i++) {
            Chapter item = list.get(i);
            if (null == item) continue;
            if (Objects.equals(item.path, chapter.path)) {
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

    private void addTool_WrapLines() {
        final ToggleButton button = MaterialIcon.WRAP_TEXT.flatToggle();
        button.setTooltip(new Tooltip("折行显示"));
        button.setOnAction(event -> webPane.executeScript("handleOnWrapLines()"));
        webPane.getTopBar().addRight(button);
    }

    private void addTool_WrapPages() {
        final ToggleButton button = MaterialIcon.VIEW_DAY.flatToggle();
        button.setTooltip(new Tooltip("折页显示"));
        button.setOnAction(event -> webPane.executeScript("handleOnWrapPages()"));
        webPane.getTopBar().addRight(button);
    }

    private void addTool_FirstLetterIndent() {
        final ToggleButton button = MaterialIcon.FORMAT_INDENT_INCREASE.flatToggle();
        button.setTooltip(new Tooltip("首行标点对齐"));
        button.setSelected(true);
        button.setOnAction(event -> webPane.executeScript("handleOnPrettyIndent()"));
        webPane.getTopBar().addRight(button);
    }

    private void addTool_EditingMarks() {
        final ToggleGroup marksGroup = new ToggleGroup();
        final int usedMarks = UserPrefs.prefs.getInt("viewer.edit.marks", -1);

        final ToggleButton markOrigInline = new ToggleButton("原编注");
        markOrigInline.getStyleClass().add("flat");
        markOrigInline.setTooltip(new Tooltip("在编注点直接嵌入原编注内容"));
        markOrigInline.setUserData(0);
        markOrigInline.setSelected(usedMarks == (int) markOrigInline.getUserData());

        final ToggleButton markModSharp = new ToggleButton("✱标");
        markModSharp.getStyleClass().add("flat");
        markModSharp.setTooltip(new Tooltip("在编注点插入✱号并划动鼠标查看CBETA编注内容"));
        markModSharp.setUserData(1);
        markModSharp.setSelected(usedMarks == (int) markModSharp.getUserData());

        final ToggleButton markModColor = new ToggleButton("着色");
        markModColor.getStyleClass().add("flat");
        markModColor.setTooltip(new Tooltip("在编注点着色被改变的文字并划动鼠标查看CBETA编注内容"));
        markModColor.setUserData(2);
        markModColor.setSelected(usedMarks == (int) markModColor.getUserData());

        final ToggleButton markModPopover = new ToggleButton("着色+原编注");
        markModPopover.getStyleClass().add("flat");
        markModPopover.setTooltip(new Tooltip("在编注点着色被改变的文字并划动鼠标查看原编注+CBETA编注内容"));
        markModPopover.setUserData(3);
        markModPopover.setSelected(usedMarks == (int) markModPopover.getUserData());

        final ToggleButton markModInline = new ToggleButton("CB编注");
        markModInline.getStyleClass().add("flat");
        markModInline.setTooltip(new Tooltip("在编注点直接嵌入CBETA编注内容"));
        markModInline.setUserData(4);
        markModInline.setSelected(usedMarks == (int) markModInline.getUserData());

        marksGroup.getToggles().setAll(markOrigInline, markModInline, markModSharp, markModColor, markModPopover);
        marksGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            UserPrefs.prefs.setProperty("viewer.edit.marks", null == nv ? -1 : nv.getUserData());
            webPane.executeScript("handleOnEditMark(" + (null == nv ? -1 : nv.getUserData()) + ")");
        });

        webPane.getTopBar().addRight(markOrigInline, markModInline, markModSharp, markModColor, markModPopover);
    }

    private void addTool_Bookmark() {
        final Button button = MaterialIcon.BOOKMARK_OUTLINE.flatButton();
        button.setTooltip(new Tooltip("添加书签"));
        button.setOnAction(event -> this.handleEventToCreateBookData(BookdataType.bookmark));
        webPane.getTopBar().addRight(button);
    }

    private void addTool_Favorite() {
        final Button button = MaterialIcon.STAR_BORDER.flatButton();
        button.setTooltip(new Tooltip("添加收藏"));
        button.setOnAction(event -> this.handleEventToCreateBookData(BookdataType.favorite));
        webPane.getTopBar().addRight(button);
    }

    protected void addTool_FindInPage() {
        webFinder().setInputConvertor(inputText -> {
            // 允许输入简繁体汉字
            if (!inputText.isEmpty() && (inputText.charAt(0) == '!' || inputText.charAt(0) == '！')) {
                // 为避免自动转换失误导致检索失败，此处特殊处理，允许以感叹号开始的字符串不自动转换简繁体
                inputText = inputText.substring(1).strip();
            } else {
                // 页内查找以显示文字为准，此处转换以匹配目标文字
                inputText = ChineseConvertors.convert(inputText.strip(), null, AppContext.hanLang());
            }
            return inputText;
        });
    }

    /* //////////////////////////////////////////////////////////////////// */

    @Override
    protected void navigating(final Object location, boolean firstTime) {
        // init tree
        this.bookBasic.activeViewport(firstTime);
        //
        Chapter item = (Chapter) location;
        // 在切换章节时先保存现有进度数据
        if (null != this.chapter) {
            saveUserData();
        }
        //
        final Attributes pos = null != item ? item : popPosition();
        if (null == item && pos instanceof Chapter c) {
            item = bookBasic.selectChapter(this.book, c);
        } else {
            item = bookBasic.selectChapter(this.book, item);
        }
        if (null == item) return;

        // 升级旧数据，从原来以book粒度记录的进度位置升级成以volume为粒度
        upgradeOldSelectorKey(item);

        setTitles(item);

        if (null != this.chapter && Objects.equals(item.path, this.chapter.path)) {
            this.chapter = item;
            this.setPosition(pos);
            if (null != item.anchor && null != pos) {
                pos.attr("anchor", item.anchor);
            }
            position(pos);
            //
            UserPrefs.recents.setProperty(book.id + ".chapter", this.chapter.id);
            //
            return;
        }
        this.chapter = item;
        this.setPosition(pos);
        //
        UserPrefs.recents.setProperty(book.id + ".chapter", this.chapter.id);
        //
        //
        super.navigating(location, firstTime);
        //
        this.bookmarks.activeViewport(firstTime);
        this.favorites.activeViewport(firstTime);
    }

    @Override
    protected WebCallbackImpl createWebCallback() {
        return new WebCallbackImpl();
    }

    @Override
    protected Object createWebContent() {
        final String htmlFile = bookDocument.getVolumeHtmlDocument(chapter.path, AppContext.hanLang(),
                body -> AppContext.hanText(StringHelper.concat("<body><article>", body.html(), "</article></body>")),
                HtmlBasedViewer.getWebIncludeURIsEx().toArray(new String[0])
        );
        return Path.of(htmlFile);
    }

    @Override
    protected void onWebEngineLoadSucceeded() {
        super.onWebEngineLoadSucceeded();

        updateTool_Goto();
        //
        app.eventBus.fireEvent(new BookEvent(BookEvent.VIEW, book));
        //
        FxHelper.runThread(100, () -> {
            webFinder().clear.fire();
            //
            webPane.getTopBar().lookupAll(".toggle-button").forEach(node -> {
                if (node instanceof ToggleButton toggle && toggle.isSelected()) {
                    if (toggle.getToggleGroup() == null) {
                        toggle.fireEvent(new ActionEvent());
                    } else {
                        toggle.getToggleGroup().selectToggle(null);
                        toggle.getToggleGroup().selectToggle(toggle);
                    }
                }
            });
        });
    }

    @Override
    protected void onWebPaneShortcutsPressed(KeyEvent event) {
        if (event.isConsumed()) {
            return;
        }
        // Ctrl + LEFT
        if (event.isShortcutDown() && event.getCode() == KeyCode.LEFT) {
            gotoPrev.fire();
            event.consume();
            return;
        }
        // Ctrl + RIGHT
        if (event.isShortcutDown() && event.getCode() == KeyCode.RIGHT) {
            gotoNext.fire();
            event.consume();
            return;
        }
        // Ctrl + T
        if (event.isShortcutDown() && event.getCode() == KeyCode.T) {
            gotoMenu.fire();
            event.consume();
            return;
        }
        super.onWebPaneShortcutsPressed(event);
    }

    @Override
    protected void onWebViewContextMenuRequest(List<MenuItem> model) {
        super.onWebViewContextMenuRequest(model);
        //
        String origText = webPane.executeScript("getValidSelectionText()");
        String trimText = null == origText ? null : origText.strip().replace('\n', ' ');
        final String availText = StringHelper.isBlank(trimText) ? null : trimText;
        //
        MenuItem copyRef = new MenuItem("复制为引用");
        copyRef.setDisable(null == availText || !book.path.startsWith("toc/"));
        copyRef.setOnAction(event -> {
            try {
                String refInfoStr = webPane.executeScript("getValidSelectionReferenceInfo2()");
                String[] refInfo = refInfoStr.split("\\|", 3);
                if (refInfo.length < 2) return;
                if (refInfo[0].isBlank()) return;
                if (refInfo[1].isBlank() || refInfo[1].length() < 6) refInfo[1] = refInfo[0];

                // 《長阿含經》卷1：「玄旨非言不傳」(CBETA 2021.Q3, T01, no. 1, p. 1a5-6)
                final StringBuilder buff = new StringBuilder();
                buff.append("《").append(this.book.title).append("》卷");
                String serial = "00";
                final short vol = BookHelper.getVolume(chapter);
                if (vol > 0) {
                    buff.append(vol);
                    serial = book.volumes.get(vol);
                } else {
                    buff.append("00");
                }

                buff.append("：「");
                buff.append(null == availText ? "" : availText);
                buff.append("」(CBETA ").append(AppContext.bookcase().getQuarterlyVersion()).append(", ");
                buff.append(book.tripitakaId).append(serial);
                buff.append(", no. ").append(book.number.replaceAll("^0+", "")).append(", p. ");
                // span#p0154b10|span#p0154b15
                if (refInfo[0].matches("span#p\\d+[a-z]\\d+")) {
                    String[] refStartInfo = refInfo[0].substring(6).replaceFirst("([a-z])", "@$1@").split("@", 3);
                    String[] refEndInfo = refInfo[1].substring(6).replaceFirst("([a-z])", "@$1@").split("@", 3);
                    buff.append(refStartInfo[0].replaceAll("^0+", ""))
                            .append(refStartInfo[1])
                            .append(refStartInfo[2].replaceAll("^0+", ""));
                    if (!refStartInfo[1].equals(refEndInfo[1]) || !refStartInfo[2].equals(refEndInfo[2])) {
                        buff.append("-");
                        if (!refStartInfo[1].equals(refEndInfo[1])) buff.append(refEndInfo[1]);
                        buff.append(refEndInfo[2].replaceAll("^0+", ""));
                    }
                } else {
                    buff.append("000");
                }
                //
                buff.append(")");
                Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, buff.toString()));
                app.toast("已复制到剪贴板");
            } catch (Throwable t) {
                t.printStackTrace(); // for debug
            }
        });
        //
        String textTip = null == availText ? "" : "：".concat(StringHelper.trimChars(availText, 8));
        String textForSearch = null == availText ? null : AppContext.hanLang() != HanLang.hantTW ? availText : "!".concat(availText);

        MenuItem searchInBook = new MenuItem("全文检索（检索本书）".concat(textTip));
        searchInBook.setOnAction(event -> app.eventBus.fireEvent(SearcherEvent.ofSearch(textForSearch, book)));
        //
        MenuItem bookmark = new MenuItem("添加书签");
        bookmark.setOnAction(event -> this.handleEventToCreateBookData(BookdataType.bookmark));

        MenuItem favorite = new MenuItem("添加收藏");
        favorite.setOnAction(event -> this.handleEventToCreateBookData(BookdataType.favorite));

        //
        model.add(createMenu_copy(origText, availText));
        model.add(copyRef);
        model.add(new SeparatorMenuItem());
        model.add(createMenu_search(textTip, textForSearch));
        model.add(createMenu_searchExact(textTip, textForSearch));
        model.add(searchInBook);
        model.add(createMenu_lookup(textTip, textForSearch));
        model.add(createMenu_finder(textTip, availText));
        model.add(new SeparatorMenuItem());
        model.add(createMenu_dict(availText));
        model.add(createMenu_pinyin(availText));
        model.add(new SeparatorMenuItem());
        model.add(bookmark);
        model.add(favorite);
    }

    private void handleEventToCreateBookData(BookdataType dataType) {
        try {
            final String anchorInfo = webPane.executeScript("getFavoriteAnchorInfo()");
            BookDataController dataHandle = null;
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
            @SuppressWarnings("ConstantConditions")
            Bookdata data = dataHandle.findDataByAnchor(anchor);
            if (null != data) {
                Alert alert = new Alert(Alert.AlertType.NONE);
                alert.setTitle("重复");
                alert.setContentText("此处已存在".concat(dataType.title).concat("记录，是否删除已有").concat(dataType.title).concat("？"));
                alert.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
                alert.initOwner(app.getPrimaryStage());
                if (ButtonType.YES == alert.showAndWait().orElse(null)) {
                    dataHandle.removeData(data);
                    app.eventBus.fireEvent(new BookdataEvent(BookdataEvent.REMOVED, data));
                    app.toast("已删除".concat(dataType.title).concat("！"));
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
            alert.initOwner(app.getPrimaryStage());
            if (ButtonType.YES == alert.showAndWait().orElse(null)) {
                String editData = content.getText().strip();
                if (editData.isBlank())
                    editData = origData;
                data = new Bookdata();
                data.createAt = data.updateAt = new Date();
                data.type = dataType;
                data.book = book.id;
                data.volume = chapter.path;
                data.location = book.title;// .concat("/").concat(currentChapter.title);
                data.anchor = anchor;
                data.data = editData.length() > 300 ? editData.substring(0, 300) : editData;
                data.extra = json.toString();
                //
                dataHandle.createData(data);
                app.eventBus.fireEvent(new BookdataEvent(BookdataEvent.CREATED, data));
                app.toast("已添加".concat(dataType.title).concat("！"));
            }
        } catch (Exception e) {
            app.toastError(e.getMessage());
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void upgradeOldSelectorKey(Chapter item) {
        if (UserPrefs.recents.containsProperty(book.id + ".selector")) {
            final String newSelectorKey = book.id + "." + BookHelper.getVolume(item);
            //
            final String selector = UserPrefs.recents.getString(book.id + ".selector", null);
            if (null != selector) {
                UserPrefs.recents.removeProperty(book.id + ".selector");
                UserPrefs.recents.setProperty(newSelectorKey + ".selector", selector);
            }

            final double percent = UserPrefs.recents.getDouble(book.id + ".percent", -1);
            if (percent != -1) {
                UserPrefs.recents.removeProperty(book.id + ".percent");
                UserPrefs.recents.setProperty(newSelectorKey + ".percent", percent);
            }
        }
    }

    class LookupLayerImpl extends LookupLayerEx {
        private final Object AK_INDEX = new Object();

        public LookupLayerImpl(StackPane owner) {
            super(owner);
        }

        @Override
        protected String getHeaderText() {
            return "快捷跳转本书目录";
        }

        @Override
        protected String getUsagesText() {
            return """
                    >> 支持简繁任意汉字匹配（以感叹号起始则强制区分）；支持按拼音（全拼）匹配，使用双引号包含则实行精确匹配；
                    >> 支持复杂条件(+/AND,默认为OR)：例1：1juan +"bu kong" AND 仪轨；例2：1juan +("bu kong" 仪轨 OR "yi gui")
                    >> 逗号分隔任意字/词/短语匹配；卷编号/卷名/章节名匹配；斜杠'/'匹配经卷目录；#号起始开启本书卷/行定位；
                    >> 快捷键：Ctrl+T 在阅读视图中开启；ESC 或 点击透明区 退出此界面；上下方向键选择列表项；回车键打开；
                    """;
        }

        @Override
        protected void updateItemLabel(Labeled labeled, Object data) {
            if (data instanceof String str) {
                labeled.setText(str.split("#", 2)[1]);
            } else if (data instanceof Chapter item) {
                labeled.setText(item.hasAttr(AK_INDEX)
                        ? StringHelper.concat(item.attrStr(AK_INDEX), " / ", AppContext.hanText(item.title))
                        : AppContext.hanText(item.title));

                super.updateItemLabel(labeled, data);
            } else super.updateItemLabel(labeled, data);
        }

        @Override
        protected void lookupByKeywords(String lookupText, int resultLimit,
                                        List<LookupResultItem> result, Set<String> usedKeywords) {
            final boolean isInputEmpty = lookupText.isBlank();
            Optional<LookupExpression> optional = isInputEmpty ? Optional.empty() : LookupExpression.of(lookupText,
                    (parent, text) -> new LookupExpression.Keyword(parent, text) {
                        @Override
                        public double score(Object data) {
                            final String text = null == data ? "" : data.toString();
                            if (this.isAsciiKeyword()) {
                                String dataInAscii = PinyinHelper.pinyin(text);
                                if (dataInAscii.contains(this.keyword())) return 1;
                            }
                            return super.score(data);
                        }
                    });
            if (!isInputEmpty && optional.isEmpty()) {
                // not a valid expression
                return;
            }
            final LookupExpression lookupExpression = optional.orElse(null);
            TreeHelper.filterLeafs(bookBasic.tocTree.getRoot(), (treeItem, itemValue) -> {
                if (null == itemValue || null == itemValue.title) return false;
                double score = isInputEmpty ? 1 : lookupExpression.score(itemValue.title);
                if (score > 0) result.add(new LookupResultItem(itemValue, score));
                return isInputEmpty && result.size() > resultLimit;
            });

            final IntHolder index = new IntHolder(0);
            TreeHelper.filterLeafs(bookBasic.volTree.getRoot(), (treeItem, itemValue) -> {
                if (null == itemValue || null == itemValue.title) return false;
                index.value++;

                String indexStr = StringHelper.padLeft(index.value, 3, '0');
                itemValue.attr(AK_INDEX, indexStr);

                String text = StringHelper.concat(indexStr, " / ", itemValue.title);
                double score = isInputEmpty ? 1 : lookupExpression.score(text);
                if (score > 0) result.add(new LookupResultItem(itemValue, score));
                return isInputEmpty && result.size() > resultLimit;
            });
            //
            if (null != lookupExpression)
                lookupExpression.keywords().forEach(k -> usedKeywords.add(k.keyword()));
        }

        @Override
        protected void lookupByCommands(String searchTerm, Collection<Object> result) {
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
                result.add(chapter.concat("#").concat(AppContext.hanText(title)));
            } else {
                result.add("#??? 使用说明：请使用以下几种格式");
                result.add("#格式1：#p0001a01  表示：转到本经行号p0001a01处");
                result.add("#格式2：#p. 1a1  表示：转到本经行号p0001a01处");
                result.add("#格式3：#1  表示：转到本经第001卷");
                result.add("#格式4：#001  表示：转到本经第001卷");
                result.add("#格式5：#0001a01  表示：转到本经行号p0001a01处");
            }
        }

        @Override
        protected void handleEnterOrDoubleClickActionOnSearchResultList(InputEvent event, Object data) {
            Chapter chapter = null;
            if (data instanceof String str) {
                String[] arr = str.split("#", 2);
                if (arr[0].isEmpty()) return;
                chapter = new Chapter();
                chapter.id = "#";
                chapter.path = arr[0];
            } else if (data instanceof Chapter item) {
                if (null == item.id) return;
                chapter = item;
            }
            if (null != chapter) {
                hide();
                navigate(chapter);
            }
        }

        @Override
        public void hide() {
            super.hide();
            webPane.webView().requestFocus();
        }
    }

    public class WebCallbackImpl extends WebViewer.WebCallbackImpl {
        /**
         * 用于将输入文字转换成与实际显示的相同，以方便页内查找
         */
        public String convertToDisplayHan(String input) {
            return ChineseConvertors.convert(input, null, AppContext.hanLang());
        }
    }
}
