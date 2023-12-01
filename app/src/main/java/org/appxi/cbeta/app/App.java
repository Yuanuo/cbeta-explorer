package org.appxi.cbeta.app;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.appxi.cbeta.Bookcase;
import org.appxi.cbeta.Profile;
import org.appxi.cbeta.ProfileManager;
import org.appxi.dictionary.ui.DictionaryContext;
import org.appxi.file.FileWatcher;
import org.appxi.javafx.app.AppEvent;
import org.appxi.javafx.app.BaseApp;
import org.appxi.javafx.app.BootstrapApp;
import org.appxi.javafx.control.CardChooser;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.visual.VisualEvent;
import org.appxi.javafx.web.WebPane;
import org.appxi.util.FileHelper;
import org.appxi.util.OSVersions;

import java.net.URL;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class App extends BootstrapApp {
    public static final String ID = "smartBooks";
    public static final String NAME = "典 · 集";
    public static final String VERSION = "23.11.31";

    public final Bookcase bookcase;

    public final ProfileManager profileMgr;
    private final Map<Profile, DataApp> openedProfiles = new HashMap<>();

    public App() {
        super(AppContext.workspace);
        this.bookcase = AppContext.bookcase();
        this.profileMgr = new ProfileManager(bookcase, workspace, config);
    }

    @Override
    public void init() {
        super.init();
        //
        new Thread(WebPane::preloadLibrary).start();
        //
        settings.add(() -> visualProvider().optionForFontSmooth());
        settings.add(() -> visualProvider().optionForFontName());
        settings.add(() -> visualProvider().optionForFontSize());
        settings.add(() -> visualProvider().optionForTheme());
        settings.add(() -> visualProvider().optionForSwatch());
        settings.add(() -> visualProvider().optionForWebFontName());
        settings.add(() -> visualProvider().optionForWebFontSize());
        settings.add(() -> visualProvider().optionForWebPageColor());
        settings.add(() -> visualProvider().optionForWebTextColor());
        //
        DictionaryContext.setupDirectories(this);
        //
        SpringConfig.setup(this);
    }

    @Override
    protected void showing(Stage primaryStage) {
        //
        final List<String> logs = profileMgr.config.getPropertyKeys().stream()
                .filter(s -> s.endsWith(".closeAt"))
                .map(s -> s.substring(0, s.length() - 8) + "/" + profileMgr.config.getString(s, "0"))
                .sorted(Comparator.comparing(s -> s.split("/", 2)[1]))
                .toList();
        final String defProfileId = !logs.isEmpty() ? logs.getLast().split("/", 2)[0] : profileMgr.getDefaultProfileId();
        final Profile defProfile = profileMgr.getProfile(defProfileId);

        if (null != defProfile && isProfileLockable(defProfile)) {
            primaryStage.setOpacity(0);
            super.showing(primaryStage);
            //
            FxHelper.runLater(() -> openProfile(defProfile));
        } else {
            title.set("打开书单");
            final Node pane = CardChooser.of("打开书单")
                    .owner(getPrimaryStage())
                    .cards(buildCards())
                    .buildPane(card -> openProfile(card.userData()));
            pane.getStyleClass().add("card-chooser");
            pane.setStyle("-fx-padding: 15px;");

            Label head = new Label("从书单开始");
            head.setStyle("-fx-padding: 15px;");

            getPrimaryGlass().getChildren().setAll(new VBox(15, pane));
            //
            super.showing(primaryStage);
        }
        //
        String cssByOS = "desktop@" + OSVersions.osName.toLowerCase().replace(" ", "") + ".css";
        if (BaseApp.productionMode) {
            Optional.ofNullable(App.class.getResource("app_desktop.css"))
                    .ifPresent(v -> primaryStage.getScene().getStylesheets().add(v.toExternalForm()));
            Optional.ofNullable(App.class.getResource("app_" + cssByOS))
                    .ifPresent(v -> primaryStage.getScene().getStylesheets().add(v.toExternalForm()));
        } else {
            Scene scene = primaryStage.getScene();
            visualProvider().visual().unAssign(scene);
            watchCss(scene, Path.of("../../appxi-javafx/src/main/resources/org/appxi/javafx/visual/visual_desktop.css"));
            watchCss(scene, Path.of("src/main/resources/org/appxi/cbeta/app/app_desktop.css"));
            watchCss(scene, Path.of("src/main/resources/org/appxi/cbeta/app/app_" + cssByOS));
            scene.getStylesheets().forEach(System.out::println);
        }
        //
        AppPreloader.hide();
    }

    private void watchCss(Scene scene, Path file) {
        try {
            final String filePath = file.toRealPath().toUri().toString().replace("///", "/");
            System.out.println("watch css: " + filePath);
            scene.getStylesheets().add(filePath);
        } catch (Exception e) {
            System.err.println(e.getMessage());
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
                visualProvider().eventBus.fireEvent(new VisualEvent(VisualEvent.SET_STYLE, null));
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
    public String getAppName() {
        return NAME;
    }

    @Override
    protected List<URL> getAppIcons() {
        return Stream.of("256")
                .map(s -> App.class.getResource("icon-" + s + ".png"))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<CardChooser.Card> buildCards() {
        return profileMgr.getProfiles().stream().map(p -> {
                    final String status;
                    if (openedProfiles.containsKey(p)) {
                        status = "已开（点击切换）";
                    } else if (isProfileLockable(p)) {
                        status = "可用（点击打开）";
                    } else {
                        status = "锁定";
                    }
                    return CardChooser.ofCard(p.toString())
                            .description("状态：" + status + "  |  备注：" + p.description())
                            .graphic(MaterialIcon.PLAYLIST_ADD_CHECK.graphic())
                            .focused(openedProfiles.containsKey(p))
                            .userData(p)
                            .get();
                })
                .toList();
    }

    public void selectProfile() {
        final Optional<CardChooser.Card> optional = CardChooser.of("打开书单")
                .owner(getPrimaryStage())
                .cards(buildCards())
                .showAndWait();
        //
        Profile profile = optional.isEmpty() || optional.get().userData() == null
                ? null
                : optional.get().userData();

        if (profile == null)
            return;

        openProfile(profile);
    }

    private void openProfile(Profile profile) {
        DataApp dataApp = openedProfiles.get(profile);
        if (null != dataApp) {
            dataApp.getPrimaryStage().toFront();
            getPrimaryStage().hide();
            return;
        }

        DataContext dataContext = null;
        try {
            dataContext = new DataContext(AppContext.bookcase, AppContext.bookMap, profile);
            dataApp = new DataApp(this, dataContext);
            dataApp.eventBus.addEventHandler(AppEvent.STARTED, event -> {
                FxHelper.runLater(() -> {
                    getPrimaryStage().hide();
                    AppPreloader.hide();
                });
            });
            dataApp.start(new Stage());
            openedProfiles.put(profile, dataApp);
            dataApp.eventBus.addEventHandler(AppEvent.STOPPING, event -> {
                logger.info("close&remove profile " + profile.title() + " from openedProfiles");
                openedProfiles.remove(profile);
                profileMgr.config.setProperty(profile.id() + ".closeAt", System.currentTimeMillis());
            });
        } catch (Exception e) {
            if (null != dataContext) {
                dataContext.release();
            }
            alertError(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void alertError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(getPrimaryStage());
        alert.setContentText(message);
        alert.showAndWait();
    }

    public boolean isProfileLockable(Profile profile) {
        final Path lockFile = profile.workspace().resolve(".lock");
        if (FileHelper.notExists(lockFile)) {
            return true;
        }
        try {
            FileHelper.delete(lockFile);
            return FileHelper.notExists(lockFile);
        } catch (Throwable t) {
            return false;
        }
    }
}
