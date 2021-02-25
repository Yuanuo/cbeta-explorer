package org.appxi.cbeta.explorer.search;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
        super("SEARCH", "搜索", application);
    }

    @Override
    public Node createToolIconGraphic(Boolean placeInSideViews) {
        this.attr(Pos.class, Pos.CENTER_LEFT);
        return new MaterialIconView(MaterialIcon.SEARCH);
    }

    @Override
    public void setupInitialize() {
        getPrimaryScene().getAccelerators().put(new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN),
                () -> openSearcherWithText(null));
        getEventBus().addEventHandler(SearcherEvent.SEARCH, event -> openSearcherWithText(event.text));
        getEventBus().addEventHandler(StatusEvent.BEANS_READY,
                event -> CompletableFuture.runAsync(new IndexingTask(application)).whenComplete((o, err) -> {
                    if (null != err) FxHelper.alertError(getApplication(), err);
                })
        );
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
                if (null != event.highlightSnippet) {
                    chapter.attr("position.term", event.highlightTerm.replace("…", ""));
                    chapter.attr("position.text", event.highlightSnippet
                            .replace("§§hl#end§§", "").replace("…", ""));
                }
            }
            getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, book, chapter));
        });
    }

    @Override
    public void onViewportShow(boolean firstTime) {
        openSearcherWithText(null);
    }

    private void openSearcherWithText(String text) {
        SearcherController searcher = findReusableSearcher(
                () -> new SearcherController("SEARCHER-".concat(DigestHelper.uid()), getApplication())
        );
        Platform.runLater(() -> {
            if (!getPrimaryViewport().existsMainView(searcher.viewId)) {
                getPrimaryViewport().addWorkbenchViewAsMainView(searcher, false);
                searcher.setupInitialize();
            }
            getPrimaryViewport().selectMainView(searcher.viewId);
            searcher.search(text);
        });
    }

    SearcherController findReusableSearcher(Supplier<SearcherController> supplier) {
        for (Tab tab : getPrimaryViewport().getMainViewsTabs()) {
            if (tab.getUserData() instanceof SearcherController view && view.isNeverSearched())
                return view;
        }
        return supplier.get();
    }
}
