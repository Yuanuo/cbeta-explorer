package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.appxi.javafx.control.FlexibleBar;
import org.appxi.javafx.control.StackPaneEx;
import org.appxi.javafx.control.WebPane;
import org.appxi.javafx.theme.Theme;
import org.appxi.javafx.theme.ThemeSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BookViewPane extends StackPaneEx {
    protected final SplitPane rootViews;
    protected final StackPane sideViews;
    protected final WebPane webPane;
    protected final FlexibleBar flexibleBar;

    private ToggleButton sideToolsToggle, moreToolsToggle;
    private Theme currentTheme;

    public BookViewPane() {
        super();

        this.sideViews = new StackPane();
        this.sideViews.getStyleClass().add("book-view-side");

        this.flexibleBar = new FlexibleBar();
        this.flexibleBar.setMaxWidth(Double.NEGATIVE_INFINITY);
        this.flexibleBar.setMaxHeight(Double.NEGATIVE_INFINITY);

        this.webPane = new WebPane();
        this.webPane.getChildren().add(this.flexibleBar);

        this.rootViews = new SplitPane(webPane);
        this.rootViews.getStyleClass().add("book-view-root");
        this.rootViews.setDividerPositions(0.2);
        VBox.setVgrow(this.rootViews, Priority.ALWAYS);
        //
        this.getChildren().add(this.rootViews);
        this.getStyleClass().add("book-view");
        //
        this.initMoreThings();
    }

    protected void initMoreThings() {
        //
        sideToolsToggle = new ToggleButton();
        sideToolsToggle.getStyleClass().add("side-toggle");
        sideToolsToggle.setGraphic(new MaterialIconView(MaterialIcon.MENU));
        sideToolsToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        sideToolsToggle.setOnAction(this::handleSideViewToggleAction);
        this.flexibleBar.addTool(sideToolsToggle);
        //
        this.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE)
                sideToolsToggle.fire();
        });
        //
