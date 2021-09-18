package org.appxi.cbeta.explorer.search;

import org.appxi.javafx.glyphfont.MaterialIcon;
import org.appxi.javafx.glyphfont.MaterialIconView;
import javafx.geometry.Pos;
import javafx.scene.control.Labeled;
import javafx.scene.input.*;
import javafx.stage.Screen;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.DisplayHelper;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.SearcherEvent;
import org.appxi.cbeta.explorer.event.StatusEvent;
import org.appxi.cbeta.explorer.model.BookList;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideToolController;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.model.Chapter;
import org.appxi.util.StringHelper;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LookupController extends WorkbenchSideToolController {
    private LookupProvider lookupProvider;

    public LookupController(WorkbenchApplication application) {
        super("LOOKUP", application);
        this.setTitles("检索", "快捷检索 (Ctrl+G)");
        this.attr(Pos.class, Pos.CENTER_LEFT);
        this.viewIcon.set(new MaterialIconView(MaterialIcon.NEAR_ME));
    }

    private long lastShiftKeyPressedTime;

    @Override
    public void setupInitialize() {
        getPrimaryScene().getAccelerators().put(new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN),
                () -> this.onViewportShowing(false));
        getPrimaryScene().addEventHandler(KeyEvent.KEY_PRESSED, evt -> {
            if (evt.getCode() == KeyCode.SHIFT) {
                final long currShiftKeyPressedTime = System.currentTimeMillis();
                if (currShiftKeyPressedTime - lastShiftKeyPressedTime <= 400) {
                    this.onViewportShowing(false);
                } else lastShiftKeyPressedTime = currShiftKeyPressedTime;
            }
        });
        getEventBus().addEventHandler(SearcherEvent.LOOKUP,
                event -> this.onViewportShowing(null != event.text ? event.text.strip() : null));
        this.lookupProvider = new LookupInMemory();
        getEventBus().addEventHandler(StatusEvent.BOOKS_READY, event -> ((LookupInMemory) lookupProvider).setupInitialize());
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        onViewportShowing(null);
    }

    private LookupViewImpl lookupView;

    private void onViewportShowing(String text) {
        if (null == lookupView) {
            lookupView = new LookupViewImpl();
        }
        lookupView.show();
        if (null != text)
            lookupView.search(text);
    }

    private class LookupViewImpl extends LookupViewExt<LookupItem> {
        private final String specialMarker = "#";

        public LookupViewImpl() {
            super(application.getPrimaryViewport());
        }

        @Override
        protected int getPrefWidth() {
            double maxWidth = getPrimaryStage().getWidth();// Screen.getPrimary().getBounds().getWidth();
            if (maxWidth >= 1920)
                return 1440;
            if (maxWidth >= 1440)
                return 1280;
            return 1080;
        }

        @Override
        protected int getPrefHeight() {
            double maxHeight = getPrimaryStage().getHeight();// Screen.getPrimary().getBounds().getHeight();
            if (maxHeight >= 1080)
                return 800;
            if (maxHeight >= 900)
                return 720;
            return 640;
        }

        @Override
        protected String getHeaderText() {
            return "快捷检索";
        }

        @Override
        protected String getUsagesText() {
            return """
                    >> 不分简繁任意汉字匹配（以感叹号起始则强制区分）；逗号分隔任意字/词/短语匹配；书名/书号ID/作者/译者/时域任意匹配；#号起始开启经卷行定位；
                    >> 快捷键：双击Shift 或 Ctrl+G 开启；ESC 或 点击透明区 退出此界面；上/下方向键选择列表项；回车键打开；
                    """;
        }

        @Override
        protected void updateItemOnce(Labeled labeled, LookupItem item) {
            labeled.setText(DisplayHelper.displayText(item.toString()));
            //
            labeled.getStyleClass().remove("visited");
            if (null != item.bookId() && null != AppContext.recentBooks.getProperty(item.bookId())) {
                labeled.getStyleClass().add("visited");
            }
        }

        @Override
        protected Collection<LookupItem> search(String searchText, String[] searchWords, int resultLimit) {
            return lookupProvider.search(searchText, searchWords, resultLimit);
        }

        @Override
        protected void convertSearchTermToCommands(String searchTerm, Collection<LookupItem> result) {
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
                CbetaBook book = BookList.getById(bookId);
                String title = null == book ? "???" : "《".concat(book.title).concat("》");
                if (!lineOrVolume) {
                    chapter = chapter.length() >= 3 ? chapter.substring(0, 3) : StringHelper.padLeft(chapter, 3, '0');
                }
                title = StringHelper.concat("转到 >>> 经号：", bookId, "，经名：", title,
                        (lineOrVolume ? "，行号：" : "，卷号：").concat(chapter));
                result.add(new LookupItem(lineOrVolume, bookId, -1, title, chapter, null, null, specialMarker));
            } else {
                result.add(new LookupItem(false, null, -1, "??? 使用说明：请使用以下几种格式", null, null, null, specialMarker));
                result.add(new LookupItem(false, null, -1, "格式1：#T01n0001_p0001a01  表示：转到经号T0001的行号p0001a01处", null, null, null, specialMarker));
                result.add(new LookupItem(false, null, -1, "格式2：#T0001_p0001a01  表示：转到经号T0001的行号p0001a01处", null, null, null, specialMarker));
                result.add(new LookupItem(false, null, -1, "格式3：#T01, no. 1, p. 1a1  表示：转到经号T0001的行号p0001a01处", null, null, null, specialMarker));
                result.add(new LookupItem(false, null, -1, "格式4：#T01n0001_1  表示：转到经号T0001的第001卷", null, null, null, specialMarker));
                result.add(new LookupItem(false, null, -1, "格式5：#T01n0001_001  表示：转到经号T0001的第001卷", null, null, null, specialMarker));
                result.add(new LookupItem(false, null, -1, "格式6：#T0001_1  表示：转到经号T0001的第001卷", null, null, null, specialMarker));
                result.add(new LookupItem(false, null, -1, "格式7：#T0001_001  表示：转到经号T0001的第001卷", null, null, null, specialMarker));
            }
        }

        @Override
        protected void handleEnterOrDoubleClickActionOnSearchResultList(InputEvent event, LookupItem item) {
            if (null == item.bookId()) {
                return;
            }
            CbetaBook book = BookList.getById(item.bookId());
            if (null == book)
                return;

            Chapter chapter = null;
            if (item.extra() == specialMarker) {
                chapter = new Chapter();
                chapter.id = "#";
                chapter.path = item.chapter();
            } else if (null != item.chapter()) {
                // open as chapter
                String[] tmpArr = item.chapter().split("#", 2);
                chapter = new Chapter();
                chapter.path = tmpArr[0];
                if (tmpArr.length == 2) {
                    chapter.start = "#".concat(tmpArr[1]);
                    chapter.attr("position.selector", chapter.start);
                }
            }
            hide();
            getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, book, chapter));
        }
    }
}
