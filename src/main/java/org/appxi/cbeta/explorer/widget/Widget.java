package org.appxi.cbeta.explorer.widget;

import javafx.scene.Node;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.util.ext.Attributes;

abstract class Widget extends Attributes {
    final WorkbenchViewController controller;

    protected Widget(WorkbenchViewController controller) {
        this.controller = controller;
    }

    abstract String getName();

    abstract Node getViewport();

    abstract void onViewportShow(boolean firstTime);
}
