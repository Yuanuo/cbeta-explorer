package org.appxi.cbeta.explorer.workbench;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.appxi.javafx.workbench.views.WorkbenchOpenpartController;

public abstract class WorkbenchOpenpartControllerExt extends WorkbenchOpenpartController {
    protected final StackPane viewport;
    protected final VBox viewpartVbox;

    public WorkbenchOpenpartControllerExt(String viewId, String viewName) {
        super(viewId, viewName);
        this.viewpartVbox = new VBox();
        this.viewport = new StackPane(this.viewpartVbox);
    }

    @Override
    public Label getViewpartInfo() {
        return new Label(this.viewName);
    }

    @Override
    public StackPane getViewport() {
        return viewport;
    }
}
