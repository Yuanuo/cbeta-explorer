package org.appxi.cbeta.explorer.search;

import javafx.scene.input.*;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.SearcherEvent;
import org.appxi.cbeta.explorer.event.StatusEvent;
import org.appxi.cbeta.explorer.model.BookList;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchNoneViewController;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.model.Chapter;

import java.util.Collection;

public class LookupController extends WorkbenchNoneViewController {
    private LookupProvider lookupProvider;

    public LookupController(WorkbenchApplication application) {
        super("LOOKUP", "快速查找", application);
    }

    @Override
    public void setupInitialize() {
        getPrimaryScene().getAccelerators().put(new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN),
                () -> this.onViewportShow(false));
        getPrimaryScene().addEventHandler(KeyEvent.KEY_PRESSED, evt -> {
            getLookupView().handleEventToShow(evt);
        });
        getEventBus().addEventHandler(SearcherEvent.LOOKUP, event -> {
            this.onViewportShow(false);
            if (null != event.text)
                getLookupView().lookup(event.text.strip());
        });
        this.lookupProvider = new LookupInMemory();
        getEventBus().addEventHandler(StatusEvent.BOOKS_READY, event -> ((LookupInMemory) lookupProvider).setupInitialize());
    }

    private LookupView<LookupItem> lookupView;

    public LookupView<LookupItem> getLookupView() {
        if (null == lookupView) {
            lookupView = new LookupView<>(getPrimaryViewport()) {
                @Override
                protected int getResultLimit() {
                    return 100;
                }

                private long lastShiftKeyPressedTime;

                @Override
                protected boolean isKeyEventHandled(KeyEvent event) {
                    boolean handled = false;
                    if (event.getCode() == KeyCode.SHIFT) {
                        final long currShiftKeyPressedTime = System.currentTimeMillis();
                        handled = currShiftKeyPressedTime - lastShiftKeyPressedTime <= 400;
                        lastShiftKeyPressedTime = currShiftKeyPressedTime;
                    }
                    return handled;
                }

                @Override
                protected String getSearchTitle() {
                    return "快速查找书籍（快捷键：双击Shift 或 Ctrl+G 开启。ESC 或 点击透明区 退出此界面。上下方向键选择列表项）";
                }

                @Override
                protected String getSearchHelp() {
                    return ">> 不分简繁任意汉字匹配（以感叹号开始强制区分）；逗号分隔任意字/词/短语匹配；书名/书号ID/作者/译者/时域任意匹配；";
                }

                @Override
                protected String convertItemToString(LookupItem item) {
                    return item.toString();
                }

                @Override
                protected void handleEnterOrDoubleClickActionOnSearchResultList(InputEvent event, LookupItem item) {
                    super.handleEnterOrDoubleClickActionOnSearchResultList(event, item);
                    CbetaBook book = BookList.getById(item.bookId());
                    Chapter chapter = null;
                    if (null != item.chapter()) {
                        // open as chapter
                        String[] tmpArr = item.chapter().split("#", 2);
                        chapter = new Chapter();
                        chapter.path = tmpArr[0];
                        if (tmpArr.length == 2) {
                            chapter.start = "#".concat(tmpArr[1]);
                            chapter.attr("position.selector", chapter.start);
                        }
                    }
                    getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, book, chapter));
                }

                @Override
                protected Collection<LookupItem> search(String searchedText, String[] searchWords, int resultLimit) {
                    return lookupProvider.search(searchedText, searchWords, resultLimit);
                }
            };
        }
        return lookupView;
    }

    @Override
    public void onViewportShow(boolean firstTime) {
        getLookupView().show();
    }
}
