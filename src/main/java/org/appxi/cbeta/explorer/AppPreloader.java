package org.appxi.cbeta.explorer;

import appxi.cbeta.BookcaseInDir;
import appxi.cbeta.BookcaseInZip;
import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.appxi.javafx.control.CardChooser;
import org.appxi.javafx.iconfont.MaterialIcon;
import org.appxi.prefs.UserPrefs;

import java.io.File;
import java.util.Optional;

public class AppPreloader extends Preloader {
    static Stage primaryStage;
    private ProgressBar progressBar;
    private Button hacker;

    @Override
    public void start(Stage primaryStage) throws Exception {
        AppPreloader.primaryStage = primaryStage;

        final ImageView imageView = new ImageView();
        Optional.ofNullable(getClass().getResourceAsStream("/appxi/cbetaExplorer/images/splash.jpg"))
                .ifPresent(v -> imageView.setImage(new Image(v)));
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(799);

        //
        hacker = new Button();
        hacker.setMaxSize(.1, .1);
        hacker.setMinSize(.1, .1);
        hacker.setPrefSize(.1, .1);
        hacker.setVisible(false);
        hacker.setOnAction(event -> setupBookcase(primaryStage));
        //
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        final HBox bottomBar = new HBox(hacker, progressBar);
        bottomBar.getStyleClass().add("black-bg");
        VBox rootPane = new VBox(imageView, bottomBar);

        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(new Scene(rootPane));
        Optional.ofNullable(getClass().getResource("/appxi/cbetaExplorer/themes/preloader.css"))
                .ifPresent(v -> primaryStage.getScene().getStylesheets().add(v.toExternalForm()));
        Optional.ofNullable(getClass().getResource("/appxi/cbetaExplorer/themes/theme-app.css"))
                .ifPresent(v -> primaryStage.getScene().getStylesheets().add(v.toExternalForm()));

        Optional.ofNullable(getClass().getResourceAsStream("/appxi/cbetaExplorer/icons/icon-32.png"))
                .ifPresent(v -> primaryStage.getIcons().setAll(new Image(v)));
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void handleProgressNotification(ProgressNotification info) {
        double percent = info.getProgress();
        percent = percent > 1 ? 1 : percent;
        progressBar.setProgress(percent);
        if (percent == 0.111) {
            hacker.fire();
        }
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification noteInfo) {
        if (noteInfo instanceof ProgressNotification info) {
            handleProgressNotification(info);
        } else if (noteInfo instanceof StateChangeNotification) {
            primaryStage.close(); // seem it be always BEFORE_START
        }
    }

    static void setupBookcase(Stage primaryStage) {
        final String key = "bookcase";
        while (true) {
            // 1，优先使用已经选择的数据包
            // 2，非正规方法，尝试加载当前目录下的数据包
            for (String path : new String[]{
                    UserPrefs.prefs.getString(key, ""),
                    "../cbeta.zip",
                    "cbeta.zip",
                    "../bookcase.zip",
                    "bookcase.zip",
            }) {
                if (path.endsWith(".zip")) {
                    try {
                        AppContext.setupInitialize(new BookcaseInZip(path));
                        return;
                    } catch (Throwable ignore) {
                    }
                }
            }
            // 3，提示并让用户选择数据源
            final Optional<CardChooser.Card> optional = CardChooser.of("选择CBETA Bookcase数据源")
                    .header("选择使用Bookcase Zip数据包 或者 数据目录？", null)
                    .cancelable()
                    .owner(primaryStage)
                    .cards(CardChooser.ofCard("Bookcase数据包")
                                    .description("CBETA官方发布的Bookcase Zip数据包。文件名类似于“bookcase_v061_20210710.zip。")
                                    .graphic(MaterialIcon.ARCHIVE.iconView())
                                    .userData(true)
                                    .get(),
                            CardChooser.ofCard("Bookcase数据目录")
                                    .description("CBETA官方阅读器CBReader自带的Bookcase数据目录。")
                                    .graphic(MaterialIcon.FOLDER.iconView())
                                    .userData(false)
                                    .get(),
                            CardChooser.ofCard("退出")
                                    .description("请退出")
                                    .graphic(MaterialIcon.EXIT_TO_APP.iconView())
                                    .get()
                    ).showAndWait();
            if (optional.isEmpty() || optional.get().userData() == null) {
                System.exit(-1);
                return;
            }
            // 4，提示并让用户选择数据包
            if (optional.get().userData() == Boolean.TRUE) {
                final FileChooser chooser = new FileChooser();
                chooser.setTitle("请选择CBETA Bookcase Zip数据包");
                chooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Bookcase Zip File", "bookcase_*.zip"));
                final File selected = chooser.showOpenDialog(primaryStage);
                if (null != selected) {
                    try {
                        String path = selected.getAbsolutePath();
                        AppContext.setupInitialize(new BookcaseInZip(path));
                        UserPrefs.prefs.setProperty(key, path);
                        return;
                    } catch (Throwable ignore) {
                    }
                }
            }
            // 5，提示并让用户选择数据目录
            if (optional.get().userData() == Boolean.FALSE) {
                for (String path : new String[]{
                        UserPrefs.prefs.getString(key, ""),
                        String.valueOf(UserPrefs.prefs.removeProperty("cbeta.dir"))
                }) {
                    if (!path.endsWith(".zip")) {
                        try {
                            AppContext.setupInitialize(new BookcaseInDir(path));
                            return;
                        } catch (Throwable ignore) {
                        }
                    }
                }
                //
                final DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle("请选择CBETA数据主目录（Bookcase 或 Bookcase/CBETA）");
                final File selected = chooser.showDialog(primaryStage);
                if (null != selected) {
                    try {
                        String path = selected.getAbsolutePath();
                        AppContext.setupInitialize(new BookcaseInDir(path));
                        return;
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
    }
}
