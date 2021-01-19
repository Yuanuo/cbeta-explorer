package org.appxi.cbeta.explorer;

import javafx.application.Preloader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AppPreloader extends Preloader {
    private Stage primaryStage;
    private BorderPane rootPane;
    private ProgressBar progressBar;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        rootPane = new BorderPane();
        final ImageView imageView = new ImageView();
        imageView.setImage(new Image(getClass().getResourceAsStream("/appxi/cbetaExplorer/images/splash.jpg")));
        rootPane.setCenter(imageView);
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(800);
        rootPane.setBottom(progressBar);

        //
        final Button button = new Button();
        button.setText("");
        button.setMaxSize(1, 1);
        button.setMinSize(1, 1);
        button.setPrefSize(1, 1);
        rootPane.setTop(button);
        button.setOnAction(event -> CbetaxHelper.setDataDirectory(primaryStage));
        //

        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(new Scene(rootPane));
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void handleProgressNotification(ProgressNotification info) {
        double percent = info.getProgress();
        percent = percent > 1 ? 1 : percent;
        progressBar.setProgress(percent);
        if (percent == 0.111) {
            ((Button) rootPane.getTop()).fire();
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
