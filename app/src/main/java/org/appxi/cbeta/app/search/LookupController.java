package org.appxi.cbeta.app.search;

import javafx.event.ActionEvent;
import javafx.scene.control.Labeled;
import javafx.scene.control.TreeItem;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.appxi.book.Chapter;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.ChapterTree;
import org.appxi.cbeta.app.DataApp;
import org.appxi.cbeta.app.event.BookEvent;
import org.appxi.cbeta.app.event.GenericEvent;
import org.appxi.javafx.app.search.SearcherEvent;
import org.appxi.javafx.control.LookupLayer;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPart;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.HanLang;
import org.appxi.util.ext.Node;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LookupController extends WorkbenchPartController implements WorkbenchPart.SideTool {
    public final DataApp dataApp;

    public LookupController(WorkbenchPane workbench, DataApp dataApp) {
        super(workbench);
        this.dataApp = dataApp;

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
        app.eventBus.addEventHandler(GenericEvent.PROFILE_READY, event -> {
            if (null != lookupLayer) lookupLayer.reset();
            TreeHelper.walkTree(dataApp.dataContext.booklist().tree(), new TreeHelper.TreeWalker<>() {
                @Override
                public void visit(TreeItem<Book> treeItem, Book book) {
                    if (null == book || book.id == null || book.path == null || !book.path.startsWith("toc/")) {
                        return;
                    }
//                        BookHelper.walkTocChaptersByXmlSAX(AppContext.bookcase(), book, (href, text) -> {
//                            System.out.println(text);
//                        });
                    // load chapters
                    new ChapterTree(dataApp.dataContext.bookcase, book).getTocChapters();
                }
            });
        });
        // 当显示汉字类型改变时需要同步更新lookupView
        app.eventBus.addEventHandler(HanLang.Event.CHANGED,
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
            super(dataApp, app.getPrimaryGlass());
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
        protected void helpButtonAction(ActionEvent actionEvent) {
            FxHelper.showTextViewerWindow(app, "appFindByNames.helpWindow", "快捷检索使用方法",
                    """
                            >> 支持简繁汉字匹配（以感叹号起始则强制区分）；支持按拼音（全拼）匹配，使用双引号包含则实行精确匹配；
                            >> 支持复杂条件(+/AND,默认为OR)：例1：1juan +"bu kong" AND 仪轨；例2：1juan +("bu kong" 仪轨 OR "yi gui")
                            >> 空格分隔任意字/词/短语匹配；书名/书号ID/作者/译者/时域/卷数任意匹配；#号起始开启经卷行定位；
                            >> 快捷键：双击Shift 或 Ctrl+G 开启；ESC 或 点击透明区 退出此界面；上/下方向键选择列表项；回车键打开；
                                    """);
        }

        @Override
        protected void updateItemLabel(Labeled labeled, Object data) {
            labeled.getStyleClass().remove("visited");
            if (data instanceof Book book) {
                if (null != book.id && null != dataApp.recentBooks.getProperty(book.id)) {
                    labeled.getStyleClass().add("visited");
                }
                labeled.setText(toDisplayableLabel(book, null));
                super.updateItemLabel(labeled, data);
            } else if (data instanceof Chapter chapter) {
                String bookId = chapter.attrStr(AK_BOOK_ID);
                Book book = dataApp.dataContext.getBook(bookId);
                if (null != book && null != book.id && null != dataApp.recentBooks.getProperty(book.id)) {
                    labeled.getStyleClass().add("visited");
                }
                labeled.setText(toDisplayableLabel(book, chapter));
                super.updateItemLabel(labeled, data);
            } else if (data instanceof LookupData item && !Objects.equals(item.extra, "#")) {
                if (null != item.bookId && null != dataApp.recentBooks.getProperty(item.bookId)) {
                    labeled.getStyleClass().add("visited");
                }
                labeled.setText(item.toString());
                super.updateItemLabel(labeled, data);
            } else if (data instanceof String str) {
                labeled.setText(str);
                super.updateItemLabel(labeled, str);
            } else {
                String str = null == data ? "" : data.toString();
                labeled.setText(str);
                super.updateItemLabel(labeled, str);
            }
        }

        String toDisplayableLabel(Book book, Chapter chapter) {
            final StringBuilder buff = new StringBuilder();
            if (null != book) {
                if (null != book.id) {
                    buff.append(" / ").append(book.id);
                }
                if (null != book.title) {
                    buff.append(" / ").append(book.title);
                }
                if (!book.volumes.isEmpty()) {
                    buff.append("（").append(book.volumes.size()).append("卷）");
                }
            }
            if (null != chapter) {
                if (null != chapter.title) {
                    buff.append(" / ").append(chapter.title);
                }
            }
            if (null != book) {
                if (StringHelper.isNotBlank(book.authorInfo)) {
                    buff.append(" / ").append(book.authorInfo);
                }
            }
            final String str = buff.toString().toLowerCase();
            return str.length() > 3 ? str.substring(3) : str;
        }

        @Override
        protected void lookupByKeywords(String lookupText, int resultLimit,
                                        List<LookupResultItem> result, Set<String> usedKeywords) {
//                    LookupByPredicate.lookup(lookupText, resultLimit, result, usedKeywords);
//            LookupByExpression.lookup(lookupDatabase, lookupText, resultLimit, result, usedKeywords);
            final boolean lookupTextIsBlank = lookupText.isBlank();
            final String[] arr = lookupText.toLowerCase().split("[ 　]+");
            final LookupWord[] words = Arrays.stream(arr).map(LookupWord::new).toArray(LookupWord[]::new);
            if (!lookupTextIsBlank) {
                usedKeywords.addAll(Arrays.asList(arr));
            }

            try {
                TreeHelper.walkTree(dataApp.dataContext.booklist().tree(), new TreeHelper.TreeWalker<Book>() {
                    @Override
                    public void start(TreeItem<Book> treeItem, Book book) {
                        if (null == book || book.id == null) {
                            return;
                        }
                        test(book, StringHelper.concat(book.id, book.title, book.authorInfo));
                    }

                    @Override
                    public void visit(TreeItem<Book> treeItem, Book book) {
                        if (null == book || book.id == null) {
                            return;
                        }
                        // book
                        test(book, StringHelper.concat(book.id, book.title, book.authorInfo));

                        // chapters
                        if (book.path != null && book.path.startsWith("toc/")) {
                            Node<Chapter> chapters = book.chapters.findFirst(node -> "tocs".equals(node.value.id));
                            if (null != chapters) {
                                chapters.children.forEach(child -> {
                                    child.walk(new Node.Walker<>() {
                                        @Override
                                        public void head(int depth, Node<Chapter> node, Chapter nodeVal) {
                                            try {
                                                test(nodeVal, nodeVal.title);
                                                nodeVal.attr(AK_BOOK_ID, book.id);
                                            } catch (ResultLimitException rle) {
                                                nodeVal.attr(AK_BOOK_ID, book.id);
                                                throw rle;
                                            }
                                        }
                                    });
                                });
                            }
                        }
                    }

                    void test(Object data, String content) {
//                        System.out.println("test : " + content);
                        if (lookupTextIsBlank) {
                            result.add(new LookupResultItem(data, 1));
                        } else {
                            // reset
                            for (LookupWord lookupWord : words) {
                                lookupWord.score = 0;
                            }
                            //
                            content = content.toLowerCase();
                            int kIdx = 0;
                            int cLen = content.length();
                            for (int i = 0; i < cLen; i++) {
                                for (; kIdx < words.length; kIdx++) {
                                    final LookupWord lookupWord = words[kIdx];
                                    final int idx = content.indexOf(lookupWord.word, i);
                                    if (idx >= i) {
                                        lookupWord.score += 10;
                                        i = idx;
                                    } else {
                                        lookupWord.score = -9999;
                                        i = cLen;
                                        break;
                                    }
                                }
                            }
                            //
                            int totalScore = 0;
                            for (LookupWord lookupWord : words) {
                                totalScore += lookupWord.score;
                            }
                            //
                            if (totalScore > 0) {
                                result.add(new LookupResultItem(data, totalScore));
                            }
                        }
                        //
                        if (result.size() >= resultLimit) {
                            throw new ResultLimitException();
                        }
                    }
                });
            } catch (ResultLimitException ignore) {
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                Book book = dataApp.dataContext.getBook(bookId);
                String title = null == book ? "???" : "《".concat(book.title).concat("》");
                if (!lineOrVolume) {
                    chapter = chapter.length() >= 3 ? chapter.substring(0, 3) : StringHelper.padLeft(chapter, 3, '0');
                }
                title = StringHelper.concat("转到 >>> 经号：", bookId, "，经名：", title,
                        (lineOrVolume ? "，行号：" : "，卷号：").concat(chapter));
                result.add(new LookupData(lineOrVolume, bookId, -1, title, chapter, null, null, "#"));
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
            if (data instanceof Book book) {
                hide();
                app.eventBus.fireEvent(new BookEvent(BookEvent.OPEN, book, null));
            } else if (data instanceof Chapter chapter) {
                String bookId = chapter.attrStr(AK_BOOK_ID);
                Book book = dataApp.dataContext.getBook(bookId);
                hide();
                app.eventBus.fireEvent(new BookEvent(BookEvent.OPEN, book, chapter));
            } else if (data instanceof LookupData item) {
                if (null == item.bookId) return;
                Book book = dataApp.dataContext.getBook(item.bookId);
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
            } else {
            }
        }
    }

    static final Object AK_BOOK_ID = new Object();

    static class LookupWord {
        final String word;
        int score = 0;

        LookupWord(String word) {
            this.word = word;
        }
    }

    static final class LookupData {
        public final boolean stdBook;
        public final String bookId;
        public final int bookVols;
        public final String bookTitle;
        public final String chapter;
        public final String chapterTitle;
        public final String authorInfo;
        public final String extra;
        public final String bookVolsLabel;

        LookupData(boolean stdBook, String bookId, int bookVols, String bookTitle,
                   String chapter, String chapterTitle,
                   String authorInfo, String extra) {
            this.stdBook = stdBook;
            this.bookId = bookId;
            this.bookVols = bookVols;
            this.bookTitle = bookTitle;
            this.chapter = chapter;
            this.chapterTitle = chapterTitle;
            this.authorInfo = authorInfo;
            this.extra = extra;
            this.bookVolsLabel = String.valueOf(bookVols).concat("卷");
        }
    }

    static final class ResultLimitException extends RuntimeException {
    }
}
