package org.appxi.cbeta.explorer.search;

import appxi.cbeta.Book;
import appxi.cbeta.Chapter;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.event.*;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.iconfont.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideToolController;
import org.appxi.util.DigestHelper;

import java.util.Objects;

public class SearchController extends WorkbenchSideToolController {
    private ProgressEvent indexingEvent;

    public SearchController(WorkbenchApplication application) {
        super("SEARCH", application);
        this.setTitles("搜索", "全文检索 (Ctrl+H)");
        this.attr(Pos.class, Pos.CENTER_LEFT);
        this.viewIcon.set(MaterialIcon.SEARCH.iconView());
    }

    @Override
    public void setupInitialize() {
        // 响应快捷键 Ctrl+H 事件，以打开搜索视图
        getPrimaryScene().getAccelerators().put(new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN),
                () -> openSearcherWithText(null, null));
        // 响应SEARCH Event事件，以打开搜索视图
        getEventBus().addEventHandler(SearcherEvent.SEARCH, event -> openSearcherWithText(event.text, event.scope));
        getEventBus().addEventHandler(ProgressEvent.INDEXING, event -> indexingEvent = event.isFinished() ? null : event);
        getEventBus().addEventHandler(GenericEvent.PROFILE_READY, event -> {
            this.attr(event.getEventType(), true);

            if (IndexedManager.isBookcaseIndexable() || IndexedManager.isBooklistIndexable()) {
                alertIndexable(null);
            } else {
                // 如果全文索引数据正常（不需重建索引），则此时尝试加载，否则仅在后续重建索引时初始化
                new Thread(AppContext::beans).start();
            }
        });
        // 响应搜索结果打开事件
        getEventBus().addEventHandler(SearchedEvent.OPEN, event -> {
            if (null == event.piece)
                return;
            Book book = AppContext.booklistProfile.getBook(event.piece.field("book_s"));
            Chapter chapter = null;
            String file = event.piece.field("file_s");
            if (null != file) {
                // open as chapter
                chapter = new Chapter();
                chapter.path = file;
                chapter.anchor = event.piece.field("anchor_s");
                if (null != chapter.anchor)
                    chapter.attr("position.selector", chapter.anchor);
                if (null != event.highlightTerm)
                    chapter.attr("position.term", event.highlightTerm.replace("…", ""));
                if (null != event.highlightSnippet)
                    chapter.attr("position.text", event.highlightSnippet
                            .replace("§§hl#end§§", "").replace("…", ""));
            }
            getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, book, chapter));
        });
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        openSearcherWithText(null, null);
    }

    private void openSearcherWithText(String text, Book scope) {
        // 优先查找可用的搜索视图，以避免打开太多未使用的搜索视图
        SearcherController searcher = getPrimaryViewport().getMainViewsTabs().stream()
                .map(tab -> (tab.getUserData() instanceof SearcherController view && view.isNeverSearched()) ? view : null)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> new SearcherController("SEARCHER-".concat(DigestHelper.uid()), getApplication()));
        FxHelper.runLater(() -> {
            if (!getPrimaryViewport().existsMainView(searcher.viewId.get())) {
                getPrimaryViewport().addWorkbenchViewAsMainView(searcher, false);
                searcher.setupInitialize();
            }
            getPrimaryViewport().selectMainView(searcher.viewId.get());
            searcher.setSearchScope(scope);
            if (null != indexingEvent)
                searcher.handleEventOnIndexingToBlocking.handle(indexingEvent);
            else if (IndexedManager.isBookcaseIndexable() || IndexedManager.isBooklistIndexable())
                Platform.runLater(() -> alertIndexable(searcher));
            else searcher.search(text);
        });
    }

    private void alertIndexable(SearcherController controller) {
        if (!this.hasAttr(GenericEvent.PROFILE_READY)) return;
        FxHelper.runLater(() -> {
            String contentText = null, headerText = null;
            if (IndexedManager.isBookcaseIndexable()) {
                headerText = "全文检索功能需要“重建全局数据”";
                contentText = """
                        检测到“全局数据”有变化或基于新的功能特性，
                        全文检索功能需要“重建”索引数据，否则无法正常使用全文检索功能！
                                            
                        “重建”索引需要耗时 大约20分钟 左右。有3种建议的操作：
                        1、暂时取消；建立适合自已的书单，切换到自己的书单一并建立索引。
                        2、立即建立索引。
                        3、“不重建”索引仅使用阅读和快捷检索等功能。

                        是否继续“重建”索引？
                        """;
            } else if (IndexedManager.isBooklistIndexable()) {
                headerText = "全文检索功能需要“更新书单数据”";
                contentText = """
                        检测到“书单数据”有变化或基于新的功能特性，
                        全文检索功能需要“更新”索引数据，否则无法正常使用全文检索功能！
                                                
                        “更新”索引仅需要耗时 大约几分钟 左右，也可以“不更新”索引仅使用阅读和快捷检索等功能！

                        是否继续“更新”索引？
                        """;
            }
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, contentText);
            alert.setTitle("数据更新！");
            alert.setHeaderText(headerText);
            alert.setWidth(640);
            FxHelper.withTheme(getApplication(), alert).showAndWait().filter(v -> v == ButtonType.OK)
                    .ifPresentOrElse(v -> {
                        indexingEvent = new ProgressEvent(ProgressEvent.INDEXING, -1, 1, "处理中。。。");
                        if (null != controller) controller.handleEventOnIndexingToBlocking.handle(indexingEvent);
                        new Thread(new IndexingTask(application)).start();
                    }, () -> {
                        if (null != controller) getPrimaryViewport().removeMainView(controller);
                    });
        });
    }
}
