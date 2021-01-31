package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.appxi.javafx.control.TabPaneExt;

public class BookViewerEx extends BookViewer {
    protected final BookViewController controller;
    public final TabPane sideViews;

    public final BookBasicView bookBasicView;

    public BookViewerEx(BookViewController controller) {
        this.controller = controller;

        this.sideViews = new TabPaneExt();
        this.sideViews.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(this.sideViews, Priority.ALWAYS);
        //
        final Tab tab1 = new Tab("基本", this.bookBasicView = new BookBasicView());
        //
        this.sideViews.getTabs().addAll(tab1);
    }

    @Override
    protected void initToolbar() {
        this.initToolbar_SideControl();
        super.initToolbar();
    }

    private void initToolbar_SideControl() {
        final ToggleButton sideControlBtn = new ToggleButton();
        sideControlBtn.setGraphic(new MaterialIconView(MaterialIcon.IMPORT_CONTACTS));
        sideControlBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        sideControlBtn.setOnAction(event -> this.controller.getPrimaryViewport().selectSideTool(BookDataController.VIEW_ID));
        this.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE)
                sideControlBtn.fire();
        });
        this.toolbar.addLeft(sideControlBtn);
    }

}
