package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
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
    protected final SplitPane viewspane;
    protected final StackPane workview;
    protected final WebPane webPane;
    protected final FlexibleBar flexibleBar;

    private ToggleButton workviewToggle, moretoolToggle;
    private Theme currentTheme;

    public BookViewPane() {
        super();

        this.workview = new StackPane();
        this.workview.getStyleClass().add("workview");

        this.flexibleBar = new FlexibleBar();
        this.flexibleBar.setMaxWidth(Double.NEGATIVE_INFINITY);
        this.flexibleBar.setMaxHeight(Double.NEGATIVE_INFINITY);

        this.webPane = new WebPane();
        this.webPane.getChildren().add(this.flexibleBar);

        this.viewspane = new SplitPane(webPane);
        this.viewspane.getStyleClass().add("viewspane");
        this.viewspane.setDividerPositions(0.2);
        VBox.setVgrow(this.viewspane, Priority.ALWAYS);
        //
        this.getChildren().add(this.viewspane);
        this.getStyleClass().add("bookview-pane");
        //
        this.initMoreThings();
    }

    protected void initMoreThings() {
        //
        workviewToggle = new ToggleButton();
        workviewToggle.getStyleClass().add("workview-toggle");
        workviewToggle.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.NAVICON));
        workviewToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        workviewToggle.setOnAction(this::handleWorkviewToggleAction);
        this.flexibleBar.addTool(workviewToggle);
        //
        this.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE)
                workviewToggle.fire();
        });
        //
//        this.viewsSwap = new Button("Swap");
//        this.viewsSwap.getStyleClass().add("views-swap");
//        this.viewsSwap.setOnAction(this::handleViewsSwapAction);
//        this.viewsSwap.setDisable(true);, this.viewsSwap
        //
        moretoolToggle = new ToggleButton();
        moretoolToggle.getStyleClass().add("moretool-toggle");
        moretoolToggle.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.EXCHANGE));
        moretoolToggle.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        this.flexibleBar.addTools(moretoolToggle);
        updateFontsizeTools();
    }

    public void updateThemeTools(ThemeSet themeSet) {
        final List<Node> moretools = this.flexibleBar.getTools(this.moretoolToggle);
        int themeToolsIdx = 0;
        List<Node> themeTools = (List<Node>) this.moretoolToggle.getProperties().get("THEME_TOOLS");
        if (null == themeTools)
            this.moretoolToggle.getProperties().put("THEME_TOOLS", themeTools = new ArrayList<>());

        if (!themeTools.isEmpty()) {
            themeToolsIdx = moretools.indexOf(themeTools.get(0));
            moretools.removeAll(themeTools);
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

                final FontAwesomeIconView faicon = new FontAwesomeIconView(FontAwesomeIcon.SQUARE);
                faicon.setFill(Color.valueOf(theme.accentColor));
                btn.setGraphic(faicon);

                btn.setOnAction(event -> {
                    final ToggleButton eventBtn = (ToggleButton) event.getSource();
                    this.applyTheme((Theme) eventBtn.getUserData());
                });
                themeTools.add(btn);
            }
            moretools.addAll(themeToolsIdx, themeTools);
        }
        this.flexibleBar.setTools(this.moretoolToggle, moretools.toArray(new Node[0]));
    }

    protected void updateFontsizeTools() {
        final List<Node> moretools = this.flexibleBar.getTools(this.moretoolToggle);
        int fsizeToolsIdx = 0;
        List<Node> fsizeTools = (List<Node>) this.moretoolToggle.getProperties().get("FSIZE_TOOLS");
        if (null == fsizeTools)
            this.moretoolToggle.getProperties().put("FSIZE_TOOLS", fsizeTools = new ArrayList<>());

        if (!fsizeTools.isEmpty()) {
            fsizeToolsIdx = moretools.indexOf(fsizeTools.get(0));
            moretools.removeAll(fsizeTools);
            fsizeTools.clear();
        }

        fsizeTools.add(new Label("FontSize: ", new FontAwesomeIconView(FontAwesomeIcon.FONT)));
        //
        final Button subBtn = new Button();
        subBtn.setUserData(false);
        subBtn.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.SUBSCRIPT));
        subBtn.setTooltip(new Tooltip("减小字号"));
        subBtn.setOnAction(this::handleFontSizeAction);
        fsizeTools.add(subBtn);
        //
        final Button supBtn = new Button();
        supBtn.setUserData(true);
        supBtn.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.SUPERSCRIPT));
        supBtn.setTooltip(new Tooltip("增大字号"));
        supBtn.setOnAction(this::handleFontSizeAction);
        fsizeTools.add(supBtn);
        //
        moretools.addAll(fsizeToolsIdx, fsizeTools);
        this.flexibleBar.setTools(this.moretoolToggle, moretools.toArray(new Node[0]));
    }

    private void handleFontSizeAction(ActionEvent event) {
        final Button btn = (Button) event.getSource();
        final boolean supAct = (boolean) btn.getUserData();
    }

    private int lastWorkviewIdx = 0;
    private double lastViewsDivider = 0.2;

    private void handleWorkviewToggleAction(ActionEvent event) {
        final ToggleButton btn = (ToggleButton) event.getSource();
        final ObservableList<Node> items = this.viewspane.getItems();
        final int idx = items.indexOf(this.workview);
        if (idx == -1) { // not exists, need show it
            items.add(lastWorkviewIdx, this.workview);
            this.viewspane.setDividerPositions(lastViewsDivider);
//            this.viewsSwap.setDisable(false);
        } else { // exists, need hide it
            lastWorkviewIdx = idx;
            lastViewsDivider = this.viewspane.getDividerPositions()[0];
            items.remove(this.workview);
//            this.viewsSwap.setDisable(true);
        }
    }

    public BookViewPane addWorkview(Node workview) {
        this.workview.getChildren().add(workview);
        return this;
    }

    public void applyTheme(Theme theme) {
        this.currentTheme = theme;
        this.webPane.getWebEngine().setUserStyleSheetLocation(theme.stylesheets.get(0));
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
