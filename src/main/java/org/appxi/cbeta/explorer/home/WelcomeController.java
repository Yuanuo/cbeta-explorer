package org.appxi.cbeta.explorer.home;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchMainViewController;

public class WelcomeController extends WorkbenchMainViewController {
    public WelcomeController(WorkbenchApplication application) {
        super("WELCOME", "欢迎", application);
    }

    @Override
    public Node createToolIconGraphic(Boolean placeInSideViews) {
        return null;
    }

    @Override
    public void setupInitialize() {
    }

    @Override
    protected void onViewportInitOnce() {
        final Label label = new Label();
        label.setStyle("-fx-font-size: 2em; -fx-opacity: .5;");
        label.setText("双击Shift ／ Ctrl+O 检索【书名／章节／作者／译者／...】");

        viewportVBox.setAlignment(Pos.CENTER);
        viewportVBox.getChildren().add(label);
    }

    @Override
    public void onViewportShow(boolean firstTime) {
    }
}
