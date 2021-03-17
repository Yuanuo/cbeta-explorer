package org.appxi.cbeta.explorer.todo;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;

public class TodoListController extends WorkbenchSideViewController {

    public TodoListController(WorkbenchApplication application) {
        super("TODOLIST", application);
        this.setTitles("待读");
        this.viewIcon.set(new MaterialIconView(MaterialIcon.UPDATE));
    }

    @Override
    public void setupInitialize() {

    }

    @Override
    protected void onViewportInitOnce() {

    }

    @Override
    public void onViewportShowing(boolean firstTime) {

    }

    @Override
    public void onViewportHiding() {
    }
}
