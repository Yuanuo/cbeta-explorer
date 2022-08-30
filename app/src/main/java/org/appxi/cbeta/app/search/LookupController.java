package org.appxi.cbeta.app.search;

import javafx.scene.control.Labeled;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.appxi.book.Chapter;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.app.AppContext;
import org.appxi.cbeta.app.event.BookEvent;
import org.appxi.cbeta.app.event.GenericEvent;
import org.appxi.cbeta.app.explorer.BooksProfile;
import org.appxi.javafx.app.search.SearcherEvent;
import org.appxi.javafx.control.LookupLayer;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPart;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.util.StringHelper;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LookupController extends WorkbenchPartController implements WorkbenchPart.SideTool {
    private LookupDatabase lookupDatabase;

    public LookupController(WorkbenchPane workbench) {
        super(workbench);

        this.id.set("LOOKUP");
        this.tooltip.set("快捷检索 (Ctrl+G)");
        this.graphic.set(MaterialIcon.NEAR_ME.graphic());
    }

    @Override
    public boolean sideToolAlignTop() {
        return true;
    }

    private long lastShiftKeyPressedTime;

    @Override
    public void postConstruct() {
        app.getPrimaryScene().getAccelerators().put(new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN),
                () -> this.activeViewport(false));
        app.getPrimaryScene().addEventHandler(KeyEvent.KEY_PRESSED, evt -> {
            if (evt.getCode() == KeyCode.SHIFT) {
                final long currShiftKeyPressedTime = System.currentTimeMillis();
                if (currShiftKeyPressedTime - lastShiftKeyPressedTime <= 400) {
                    this.activeViewport(false);
                } else lastShiftKeyPressedTime = currShiftKeyPressedTime;
            }
        });
        app.eventBus.addEventHandler(SearcherEvent.LOOKUP,
                event -> this.lookup(null != event.text ? event.text.strip() : null));
        this.lookupDatabase = new LookupDatabase();
        app.eventBus.addEventHandler(GenericEvent.PROFILE_READY, event -> {
            if (null != lookupLayer) lookupLayer.reset();
            lookupDatabase.reload();
        });
        // 当显示汉字类型改变时需要同步更新lookupView
        app.eventBus.addEventHandler(GenericEvent.HAN_LANG_CHANGED,
                event -> Optional.ofNullable(this.lookupLayer).ifPresent(LookupLayer::refresh));
    }

    @Override
    public void activeViewport(boolean firstTime) {
        lookup(null);
    }

    private LookupLayerEx lookupLayer;

    private void lookup(String text) {
        if (null == lookupLayer) {
            lookupLayer = new LookupLayerImpl();

            app.eventBus.addEventHandler(BookEvent.VIEW, event -> lookupLayer.refresh());
        }
        lookupLayer.show(text != null ? text : lookupLayer.inputQuery);
    }

    class LookupLayerImpl extends LookupLayerEx {
        public LookupLayerImpl() {
            super(app.getPrimaryGlass());
        }

        @Override
        protected int getPaddingSizeOfParent() {
            return 200;
        }

        @Override
        protected String getHeaderText() {
            return "快捷检索";
        }

        @Override
        protected String getUsagesText() {
            return """
                    >> 支持简繁汉字匹配（以感叹号起始则强制区分）；支持按拼音（全拼）匹配，使用双引号包含则实行精确匹配；
                    >> 支持复杂条件(+/AND,默认为OR)：例1：1juan +"bu kong" AND 仪轨；例2：1juan +("bu kong" 仪轨 OR "yi gui")
                    >> 空格分隔任意字/词/短语匹配；书名/书号ID/作者/译者/时域/卷数任意匹配；#号起始开启经卷行定位；
                    >> 快捷键：双击Shift 或 Ctrl+G 开启；ESC 或 点击透明区 退出此界面；上/下方向键选择列表项；回车键打开；
                    """;
        }

        @Override
        protected void updateItemLabel(Labeled labeled, Object data) {
            labeled.getStyleClass().remove("visited");
            if (data instanceof LookupDatabase.LookupData item && !Objects.equals(item.extra, "#")) {
                if (null != item.bookId && null != AppContext.recentBooks.getProperty(item.bookId)) {
                    labeled.getStyleClass().add("visited");
                }
                labeled.setText(item.toString());
                super.updateItemLabel(labeled, data);
            } else if (data instanceof String str) {
                labeled.setText(str);
            } else labeled.setText(null == data ? "" : data.toString());
        }

        @Override
        protected void lookupByKeywords(String lookupText, int resultLimit,
                                        List<LookupResultItem> result, Set<String> usedKeywords) {
//                    LookupByPredicate.lookup(lookupText, resultLimit, result, usedKeywords);
            LookupByExpression.lookup(lookupText, resultLimit, result, usedKeywords);
        }

        @Override
        protected void lookupByCommands(String searchTerm, Collection<Object> result) {
            String bookId = null, chapter = null;
            boolean lineOrVolume = true;
            Matcher matcher;
            if (searchTerm.isBlank()) {
                if (!result.isEmpty())
                    return;
            } else if ((matcher = Pattern.compile("([A-Z]+)(\\d+)n(\\d+)[_.#]p(.*)").matcher(searchTerm)).matches()) {
                bookId = matcher.group(1).concat(matcher.group(3));
                chapter = "p".concat(matcher.group(4));
            } else if ((matcher = Pattern.compile("([A-Z]+)(\\d+)[_.#]p(.*)").matcher(searchTerm)).matches()) {
                bookId = matcher.group(1).concat(matcher.group(2));
                chapter = "p".concat(matcher.group(3));
            } else if ((matcher = Pattern.compile("([A-Z]+)(\\d+), no\\. (\\d+), p\\. (.*)").matcher(searchTerm)).matches()) {
                String str = matcher.group(3);
                if (str.matches("\\d+"))
                    str = StringHelper.padLeft(str, 4, '0');
                else str = StringHelper.padLeft(str, 5, '0');
                bookId = matcher.group(1).concat(str);
                //
                str = matcher.group(4);
                if (str.matches(".*[a-z]\\d$"))
                    str = str.substring(0, str.length() - 1).concat("0").concat(str.substring(str.length() - 1));
                str = StringHelper.padLeft(str, 7, '0');
                chapter = "p".concat(str);
            } else if ((matcher = Pattern.compile("([A-Z]+)(\\d+)n(\\d+)[_.#](\\d+)").matcher(searchTerm)).matches()) {
                bookId = matcher.group(1).concat(matcher.group(3));
                chapter = matcher.group(4);
                lineOrVolume = false;
            } else if ((matcher = Pattern.compile("([A-Z]+)(\\d+)[_.#](\\d+)").matcher(searchTerm)).matches()) {
                bookId = matcher.group(1).concat(matcher.group(2));
                chapter = matcher.group(3);
                lineOrVolume = false;
            }
            if (null != bookId) {
                Book book = BooksProfile.ONE.getBook(bookId);
                String title = null == book ? "???" : "《".concat(book.title).concat("》");
                if (!lineOrVolume) {
                    chapter = chapter.length() >= 3 ? chapter.substring(0, 3) : StringHelper.padLeft(chapter, 3, '0');
                }
                title = StringHelper.concat("转到 >>> 经号：", bookId, "，经名：", title,
                        (lineOrVolume ? "，行号：" : "，卷号：").concat(chapter));
                result.add(new LookupDatabase.LookupData(lineOrVolume, bookId, -1, title, chapter, null, null, "#"));
            } else {
                result.add("??? 使用说明：请使用以下几种格式");
                result.add("格式1：#T01n0001_p0001a01  表示：转到经号T0001的行号p0001a01处");
                result.add("格式2：#T0001_p0001a01  表示：转到经号T0001的行号p0001a01处");
                result.add("格式3：#T01, no. 1, p. 1a1  表示：转到经号T0001的行号p0001a01处");
                result.add("格式4：#T01n0001_1  表示：转到经号T0001的第001卷");
                result.add("格式5：#T01n0001_001  表示：转到经号T0001的第001卷");
                result.add("格式6：#T0001_1  表示：转到经号T0001的第001卷");
                result.add("格式7：#T0001_001  表示：转到经号T0001的第001卷");
            }
        }

        @Override
        protected void handleEnterOrDoubleClickActionOnSearchResultList(InputEvent event, Object data) {
            if (!(data instanceof LookupDatabase.LookupData item)) return;
            if (null == item.bookId) return;
            Book book = BooksProfile.ONE.getBook(item.bookId);
            if (null == book) return;

            Chapter chapter = null;
            if (Objects.equals(item.extra, "#")) {
                chapter = book.ofChapter();
                chapter.id = "#";
                chapter.path = item.chapter;
            } else if (null != item.chapter) {
                // open as chapter
                String[] tmpArr = item.chapter.split("#", 2);
                chapter = book.ofChapter();
                chapter.path = tmpArr[0];
                if (tmpArr.length == 2) {
                    chapter.anchor = "#".concat(tmpArr[1]);
                    chapter.attr("position.selector", chapter.anchor);
                }
            }
            hide();
            app.eventBus.fireEvent(new BookEvent(BookEvent.OPEN, book, chapter));
        }
    }
}
