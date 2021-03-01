package org.appxi.cbeta.explorer.todo;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.scene.Node;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;

public class TodoListController extends WorkbenchSideViewController {

    public TodoListController(WorkbenchApplication application) {
        super("TODOLIST", "待读", application);
    }

    @Override
    public Node createToolIconGraphic(boolean sideToolOrElseViewTool) {
        return new MaterialIconView(MaterialIcon.UPDATE);
    }

    @Override
    public void setupInitialize() {

    }

    @Override
    protected void onViewportInitOnce() {

    }

    @Override
    public void onViewportShow(boolean firstTime) {

    }
}
