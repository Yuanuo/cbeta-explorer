package org.appxi.cbeta.explorer.home;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import org.appxi.javafx.workbench.views.WorkbenchWorktoolController;

public class AboutController extends WorkbenchWorktoolController {
    public AboutController() {
        super("ABOUT", "关于");
    }

    @Override
    public Label getViewpartInfo() {
        final Label info = new Label(this.viewName, new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE));
        info.setAlignment(Pos.CENTER_RIGHT);
        return info;
    }

    @Override
    public Node getViewport() {
        return null;
    }

    @Override
    public void setupInitialize() {
    }

    @Override
    public void onViewportSelected(boolean firstTime) {
        new Alert(Alert.AlertType.INFORMATION, "About me...").show();
    }
}
