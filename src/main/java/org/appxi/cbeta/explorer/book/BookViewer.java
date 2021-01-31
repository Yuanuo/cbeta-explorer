package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
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
    public final WebViewer webViewer;

    private Node themeMarker;

    public BookViewer() {
        super();
        this.getStyleClass().add("book-viewer");

        this.toolbar = new AlignedBar();
        this.initToolbar();

        this.webViewer = new WebViewer();
        //
        final BorderPane borderPane = new BorderPane();
        borderPane.setTop(this.toolbar);
        borderPane.setCenter(this.webViewer);
        //
        this.getChildren().add(borderPane);
    }

    protected void initToolbar() {
        initToolbar_FontSize();
        //
        themeMarker = new MaterialIconView(MaterialIcon.STYLE);
        themeMarker.getStyleClass().add("label");
        this.toolbar.addRight(new Separator(Orientation.VERTICAL), themeMarker);
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
        MaterialIconView fSizeIcon = new MaterialIconView(MaterialIcon.TEXT_FIELDS);
        fSizeIcon.getStyleClass().add("label");

        Button fSizeSubBtn = new Button(null, new MaterialIconView(MaterialIcon.ZOOM_OUT));
        fSizeSubBtn.setTooltip(new Tooltip("减小字号"));
        fSizeSubBtn.setOnAction(event -> fSizeAction.accept(false));

        Button fSizeSupBtn = new Button(null, new MaterialIconView(MaterialIcon.ZOOM_IN));
        fSizeSupBtn.setTooltip(new Tooltip("增大字号"));
        fSizeSupBtn.setOnAction(event -> fSizeAction.accept(true));
        this.toolbar.addRight(fSizeIcon, fSizeSubBtn, fSizeSupBtn);
    }

    public void applyThemeSet(ThemeSet themeSet) {
        ObservableList<Node> toolbarItems = this.toolbar.getAlignedItems();
        int themeToolsIdx = toolbarItems.indexOf(this.themeMarker) + 1;

        // clean old
        for (int i = themeToolsIdx; i < toolbarItems.size(); i++) {
            if (toolbarItems.get(i).getProperties().containsKey(themeMarker))
                toolbarItems.remove(i--);
        }
        this.themeMarker.setUserData(null);
        //
        final ToggleGroup themeToolsGroup = new ToggleGroup();
        themeToolsGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (null == nv) return;
            final Theme theme = (Theme) nv.getUserData();
            if (null == theme || Objects.equals(theme, themeMarker.getUserData()) || theme.stylesheets.isEmpty())
                return;
            this.themeMarker.setUserData(theme);
            // TODO 支持使用多个css文件
            this.webViewer.getEngine().setUserStyleSheetLocation(theme.stylesheets.iterator().next());
        });
        final List<RadioButton> themeToolsList = themeSet.themes.stream().map(theme -> {
            final RadioButton themeTool = new RadioButton();
            themeTool.setToggleGroup(themeToolsGroup);
            themeTool.setUserData(theme);
            themeTool.setTooltip(new Tooltip(theme.title));
            themeTool.getProperties().put(this.themeMarker, true);

            final MaterialIconView icon = new MaterialIconView(MaterialIcon.LENS);
            icon.setFill(Color.valueOf(theme.accentColor));
            themeTool.setGraphic(icon);

            return themeTool;
        }).collect(Collectors.toList());
        toolbarItems.addAll(themeToolsIdx, themeToolsList);

        if (!themeToolsList.isEmpty())
            themeToolsList.get(0).fire();
    }
}
