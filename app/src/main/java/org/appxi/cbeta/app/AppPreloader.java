package org.appxi.cbeta.app;

import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.appxi.cbeta.BookcaseInDir;
import org.appxi.cbeta.BookcaseInZip;
import org.appxi.javafx.control.CardChooser;
import org.appxi.javafx.helper.FontFaceHelper;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.visual.Swatch;
import org.appxi.javafx.visual.Theme;
import org.appxi.javafx.visual.Visual;
import org.appxi.util.OSVersions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.appxi.cbeta.app.AppLauncher.appConfig;

public class AppPreloader extends Preloader {
    private static final Logger logger = LoggerFactory.getLogger(AppPreloader.class);
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
        //
        FontFaceHelper.fixFontFamilies();
        //
        if (OSVersions.isLinux || OSVersions.isMac) {
            new javafx.scene.control.TextField("");
            new javax.swing.JTextField("");
        }
    }

    public static void hide() {
        if (null != primaryStage) primaryStage.close();
    }

    private static boolean themed = false;

    private static Collection<Path> getPortablePaths() {
        final Collection<Path> result = new ArrayList<>();
        final Path appDir = FxHelper.appDir();
        final Path appDirParent = appDir.getParent();

        result.add(appDirParent.resolve("bookcase.zip"));
        result.add(appDirParent.resolve("cbeta.zip"));
        result.add(appDir.resolve("bookcase.zip"));
        result.add(appDir.resolve("cbeta.zip"));
        if (appDir.getNameCount() > 2) {
            result.add(appDirParent.getParent().resolve("bookcase.zip"));
            result.add(appDirParent.getParent().resolve("cbeta.zip"));
        }

        try {
            File[] files = appDirParent.toFile().listFiles((dir, name) -> {
                name = name.toLowerCase();
                return name.endsWith(".zip") && (name.startsWith("bookcase") || name.startsWith("cbeta"));
            });
            if (null != files && files.length > 0) {
                List.of(files).reversed().forEach(p -> result.add(p.toPath()));
            }
        } catch (Exception ignore) {
        }
        return result;
    }

    static void setupBookcase(Stage primaryStage) {
        final String key = "bookcase";
        while (true) {
            // 1，优先使用App集成数据包
            // 2，优先使用当前目录下的数据包
            for (Path path : getPortablePaths()) {
                try {
                    AppContext.setupBookcase(new BookcaseInZip(path.toString()));
                    appConfig.setProperty(key, AppContext.bookcase.getPath());
                    return;
                } catch (Throwable e) {
                    logger.error("try portable but {}", e.toString());
                }
            }
            // 3，优先使用已经选择的数据包
            try {
                AppContext.setupBookcase(new BookcaseInZip(appConfig.getString(key, "never used")));
                return;
            } catch (Throwable e) {
                logger.error("try lastUsed but {}", e.toString());
            }
            if (!themed) {
                primaryStage.getScene().getRoot().setStyle("-fx-font-size: 16px;");
                Theme.getDefault().assignTo(primaryStage.getScene());
                Swatch.getDefault().assignTo(primaryStage.getScene());
                Visual.getDefault().assignTo(primaryStage.getScene());
                Optional.ofNullable(AppPreloader.class.getResource("app_desktop.css"))
                        .ifPresent(v -> primaryStage.getScene().getStylesheets().add(v.toExternalForm()));
                themed = true;
            }
            // 3，提示并让用户选择数据源
            final Optional<CardChooser.Card> optional = CardChooser.of("选择CBETA Bookcase数据源")
                    .header("选择使用Bookcase Zip数据包 或者 数据目录？", null)
                    .owner(primaryStage)
                    .cards(CardChooser.ofCard("Bookcase数据包")
                                    .description("CBETA官方发布的Bookcase Zip数据包。文件名类似于“bookcase_v061_20210710.zip”。")
                                    .graphic(MaterialIcon.ARCHIVE.graphic())
                                    .userData(true)
                                    .get(),
                            CardChooser.ofCard("Bookcase数据目录")
                                    .description("CBETA官方阅读器CBReader自带的Bookcase数据目录。文件夹类似于“Bookcase/CBETA/”。")
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
                        new FileChooser.ExtensionFilter("CBETA Bookcase Zip File", "cbeta.zip", "bookcase.zip", "bookcase_*.zip")
                );
                final File selected = chooser.showOpenDialog(primaryStage);
                if (null != selected) {
                    try {
                        String path = selected.getAbsolutePath();
                        BookcaseInZip bookcase = new BookcaseInZip(path);
                        AppContext.setupBookcase(bookcase);
                        appConfig.setProperty(key, path);
                        appConfig.save();
                        return;
                    } catch (Throwable ignore) {
                    }
                }
            }
            // 5，提示并让用户选择数据目录
            if (optional.get().userData() == Boolean.FALSE) {
                for (String path : new String[]{
                        appConfig.getString(key, ""),
                        String.valueOf(appConfig.removeProperty("cbeta.dir"))
                }) {
                    if (!path.endsWith(".zip")) {
                        try {
                            BookcaseInDir bookcase = new BookcaseInDir(path);
                            AppContext.setupBookcase(bookcase);
                            appConfig.setProperty(key, path);
                            appConfig.save();
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
                        BookcaseInDir bookcase = new BookcaseInDir(path);
                        AppContext.setupBookcase(bookcase);
                        appConfig.setProperty(key, path);
                        appConfig.save();
                        return;
                    } catch (Throwable ignore) {
                    }
                }
            }
        }
    }
}
