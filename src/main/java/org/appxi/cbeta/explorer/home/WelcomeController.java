package org.appxi.cbeta.explorer.home;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.appxi.cbeta.explorer.workbench.WorkbenchOpenpartControllerExt;

public class WelcomeController extends WorkbenchOpenpartControllerExt {

    public WelcomeController() {
        super("WELCOME", "欢迎");
    }

    public Label getViewpartInfo() {
        return new Label(this.viewName, new FontAwesomeIconView(FontAwesomeIcon.HOME));
    }

    @Override
    public void setupInitialize() {
    }

    @Override
    public StackPane getViewport() {
        final Label label = new Label();
        label.setStyle("-fx-font-size: 2em; -fx-opacity: .5;");
        label.setText("双击Shift ／ Ctrl+O 检索【书名／章节／作者／译者／...】");

        viewpartVbox.setAlignment(Pos.CENTER);
        viewpartVbox.getChildren().add(label);
        //
        return super.getViewport();
    }
}
