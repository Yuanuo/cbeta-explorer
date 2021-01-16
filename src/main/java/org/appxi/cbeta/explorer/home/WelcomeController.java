package org.appxi.cbeta.explorer.home;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.control.Label;
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
        // TODO Auto-generated method stub

    }

}
