package org.appxi.cbeta.explorer;

import javafx.application.Platform;
import javafx.scene.web.WebView;
import org.appxi.cbeta.explorer.dao.DaoHelper;
import org.appxi.cbeta.explorer.dao.DaoService;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.theme.Theme;
import org.appxi.javafx.theme.ThemeSet;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchPrimaryController;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.DateHelper;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AppWorkbench extends WorkbenchApplication {
    public AppWorkbench() {
        AppContext.setApplication(this);
    }

    @Override
    public void init() throws Exception {
        super.init();
        // 由于在配置文件中不能使用动态变量作为路径，故在此设置日志文件路径
        if (FxHelper.productionMode) {
            System.setProperty("org.slf4j.simpleLogger.logFile", UserPrefs.dataDir().resolve(".logs")
                    .resolve(DateHelper.format(new Date()).replaceAll("[\s:]", "-").concat(".log")).toString());
        }
        // 在此设置数据库基本环境，以供后续的功能正常使用
        DaoHelper.setupDatabaseService(UserPrefs.dataDir().resolve(".db"));
        initThemes();
        eventBus.addEventHandler(ApplicationEvent.STARTED, event -> Platform.runLater(WebView::new));
        CompletableFuture.runAsync(() -> {
            DaoService.setupInitialize();
            ChineseConvertors.hans2HantTW("测试");
            AppContext.setupInitialize();
        }).whenComplete((o, err) -> {
            if (null != err) FxHelper.alertError(this, err);
        });
    }

    private void initThemes() {
        themeProvider.addTheme(ThemeSet.light("light", "Light", "#e9e9eb")
                .addStylesheet(css("/appxi/cbetaExplorer/themes/theme-light-default.css"))
                .addTheme(Theme.light("default", "亮", "#e9e9eb")
                        .addStylesheet(css("/appxi/cbetaExplorer/themes/base-web.css"))
                        .addStylesheet(css("/appxi/cbetaExplorer/themes/theme-light-default-web.css"))
                ));

        themeProvider.addTheme(ThemeSet.light("light-2", "Green", "#328291")
                .addStylesheet(css("/appxi/cbetaExplorer/themes/theme-light-extend.css"))
                .addTheme(Theme.light("default", "明亮", "#328291")
                        .addStylesheet(css("/appxi/cbetaExplorer/themes/base-web.css"))
                        .addStylesheet(css("/appxi/cbetaExplorer/themes/theme-light-extend-web.css"))
                ));

        themeProvider.addTheme(ThemeSet.light("light-javafx", "JavaFX Light", "#dddddd")
                .addStylesheet(css("/appxi/cbetaExplorer/themes/theme-light-javafx.css"))
                .addTheme(Theme.light("default", "亮", "#e9e9eb")
                        .addStylesheet(css("/appxi/cbetaExplorer/themes/base-web.css"))
                        .addStylesheet(css("/appxi/cbetaExplorer/themes/theme-light-javafx-web.css"))
                ));

        themeProvider.addTheme(ThemeSet.dark("dark", "Dark", "#3b3b3b")
                .addStylesheet(css("/appxi/cbetaExplorer/themes/theme-dark-default.css"))
                .addTheme(Theme.dark("default", "暗", "#3b3b3b")
                        .addStylesheet(css("/appxi/cbetaExplorer/themes/base-web.css"))
                        .addStylesheet(css("/appxi/cbetaExplorer/themes/theme-dark-default-web.css"))
                ));
    }

    protected String css(String path) {
        return getClass().getResource(path).toExternalForm();
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
        final String[] iconSizes = new String[]{"24", "32", "48", "64", "72", "96", "128"};
        final List<URL> result = new ArrayList<>(iconSizes.length);
        for (String iconSize : iconSizes) {
            result.add(getClass().getResource("/appxi/cbetaExplorer/icons/icon-".concat(iconSize).concat(".png")));
        }
        return result;
    }

    @Override
    protected WorkbenchPrimaryController createPrimaryController() {
        return new WorkbenchRootController(this);
    }
}
