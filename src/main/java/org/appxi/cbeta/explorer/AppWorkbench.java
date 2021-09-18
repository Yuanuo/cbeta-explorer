package org.appxi.cbeta.explorer;

import javafx.application.Platform;
import javafx.scene.web.WebView;
import org.appxi.cbeta.explorer.dao.DaoHelper;
import org.appxi.cbeta.explorer.dao.DaoService;
import org.appxi.cbeta.explorer.model.BookList;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchPrimaryController;
import org.appxi.prefs.UserPrefs;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class AppWorkbench extends WorkbenchApplication {
    public AppWorkbench() {
        AppContext.setApplication(this);
    }

    @Override
    public void init() throws Exception {
        super.init();
        //
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
        // 在此设置数据库基本环境，以供后续的功能正常使用
        DaoHelper.setupDatabaseService(UserPrefs.dataDir().resolve(".db"));
        initThemes();
        eventBus.addEventHandler(ApplicationEvent.STARTED, event -> Platform.runLater(WebView::new));
        CompletableFuture.runAsync(() -> {
            BookList.books.getDataMap();
            DaoService.setupInitialize();
            ChineseConvertors.hans2HantTW("测试");
            AppContext.setupInitialize();
        }).whenComplete((o, err) -> {
            if (null != err) FxHelper.alertError(this, err);
        });
    }

    @Override
    protected URL getResource(String path) {
        return (path.startsWith("/appxi/javafx/") ? WorkbenchApplication.class : this.getClass()).getResource(path);
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
