package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import org.appxi.holder.IntHolder;
import org.appxi.holder.NumHolder;
import org.appxi.javafx.control.AlignedBar;
import org.appxi.javafx.control.StackPaneEx;
import org.appxi.javafx.control.WebViewer;
import org.appxi.javafx.theme.Theme;
import org.appxi.javafx.theme.ThemeSet;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BookViewer extends StackPaneEx {
    protected final AlignedBar toolbar;
    protected final SplitPane rootView;
    public final Accordion accordion;
    public final WebViewer webViewer;

    private Label themeLabel;

    public BookViewer() {
        super();
        this.getStyleClass().add("book-viewer");

        this.toolbar = new AlignedBar();
        this.initToolbar();

        this.accordion = new Accordion();

        this.webViewer = new WebViewer();

        this.rootView = new SplitPane(webViewer);
        this.rootView.setDividerPositions(0.2);
        //
        final BorderPane borderPane = new BorderPane();
        borderPane.setTop(this.toolbar);
        borderPane.setCenter(this.rootView);
        //
        this.getChildren().add(borderPane);
    }

    private void initToolbar() {
        initToolbar_SideControl();
        initToolbar_FontSize();
        //
        themeLabel = new Label("样式:");
        this.toolbar.addRight(new Separator(Orientation.VERTICAL), themeLabel);
    }

    private void initToolbar_SideControl() {
        final ToggleButton sideControlBtn = new ToggleButton();
        sideControlBtn.setGraphic(new MaterialIconView(MaterialIcon.MENU));
        sideControlBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        final IntHolder lastSideViewIdx = new IntHolder(0);
        final NumHolder lastRootViewsDivider = new NumHolder(0.2);
        sideControlBtn.setOnAction(event -> {
            final ObservableList<Node> items = this.rootView.getItems();
            final int idx = items.indexOf(this.accordion);
            if (idx == -1) {
                items.add(lastSideViewIdx.value, this.accordion);
                this.rootView.setDividerPositions(lastRootViewsDivider.value.doubleValue());
            } else {
                lastSideViewIdx.value = idx;
                lastRootViewsDivider.value = this.rootView.getDividerPositions()[0];
                items.remove(this.accordion);
            }
        });
        this.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE)
                sideControlBtn.fire();
        });
        this.toolbar.addLeft(sideControlBtn);
    }

    private void initToolbar_FontSize() {
        //TODO 是否使用全局默认值，并可保存为默认？
        final Consumer<Boolean> fSizeAction = state -> {
            if (null == state) {// reset ?
                webViewer.getViewer().setFontScale(1.0);
            } else if (state) { //
                webViewer.getViewer().setFontScale(webViewer.getViewer().getFontScale() + 0.1);
            } else {
                webViewer.getViewer().setFontScale(webViewer.getViewer().getFontScale() - 0.1);
            }
        };
        Label fSizeLabel = new Label("字号:");

        Button fSizeSubBtn = new Button(null, new MaterialIconView(MaterialIcon.ZOOM_OUT));
        fSizeSubBtn.setTooltip(new Tooltip("减小字号"));
        fSizeSubBtn.setOnAction(event -> fSizeAction.accept(false));

        Button fSizeSupBtn = new Button(null, new MaterialIconView(MaterialIcon.ZOOM_IN));
        fSizeSupBtn.setTooltip(new Tooltip("增大字号"));
        fSizeSupBtn.setOnAction(event -> fSizeAction.accept(true));
        this.toolbar.addRight(fSizeLabel, fSizeSubBtn, fSizeSupBtn);
    }

    public void applyThemeSet(ThemeSet themeSet) {
        ObservableList<Node> toolbarItems = this.toolbar.getAlignedItems();
        int themeToolsIdx = toolbarItems.indexOf(this.themeLabel) + 1;

        // clean old
        for (int i = themeToolsIdx; i < toolbarItems.size(); i++) {
            if (toolbarItems.get(i).getProperties().containsKey(themeLabel))
                toolbarItems.remove(i--);
        }
        this.themeLabel.setUserData(null);
        //
        final ToggleGroup themeToolsGroup = new ToggleGroup();
        final List<RadioButton> themeToolsList = themeSet.themes.stream().map(theme -> {
            final RadioButton themeTool = new RadioButton();
            themeTool.setToggleGroup(themeToolsGroup);
            themeTool.setUserData(theme);
            themeTool.setTooltip(new Tooltip(theme.title));
            themeTool.getProperties().put(this.themeLabel, true);

            final MaterialIconView icon = new MaterialIconView(MaterialIcon.LENS);
            icon.setFill(Color.valueOf(theme.accentColor));
            themeTool.setGraphic(icon);

            themeTool.setOnAction(event -> {
                final Theme tgtTheme = (Theme) ((ToggleButton) event.getSource()).getUserData();
                if (null == tgtTheme || Objects.equals(tgtTheme, themeLabel.getUserData()) || tgtTheme.stylesheets.isEmpty())
                    return;
                this.themeLabel.setUserData(tgtTheme);
                // TODO 支持使用多个css文件
                this.webViewer.getEngine().setUserStyleSheetLocation(tgtTheme.stylesheets.iterator().next());
            });
            return themeTool;
        }).collect(Collectors.toList());
        toolbarItems.addAll(themeToolsIdx, themeToolsList);

        if (!themeToolsList.isEmpty())
            themeToolsList.get(0).fire();
    }
}
