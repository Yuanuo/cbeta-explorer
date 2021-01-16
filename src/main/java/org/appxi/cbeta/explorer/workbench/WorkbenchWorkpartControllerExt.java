package org.appxi.cbeta.explorer.workbench;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.appxi.javafx.control.AlignedBar;
import org.appxi.javafx.workbench.views.WorkbenchWorkpartController;

public abstract class WorkbenchWorkpartControllerExt extends WorkbenchWorkpartController {
    protected final StackPane viewport;
    protected final VBox viewpartVbox;
    protected final AlignedBar toolbar;

    public WorkbenchWorkpartControllerExt(String viewId, String viewName) {
        super(viewId, viewName);
        this.toolbar = new AlignedBar();
        this.viewpartVbox = new VBox(this.toolbar);
        this.viewport = new StackPane(this.viewpartVbox);
        //
        final Label workviewName = new Label(viewName);
        workviewName.getStyleClass().add("workview-name");
        this.toolbar.addLeft(workviewName);
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
