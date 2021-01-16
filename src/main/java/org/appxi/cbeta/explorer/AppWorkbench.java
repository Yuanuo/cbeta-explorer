package org.appxi.cbeta.explorer;

import javafx.application.Platform;
import javafx.scene.web.WebView;
import org.appxi.cbeta.explorer.event.DataEvent;
import org.appxi.cbeta.explorer.search.SearchHelper;
import org.appxi.cbeta.explorer.workbench.WorkbenchRootController;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.theme.Theme;
import org.appxi.javafx.theme.ThemeSet;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchController;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AppWorkbench extends WorkbenchApplication {
    private WorkbenchRootController rootController;

    @Override
    public void init() throws Exception {
        super.init();
        initThemes();
        eventBus.addEventHandler(ApplicationEvent.STARTED, event -> Platform.runLater(WebView::new));
    }

    private void initThemes() {
        final Theme webLight = Theme.light("web-light", "亮", "#e9e9eb")
                .addStylesheet(getClass().getResource("/appxi/cbetaexplorer/themes/style-light.css"));
        final Theme webLight2 = Theme.light("web-light-2", "明亮", "#328291")
                .addStylesheet(getClass().getResource("/appxi/cbetaexplorer/themes/style-light-2.css"));
        final Theme webDark = Theme.dark("web-dark", "暗", "#3b3b3b")
                .addStylesheet(getClass().getResource("/appxi/cbetaexplorer/themes/style-dark.css"));

        themeProvider.addTheme(ThemeSet.light("light", "亮", "#e9e9eb")
                .addStylesheet(getClass().getResource("/appxi/cbetaexplorer/themes/theme-light.css"))
                .addTheme(webLight, webLight2));

        themeProvider.addTheme(ThemeSet.light("light-2", "明亮", "#328291")
                .addStylesheet(getClass().getResource("/appxi/cbetaexplorer/themes/theme-light-2.css"))
                .addTheme(webLight2, webLight));

        themeProvider.addTheme(ThemeSet.dark("dark", "暗", "#3b3b3b")
                .addStylesheet(getClass().getResource("/appxi/cbetaexplorer/themes/theme-dark.css"))
                .addTheme(webDark));
    }

    @Override
    protected void started() {
        super.started();
        CompletableFuture.runAsync(() -> {
            CbetaxHelper.books.getDataMap();
            SearchHelper.searchById = id -> CbetaxHelper.books.getDataMap().get(id);
        }).whenComplete((o, err) -> {
            eventBus.fireEvent(new DataEvent(DataEvent.BOOKS_READY));
            SearchHelper.setupSearchService(this.rootController);
        });
    }

    @Override
    protected String getApplicationTitle() {
        return "智悲乐藏";
    }

    @Override
    protected List<URL> getApplicationIcons() {
        final int[] iconSizes = new int[]{24, 32, 48, 64, 72, 96, 128};
        final List<URL> result = new ArrayList<>(iconSizes.length);
        for (int iconSize : iconSizes) {
            result.add(getClass().getResource("/appxi/cbetaexplorer/icons/icon-" + iconSize + ".png"));
        }
        return result;
    }

    @Override
    protected WorkbenchController createPrimarySceneRootController() {
        return this.rootController = new WorkbenchRootController();
    }
}
