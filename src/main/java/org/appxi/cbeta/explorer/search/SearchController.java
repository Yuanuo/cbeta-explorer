package org.appxi.cbeta.explorer.search;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.SearchedEvent;
import org.appxi.cbeta.explorer.event.SearcherEvent;
import org.appxi.cbeta.explorer.event.StatusEvent;
import org.appxi.cbeta.explorer.model.BookList;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideToolController;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.model.Chapter;
import org.appxi.util.DigestHelper;
import org.controlsfx.glyphfont.MaterialIcon;
import org.controlsfx.glyphfont.MaterialIconView;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class SearchController extends WorkbenchSideToolController {

    public SearchController(WorkbenchApplication application) {
        super("SEARCH", application);
        this.setTitles("搜索", "全文检索 (Ctrl+H)");
        this.attr(Pos.class, Pos.CENTER_LEFT);
        this.viewIcon.set(new MaterialIconView(MaterialIcon.SEARCH));
    }

    @Override
    public void setupInitialize() {
        // 响应快捷键 Ctrl+H 事件，以打开搜索视图
        getPrimaryScene().getAccelerators().put(new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN),
                () -> openSearcherWithText(null, null));
        // 响应SEARCH Event事件，以打开搜索视图
        getEventBus().addEventHandler(SearcherEvent.SEARCH, event -> openSearcherWithText(event.text, event.scope));
        // 尝试建立/重建全文索引
        getEventBus().addEventHandler(StatusEvent.BEANS_READY, event -> {
            if (IndexingHelper.indexedIsValid()) return;
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, """
                        检测到数据变化或基于新的功能特性，
                        全文检索功能需要更新索引数据，否则无法正常使用全文检索功能！
                        （可以不更新索引只使用阅读功能！）
                                            
                        是否继续更新索引？
                        """);
                FxHelper.withTheme(getApplication(), alert).showAndWait().filter(v -> v == ButtonType.OK).ifPresent(v -> {
                    CompletableFuture.runAsync(new IndexingTask(application)).whenComplete((o, err) -> {
                        if (null != err) FxHelper.alertError(getApplication(), err);
                    });
                });
            });
        });
        // 响应搜索结果打开事件
        getEventBus().addEventHandler(SearchedEvent.OPEN, event -> {
            if (null == event.piece)
                return;
            CbetaBook book = BookList.getById(event.piece.fields.get("book_s"));
            Chapter chapter = null;
            if (null != event.piece.path) {
                // open as chapter
                chapter = new Chapter();
                chapter.path = event.piece.path;
                chapter.start = event.piece.fields.get("anchor_s");
                if (null != chapter.start)
                    chapter.attr("position.selector", chapter.start);
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

    private void openSearcherWithText(String text, CbetaBook scope) {
        // 优先查找可用的搜索视图，以避免打开太多未使用的搜索视图
        SearcherController searcher = findReusableSearcher(
                () -> new SearcherController("SEARCHER-".concat(DigestHelper.uid()), getApplication())
        );
        FxHelper.runLater(() -> {
            if (!getPrimaryViewport().existsMainView(searcher.viewId.get())) {
                getPrimaryViewport().addWorkbenchViewAsMainView(searcher, false);
                searcher.setupInitialize();
            }
            getPrimaryViewport().selectMainView(searcher.viewId.get());
            searcher.setSearchScope(scope);
            searcher.search(text);
        });
    }

    private SearcherController findReusableSearcher(Supplier<SearcherController> supplier) {
        for (Tab tab : getPrimaryViewport().getMainViewsTabs()) {
            if (tab.getUserData() instanceof SearcherController view && view.isNeverSearched())
                return view;
        }
        return supplier.get();
    }
}
