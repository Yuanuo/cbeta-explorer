package org.appxi.cbeta.explorer;

import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AppPreloader extends Preloader {
    private Stage primaryStage;
    private VBox rootPane;
    private ProgressBar progressBar;
    private Button hacker;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        final ImageView imageView = new ImageView();
        imageView.setImage(new Image(getClass().getResourceAsStream("/appxi/cbetaExplorer/images/splash.jpg")));
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(799);

        //
        hacker = new Button();
        hacker.setMaxSize(.1, .1);
        hacker.setMinSize(.1, .1);
        hacker.setPrefSize(.1, .1);
        hacker.setVisible(false);
        hacker.setOnAction(event -> CbetaxHelper.setDataDirectory(primaryStage));
        //
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        final HBox bottomBar = new HBox(hacker, progressBar);
        bottomBar.getStyleClass().add("black-bg");
        rootPane = new VBox(imageView, bottomBar);

        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(new Scene(rootPane));
        primaryStage.getScene().getStylesheets().add(getClass().getResource("/appxi/cbetaExplorer/themes/preloader.css").toExternalForm());
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
        } else if (noteInfo instanceof StateChangeNotification info) {
            primaryStage.close(); // seem it always be BEFORE_START
        }
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        switch (info.getType()) {
            case BEFORE_LOAD:
//                loaded = true;
//                    label.textProperty().set("初始化成功...");
                break;
            case BEFORE_INIT:
//                    label.textProperty().set("正在加载模块...");
                break;
            case BEFORE_START:
                break;
//                    label.textProperty().set("加载成功，即将跳转到主页面");
        }
    }
}
