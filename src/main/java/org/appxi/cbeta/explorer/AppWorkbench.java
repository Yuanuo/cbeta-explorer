package org.appxi.cbeta.explorer;

import javafx.application.Platform;
import javafx.scene.web.WebView;
import org.appxi.cbeta.explorer.event.DataEvent;
import org.appxi.cbeta.explorer.search.SearchHelper;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.theme.Theme;
import org.appxi.javafx.theme.ThemeSet;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchPrimaryController;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AppWorkbench extends WorkbenchApplication {

    @Override
    public void init() throws Exception {
        super.init();
        initThemes();
        eventBus.addEventHandler(ApplicationEvent.STARTED, event -> Platform.runLater(WebView::new));
    }

    private void initThemes() {
        themeProvider.addTheme(ThemeSet.light("light", "Default Light", "#e9e9eb")
                .addStylesheet(getClass().getResource("/appxi/cbetaExplorer/themes/theme-light-default.css"))
                .addTheme(Theme.light("web-light", "亮", "#e9e9eb")
                        .addStylesheet(getClass().getResource("/appxi/cbetaExplorer/themes/theme-light-default-web.css"))
                ));

        themeProvider.addTheme(ThemeSet.light("light-2", "Extend Light", "#328291")
                .addStylesheet(getClass().getResource("/appxi/cbetaExplorer/themes/theme-light-extend.css"))
                .addTheme(Theme.light("web-light-2", "明亮", "#328291")
                        .addStylesheet(getClass().getResource("/appxi/cbetaExplorer/themes/theme-light-extend-web.css"))
                ));

        themeProvider.addTheme(ThemeSet.light("light-javafx", "JavaFX Light", "#dddddd")
                .addStylesheet(getClass().getResource("/appxi/cbetaExplorer/themes/theme-light-javafx.css"))
        );

        themeProvider.addTheme(ThemeSet.dark("dark", "Default Dark", "#3b3b3b")
                .addStylesheet(getClass().getResource("/appxi/cbetaExplorer/themes/theme-dark-default.css"))
                .addTheme(Theme.dark("web-dark", "暗", "#3b3b3b")
                        .addStylesheet(getClass().getResource("/appxi/cbetaExplorer/themes/theme-dark-default-web.css"))
                ));
    }

    @Override
    protected void started() {
        super.started();
        CompletableFuture.runAsync(() -> {
            CbetaxHelper.books.getDataMap();
            SearchHelper.searchById = id -> CbetaxHelper.books.getDataMap().get(id);
        }).whenComplete((o, err) -> {
            eventBus.fireEvent(new DataEvent(DataEvent.BOOKS_READY));
            SearchHelper.setupSearchService(this);
        });
    }

    @Override
    protected String getApplicationId() {
        return AppInfo.ID;
    }

    @Override
    protected String getApplicationTitle() {
        return AppInfo.NAME;
    }

    @Override
    protected List<URL> getApplicationIcons() {
        final int[] iconSizes = new int[]{24, 32, 48, 64, 72, 96, 128};
        final List<URL> result = new ArrayList<>(iconSizes.length);
        for (int iconSize : iconSizes) {
            result.add(getClass().getResource("/appxi/cbetaExplorer/icons/icon-" + iconSize + ".png"));
        }
        return result;
    }

    @Override
    protected WorkbenchPrimaryController createPrimaryController() {
        return new WorkbenchRootController(this);
    }
}
