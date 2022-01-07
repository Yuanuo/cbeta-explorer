package org.appxi.cbeta.explorer;

import appxi.cbeta.BookcaseInDir;
import appxi.cbeta.BookcaseInZip;
import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.appxi.javafx.control.CardChooser;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.visual.Swatch;
import org.appxi.javafx.visual.Theme;
import org.appxi.javafx.visual.Visual;
import org.appxi.prefs.UserPrefs;

import java.io.File;
import java.util.Optional;

public class AppPreloader extends Preloader {
    private static Stage primaryStage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        AppPreloader.primaryStage = primaryStage;
        primaryStage.setTitle(App.NAME);

        final ImageView imageView = new ImageView();
        Optional.ofNullable(AppPreloader.class.getResourceAsStream("splash.jpg"))
                .ifPresent(v -> imageView.setImage(new Image(v)));

        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(new Scene(new BorderPane(imageView), 800, 450));

        Optional.ofNullable(getClass().getResourceAsStream("icon-32.png"))
                .ifPresent(v -> primaryStage.getIcons().setAll(new Image(v)));
        primaryStage.centerOnScreen();
        primaryStage.show();
        setupBookcase(primaryStage);
        new javafx.scene.control.TextField("");
        new javax.swing.JTextField("");
    }

    public static void hide() {
        if (null != primaryStage) primaryStage.close();
    }

    private static boolean themed = false;

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
            if (!themed) {
                primaryStage.getScene().getRoot().setStyle("-fx-font-size: 16px;");
                Theme.getDefault().assignTo(primaryStage.getScene());
                Swatch.getDefault().assignTo(primaryStage.getScene());
                Visual.getDefault().assignTo(primaryStage.getScene());
                themed = true;
            }
            // 3，提示并让用户选择数据源
            final Optional<CardChooser.Card> optional = CardChooser.of("选择CBETA Bookcase数据源")
                    .header("选择使用Bookcase Zip数据包 或者 数据目录？", null)
                    .owner(primaryStage)
                    .cards(CardChooser.ofCard("Bookcase数据包")
                                    .description("CBETA官方发布的Bookcase Zip数据包。文件名类似于“bookcase_v061_20210710.zip。")
                                    .graphic(MaterialIcon.ARCHIVE.graphic())
                                    .userData(true)
                                    .get(),
                            CardChooser.ofCard("Bookcase数据目录")
                                    .description("CBETA官方阅读器CBReader自带的Bookcase数据目录。")
                                    .graphic(MaterialIcon.FOLDER.graphic())
                                    .userData(false)
                                    .get(),
                            CardChooser.ofCard("退出")
                                    .description("请退出")
                                    .graphic(MaterialIcon.EXIT_TO_APP.graphic())
                                    .get()
                    ).showAndWait();
            if (optional.isEmpty() || optional.get().userData() == null) {
                System.exit(0);
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
