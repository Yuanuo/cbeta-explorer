package org.appxi.cbeta.explorer.book;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.appxi.javafx.control.AlignedBar;
import org.appxi.javafx.control.StackPaneEx;
import org.appxi.javafx.control.TabPaneExt;
import org.appxi.javafx.theme.Theme;
import org.appxi.javafx.theme.ThemeSet;
import org.appxi.tome.model.Book;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class BookViewer extends StackPaneEx {
    protected final BookViewController controller;
    protected final Book book;

    public final TabPane sideViews;
    public final BookBasicView bookBasicView;

    public final WebViewerEx webViewer;

    protected final AlignedBar toolbar;
    private Node themeMarker;

    public BookViewer(BookViewController controller) {
        super();
        this.getStyleClass().add("book-viewer");

        this.controller = controller;
        this.book = controller.book;

        this.toolbar = new AlignedBar();
        this.initToolbar();

        this.webViewer = new WebViewerEx();
        //
        final BorderPane borderPane = new BorderPane();
        borderPane.setTop(this.toolbar);
        borderPane.setCenter(this.webViewer);
        //
        this.getChildren().add(borderPane);

        //
        this.sideViews = new TabPaneExt();
        this.sideViews.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(this.sideViews, Priority.ALWAYS);
        //
        final Tab tab1 = new Tab("基本", this.bookBasicView = new BookBasicView());
        //
        this.sideViews.getTabs().addAll(tab1);
    }

    protected void initToolbar() {
        addTool_SideControl();
        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
        addTool_FontSize();
        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
        addTool_Themes();
        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
        addTool_WrapLines();
        addTool_FirstLetterIndent();
        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
        addTool_EditingMark();
        //
        this.toolbar.addRight(new Separator(Orientation.VERTICAL));
        addTool_Bookmark();
        addTool_Favorite();
        addTool_SearchInPage();
    }

    private void addTool_SideControl() {
        final Button button = new Button();
        button.setGraphic(new MaterialIconView(MaterialIcon.IMPORT_CONTACTS));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("显示本书相关数据（目录、书签等）"));
        button.setOnAction(event -> this.controller.getPrimaryViewport().selectSideTool(
                BookDataController.getInstance().viewId));
        this.toolbar.addLeft(button);
    }

    private void addTool_FontSize() {
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

        Button fSizeSubBtn = new Button(null, new MaterialIconView(MaterialIcon.ZOOM_OUT));
        fSizeSubBtn.setTooltip(new Tooltip("减小字号"));
        fSizeSubBtn.setOnAction(event -> fSizeAction.accept(false));

        Button fSizeSupBtn = new Button(null, new MaterialIconView(MaterialIcon.ZOOM_IN));
        fSizeSupBtn.setTooltip(new Tooltip("增大字号"));
        fSizeSupBtn.setOnAction(event -> fSizeAction.accept(true));
        this.toolbar.addRight(fSizeSubBtn, fSizeSupBtn);
    }

    private void addTool_Themes() {
        themeMarker = new MaterialIconView(MaterialIcon.PALETTE);
        themeMarker.getStyleClass().add("label");
        this.toolbar.addRight(themeMarker);
    }

    private void addTool_WrapLines() {
        final ToggleButton button = new ToggleButton();
        button.setGraphic(new MaterialIconView(MaterialIcon.LINE_STYLE));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("折行显示"));
        button.setOnAction(event -> webViewer.executeScript("handleOnWrapLines()"));
        this.toolbar.addRight(button);
    }

    private void addTool_FirstLetterIndent() {
        final ToggleButton button = new ToggleButton();
        button.setGraphic(new MaterialIconView(MaterialIcon.FORMAT_INDENT_INCREASE));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("首行标点对齐"));
        button.setOnAction(event -> webViewer.executeScript("handleOnFirstLetterIndent()"));
        this.toolbar.addRight(button);
    }

    private void addTool_EditingMark() {
        final ToggleGroup marksGroup = new ToggleGroup();

        final ToggleButton markClean = new ToggleButton("无编注");
        markClean.setTooltip(new Tooltip("不显示编注内容"));
        markClean.setUserData(-1);

        final ToggleButton markOrigInline = new ToggleButton("原始");
        markOrigInline.setTooltip(new Tooltip("在编注点嵌入原始编注内容"));
        markOrigInline.setUserData(0);

        final ToggleButton markModSharp = new ToggleButton("标记");
        markModSharp.setTooltip(new Tooltip("在编注点插入#号并划动鼠标查看编注内容"));
        markModSharp.setUserData(1);

        final ToggleButton markModColor = new ToggleButton("着色");
        markModColor.setTooltip(new Tooltip("在编注点着色被改变的文字并划动鼠标查看"));
        markModColor.setUserData(2);

        final ToggleButton markModPopover = new ToggleButton("着色（多行）");
        markModPopover.setTooltip(new Tooltip("在编注点着色被改变的文字并划动鼠标查看"));
        markModPopover.setUserData(3);
        markModPopover.setDisable(true); // TODO impl js

        final ToggleButton markModInline = new ToggleButton("嵌入");
        markModInline.setTooltip(new Tooltip("在编注点直接嵌入显示编注内容"));
        markModInline.setUserData(4);

        marksGroup.getToggles().setAll(markClean, markOrigInline, markModSharp, markModColor, markModPopover, markModInline);
        marksGroup.selectToggle(markClean);
        marksGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            if (null != nv)
                webViewer.executeScript("handleOnEditMark(" + nv.getUserData() + ")");
        });

        this.toolbar.addRight(markClean, markOrigInline, markModSharp, markModColor, markModPopover, markModInline);
    }


    private void addTool_Bookmark() {
    }

    private void addTool_Favorite() {
    }

    private void addTool_SearchInPage() {
        final Button button = new Button();
        button.setGraphic(new MaterialIconView(MaterialIcon.SEARCH));
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setTooltip(new Tooltip("查找"));
        button.setOnAction(event -> webViewer.executeScript("handleOnSearchInPage()"));
        this.toolbar.addRight(button);
    }


    /* //////////////////////////////////////////////////////////////////// */

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
