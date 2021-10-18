package org.appxi.cbeta.explorer;

import javafx.application.Platform;
import javafx.scene.web.WebView;
import org.appxi.cbeta.explorer.dao.DaoHelper;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchPrimaryController;
import org.appxi.prefs.UserPrefs;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    }

    @Override
    protected void showing() {
        AppPreloader.primaryStage.close();
    }

    @Override
    protected URL getResource(String path) {
        return (path.startsWith("/appxi/javafx/") ? WorkbenchApplication.class : AppWorkbench.class).getResource(path);
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
        final String[] iconSizes = new String[]{"32", "64", "128", "256"};
        final List<URL> result = new ArrayList<>(iconSizes.length);
        for (String iconSize : iconSizes) {
            result.add(AppWorkbench.class.getResource("/appxi/cbetaExplorer/icons/icon-".concat(iconSize).concat(".png")));
        }
        return result;
    }

    @Override
    protected WorkbenchPrimaryController createPrimaryController() {
        return new WorkbenchRootController(this);
    }
}
