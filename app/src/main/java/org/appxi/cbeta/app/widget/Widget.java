package org.appxi.cbeta.app.widget;

import javafx.scene.Node;
import org.appxi.util.ext.Attributes;

abstract class Widget extends Attributes {
    final WidgetsController controller;

    protected Widget(WidgetsController controller) {
        this.controller = controller;
    }

    abstract String getName();

    abstract Node getViewport();

    abstract void activeViewport(boolean firstTime);
}
