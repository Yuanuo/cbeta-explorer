package org.appxi.cbeta.explorer;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.appxi.cbeta.explorer.book.BookDataPlaceController;
import org.appxi.cbeta.explorer.book.BooklistExplorer;
import org.appxi.cbeta.explorer.bookdata.BookmarksController;
import org.appxi.cbeta.explorer.bookdata.FavoritesController;
import org.appxi.cbeta.explorer.dao.DaoHelper;
import org.appxi.cbeta.explorer.home.AboutController;
import org.appxi.cbeta.explorer.home.PreferencesController;
import org.appxi.cbeta.explorer.recent.RecentItemsController;
import org.appxi.cbeta.explorer.recent.RecentViewsController;
import org.appxi.cbeta.explorer.search.LookupController;
import org.appxi.cbeta.explorer.search.SearchController;
import org.appxi.cbeta.explorer.widget.WidgetsController;
import org.appxi.file.FileWatcher;
import org.appxi.javafx.app.DesktopApp;
import org.appxi.javafx.control.WebPane;
import org.appxi.javafx.visual.VisualEvent;
import org.appxi.javafx.workbench.WorkbenchApp;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.prefs.UserPrefs;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class App extends WorkbenchApp {
    public static final String ID = "cbetaExplorer";
    public static final String NAME = "智悲乐藏";
    public static final String VERSION = "22.01.05";
    private static App instance;

    public App() {
        App.instance = this;
    }

    public static App app() {
        return instance;
    }

    @Override
    public void init() throws Exception {
        super.init();
        //
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
        // 在此设置数据库基本环境，以供后续的功能正常使用
        DaoHelper.setupDatabaseService(UserPrefs.dataDir().resolve(".db"));
        new Thread(WebPane::preloadLibrary).start();
    }

    @Override
    protected void showing(Stage primaryStage) {
        super.showing(primaryStage);
        if (DesktopApp.productionMode) {
            Optional.ofNullable(App.class.getResource("app_desktop.css"))
                    .ifPresent(v -> primaryStage.getScene().getStylesheets().add(v.toExternalForm()));
        } else {
            Scene scene = primaryStage.getScene();
            visualProvider.visual().unAssign(scene);
            watchCss(scene, Path.of("..\\appxi-javafx\\src\\main\\resources\\org\\appxi\\javafx\\visual\\visual_desktop.css"));
            watchCss(scene, Path.of("src\\main\\resources\\org\\appxi\\cbeta\\explorer\\app_desktop.css"));
            scene.getStylesheets().forEach(System.out::println);
        }

        AppPreloader.hide();
    }

    private void watchCss(Scene scene, Path file) {
        try {
            final String filePath = file.toRealPath().toUri().toString().replace("///", "/");
            System.out.println("watch css: " + filePath);
            scene.getStylesheets().add(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        FileWatcher watcher = new FileWatcher(file.getParent());
        watcher.watching();
        watcher.addListener(event -> {
            if (event.type != FileWatcher.WatchType.MODIFY) return;
            if (event.getSource().getFileName().toString().endsWith("~")) return;
            String css = null;
            try {
                css = event.getSource().toRealPath().toUri().toString().replace("///", "/");
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (null == css) return;
            System.out.println("CSS < " + css);
            if (css.endsWith("web.css")) {
                eventBus.fireEvent(new VisualEvent(VisualEvent.STYLE_CHANGED, null));
            } else if (scene.getStylesheets().contains(css)) {
                final int idx = scene.getStylesheets().indexOf(css);
                String finalCss = css;
                javafx.application.Platform.runLater(() -> {
                    scene.getStylesheets().remove(finalCss);
                    if (idx != -1) scene.getStylesheets().add(idx, finalCss);
                    else scene.getStylesheets().add(finalCss);
                });
            }
        });
    }

    @Override
    protected String getAppName() {
        return NAME;
    }

    @Override
    protected List<URL> getAppIcons() {
        final String[] iconSizes = new String[]{"32", "64", "128", "256"};
        final List<URL> result = new ArrayList<>(iconSizes.length);
        for (String iconSize : iconSizes) {
            result.add(App.class.getResource("icon-".concat(iconSize).concat(".png")));
        }
        return result;
    }

    @Override
    protected List<WorkbenchViewController> createWorkbenchViews(WorkbenchPane workbench) {
        final List<WorkbenchViewController> result = new ArrayList<>();

        result.add(new BooklistExplorer(workbench));
        result.add(new BookDataPlaceController(workbench));
        result.add(new RecentItemsController(workbench));
        result.add(new RecentViewsController(workbench));
        result.add(new FavoritesController(workbench));
        result.add(new BookmarksController(workbench));

        result.add(new LookupController(workbench));
        result.add(new SearchController(workbench));
        result.add(new WidgetsController(workbench));

        result.add(new PreferencesController(workbench));
        result.add(new AboutController(workbench));

        return result;
    }
}
