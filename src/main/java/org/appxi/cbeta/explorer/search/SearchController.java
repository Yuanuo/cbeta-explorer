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
import org.appxi.cbeta.explorer.event.SearchEvent;
import org.appxi.cbeta.explorer.event.StatusEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideToolController;
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
        getEventBus().addEventHandler(SearchEvent.SEARCH, event -> openSearcherWithText(event.text));
        getEventBus().addEventHandler(StatusEvent.BEANS_READY,
                event -> CompletableFuture.runAsync(new IndexingTask(application)).whenComplete((o, err) -> {
                    if (null != err) FxHelper.alertError(getApplication(), err);
                })
        );
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
