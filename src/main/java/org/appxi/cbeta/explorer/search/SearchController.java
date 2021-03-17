package org.appxi.cbeta.explorer.search;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.geometry.Pos;
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
                () -> openSearcherWithText(null));
        // 响应SEARCH Event事件，以打开搜索视图
        getEventBus().addEventHandler(SearcherEvent.SEARCH, event -> openSearcherWithText(event.text));
        // 尝试建立/重建全文索引
        getEventBus().addEventHandler(StatusEvent.BEANS_READY,
                event -> CompletableFuture.runAsync(new IndexingTask(application)).whenComplete((o, err) -> {
                    if (null != err) FxHelper.alertError(getApplication(), err);
                })
        );
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
        openSearcherWithText(null);
    }

    private void openSearcherWithText(String text) {
        // 优先查找可用的搜索视图，以避免打开太多未使用的搜索视图
        SearcherController searcher = findReusableSearcher(
                () -> new SearcherController("SEARCHER-".concat(DigestHelper.uid()), getApplication())
        );
        FxHelper.runLater(() -> {
            if (!getPrimaryViewport().existsMainView(searcher.viewId)) {
                getPrimaryViewport().addWorkbenchViewAsMainView(searcher, false);
                searcher.setupInitialize();
            }
            getPrimaryViewport().selectMainView(searcher.viewId);
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
