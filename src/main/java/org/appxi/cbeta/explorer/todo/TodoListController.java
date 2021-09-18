package org.appxi.cbeta.explorer.todo;

import org.controlsfx.glyphfont.MaterialIcon;
import org.controlsfx.glyphfont.MaterialIconView;
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