//        this.viewsSwap = new Button("Swap");
//        this.viewsSwap.getStyleClass().add("views-swap");
//        this.viewsSwap.setOnAction(this::handleViewsSwapAction);
//        this.viewsSwap.setDisable(true);, this.viewsSwap
        //
        moreToolsToggle = new ToggleButton();
        moreToolsToggle.getStyleClass().add("more-toggle");
        moreToolsToggle.setGraphic(new MaterialIconView(MaterialIcon.SWAP_HORIZ));
        moreToolsToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        this.flexibleBar.addTools(moreToolsToggle);
        updateFontsizeTools();
    }

    public void updateThemeTools(ThemeSet themeSet) {
        final List<Node> moreTools = this.flexibleBar.getTools(this.moreToolsToggle);
        int themeToolsIdx = 0;
        List<Node> themeTools = (List<Node>) this.moreToolsToggle.getProperties().get("THEME_TOOLS");
        if (null == themeTools)
            this.moreToolsToggle.getProperties().put("THEME_TOOLS", themeTools = new ArrayList<>());

        if (!themeTools.isEmpty()) {
            themeToolsIdx = moreTools.indexOf(themeTools.get(0));
            moreTools.removeAll(themeTools);
            themeTools.clear();
        }

        if (!themeSet.themes.isEmpty()) {
            themeTools.add(new Label("Theme: "));
            final ToggleGroup themeToolsGroup = new ToggleGroup();
            for (Theme theme : themeSet.themes) {
                final RadioButton btn = new RadioButton();
                btn.setToggleGroup(themeToolsGroup);
                btn.setUserData(theme);
                btn.setTooltip(new Tooltip(theme.title));
                if (Objects.equals(this.currentTheme, theme))
                    btn.setSelected(true);

                final MaterialIconView icon = new MaterialIconView(MaterialIcon.LENS);
                icon.setFill(Color.valueOf(theme.accentColor));
                btn.setGraphic(icon);

                btn.setOnAction(event -> {
                    final ToggleButton eventBtn = (ToggleButton) event.getSource();
                    this.applyTheme((Theme) eventBtn.getUserData());
                });
                themeTools.add(btn);
            }
            moreTools.addAll(themeToolsIdx, themeTools);
        }
        this.flexibleBar.setTools(this.moreToolsToggle, moreTools.toArray(new Node[0]));
    }

    protected void updateFontsizeTools() {
        final List<Node> moreTools = this.flexibleBar.getTools(this.moreToolsToggle);
        int fsizeToolsIdx = 0;
        List<Node> fsizeTools = (List<Node>) this.moreToolsToggle.getProperties().get("FSIZE_TOOLS");
        if (null == fsizeTools)
            this.moreToolsToggle.getProperties().put("FSIZE_TOOLS", fsizeTools = new ArrayList<>());

        if (!fsizeTools.isEmpty()) {
            fsizeToolsIdx = moreTools.indexOf(fsizeTools.get(0));
            moreTools.removeAll(fsizeTools);
            fsizeTools.clear();
        }

        fsizeTools.add(new Label("FontSize: ", new MaterialIconView(MaterialIcon.TEXT_FIELDS)));
        //
        final Button subBtn = new Button();
        subBtn.setUserData(false);
        subBtn.setGraphic(new MaterialIconView(MaterialIcon.EXPOSURE_NEG_1));
        subBtn.setTooltip(new Tooltip("减小字号"));
        subBtn.setOnAction(this::handleFontSizeAction);
        fsizeTools.add(subBtn);
        //
        final Button supBtn = new Button();
        supBtn.setUserData(true);
        supBtn.setGraphic(new MaterialIconView(MaterialIcon.EXPOSURE_PLUS_1));
        supBtn.setTooltip(new Tooltip("增大字号"));
        supBtn.setOnAction(this::handleFontSizeAction);
        fsizeTools.add(supBtn);
        //
        moreTools.addAll(fsizeToolsIdx, fsizeTools);
        this.flexibleBar.setTools(this.moreToolsToggle, moreTools.toArray(new Node[0]));
    }

    private void handleFontSizeAction(ActionEvent event) {
        final Button btn = (Button) event.getSource();
        final boolean supAct = (boolean) btn.getUserData();
    }

    private int lastSideViewIdx = 0;
    private double lastRootViewsDivider = 0.2;

    private void handleSideViewToggleAction(ActionEvent event) {
        final ToggleButton btn = (ToggleButton) event.getSource();
        final ObservableList<Node> items = this.rootViews.getItems();
        final int idx = items.indexOf(this.sideViews);
        if (idx == -1) { // not exists, need show it
            items.add(lastSideViewIdx, this.sideViews);
            this.rootViews.setDividerPositions(lastRootViewsDivider);
//            this.viewsSwap.setDisable(false);
        } else { // exists, need hide it
            lastSideViewIdx = idx;
            lastRootViewsDivider = this.rootViews.getDividerPositions()[0];
            items.remove(this.sideViews);
//            this.viewsSwap.setDisable(true);
        }
    }

    public BookViewPane addSideView(Node sideView) {
        this.sideViews.getChildren().add(sideView);
        return this;
    }

    public void applyTheme(Theme theme) {
        if (null == theme || theme.stylesheets.isEmpty())
            return;
        this.currentTheme = theme;
        this.webPane.getWebEngine().setUserStyleSheetLocation(theme.stylesheets.iterator().next());
    }

//    protected final Button viewsSwap;
//    private void handleViewsSwapAction(ActionEvent event) {
//        final ObservableList<Node> items = this.viewspane.getItems();
//        final int currWorkviewIdx = items.indexOf(this.workview);
//        lastWorkviewIdx = currWorkviewIdx == 0 ? 1 : 0;
//        lastViewsDivider = 1 - this.viewspane.getDividerPositions()[0];
//
//        items.remove(this.workview);
//        items.add(lastWorkviewIdx, this.workview);
//        this.viewspane.setDividerPositions(lastViewsDivider);
//    }
}
