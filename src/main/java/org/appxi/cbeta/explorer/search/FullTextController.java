package org.appxi.cbeta.explorer.search;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.scene.Node;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;

public class FullTextController extends WorkbenchSideViewController {

    public FullTextController(WorkbenchApplication application) {
        super("FULLTEXT", "搜索", application);
    }

    @Override
    public Node createToolIconGraphic(Boolean placeInSideViews) {
        return new MaterialIconView(MaterialIcon.SEARCH);
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
