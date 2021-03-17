package org.appxi.cbeta.explorer.prefs;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.appxi.cbeta.explorer.DisplayHelper;
import org.appxi.cbeta.explorer.event.GenericEvent;
import org.appxi.holder.RawHolder;
import org.appxi.holder.StringHolder;
import org.appxi.javafx.control.DialogPaneEx;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.theme.Theme;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideToolController;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.ext.HanLang;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class PreferencesController extends WorkbenchSideToolController {
    public PreferencesController(WorkbenchApplication application) {
        super("PREFERENCES", application);
        this.setTitles("设置");
        this.viewIcon.set(new MaterialIconView(MaterialIcon.TUNE));
    }

    @Override
    public void setupInitialize() {
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        final List<Node> nodes = new ArrayList<>();

        buildThemeConfig(nodes);
        buildDisplayHanConfig(nodes);
        buildDisplayZoomConfig(nodes);

        final ScrollPane scrollPane = new ScrollPane(new VBox(nodes.toArray(new Node[0])));
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        final DialogPaneEx dialogPane = new DialogPaneEx();
        dialogPane.getStyleClass().add("prefs-dialog-pane");
        dialogPane.setStyle(dialogPane.getStyle().concat("-fx-padding: 1em;"));
        dialogPane.setPrefSize(480, 640);
        dialogPane.setContent(scrollPane);

        final Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(viewTitle.get());
        alert.setDialogPane(dialogPane);
        FxHelper.withTheme(getApplication(), alert).show();
    }

    private void buildThemeConfig(List<Node> nodes) {
        if (nodes.size() > 0)
            nodes.add(createSeparator());
        //
        nodes.add(createGroupLabel("主题/颜色"));

        final Theme currentTheme = this.getThemeProvider().getTheme();
        final ToggleGroup themeBtnGroup = new ToggleGroup();
        getThemeProvider().getThemes().forEach(t -> {
            final RadioButton btn = new RadioButton(t.title);
            btn.setToggleGroup(themeBtnGroup);
            btn.setUserData(t);

            final MaterialIconView icon = new MaterialIconView(MaterialIcon.LENS, "2em");
            icon.setFill(Color.valueOf(t.accentColor));
            btn.setGraphic(icon);

            btn.setStyle("-fx-padding: .5em 1em;");

            nodes.add(btn);
            //
            if (Objects.equals(currentTheme, t))
                btn.setSelected(true);
        });
        themeBtnGroup.selectedToggleProperty().addListener(((val, ov, nv) -> {
            if (ov == nv)
                return;
            final Theme selTheme = (Theme) nv.getUserData();
            this.getThemeProvider().applyTheme(selTheme);
            UserPrefs.prefs.setProperty("ui.theme", selTheme.name);
        }));
    }

    private void buildDisplayHanConfig(List<Node> nodes) {
        if (nodes.size() > 0)
            nodes.add(createSeparator());
        //
        nodes.add(createGroupLabel("以 简体/繁体 显示经名标题、阅读视图等经藏数据"));

        final StringHolder currentLang = new StringHolder(DisplayHelper.getDisplayHan().lang);
        final ToggleGroup btnGroup = new ToggleGroup();

        Arrays.asList(HanLang.hans, HanLang.hantTW).forEach(t -> {
            final RadioButton btn = new RadioButton(t.text);
            btn.setToggleGroup(btnGroup);
            btn.setUserData(t);
            btn.setStyle("-fx-padding: .5em 1em;");

            nodes.add(btn);
            //
            if (Objects.equals(currentLang.value, t.lang))
                btn.setSelected(true);
        });
        btnGroup.selectedToggleProperty().addListener(((val, ov, nv) -> {
            if (ov == nv)
                return;
            HanLang selHan = (HanLang) nv.getUserData();
            if (currentLang.value.equals(selHan.lang))
                return;
            currentLang.value = selHan.lang;
            DisplayHelper.setDisplayHan(selHan);
            UserPrefs.prefs.setProperty("display.han", selHan.lang);
            getEventBus().fireEvent(new GenericEvent(GenericEvent.DISPLAY_HAN_CHANGED, selHan));
        }));
    }

    private void buildDisplayZoomConfig(List<Node> nodes) {
        if (nodes.size() > 0)
            nodes.add(createSeparator());
        //
        nodes.add(createGroupLabel("阅读视图默认字号缩放级别"));

        final ChoiceBox<Double> zoomLevels = new ChoiceBox<>();
        zoomLevels.getItems().setAll(
                DoubleStream.iterate(1.5, v -> v <= 3.0,
                        v -> new BigDecimal(v + .1).setScale(1, RoundingMode.HALF_UP).doubleValue())
                        .boxed().collect(Collectors.toList())
        );
        nodes.add(zoomLevels);

        final RawHolder<Double> currentZoom = new RawHolder<>(DisplayHelper.getDisplayZoom());
        if (!zoomLevels.getItems().contains(currentZoom.value))
            currentZoom.value = 1.6;
        zoomLevels.setValue(currentZoom.value);

        zoomLevels.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> {
            if (currentZoom.value.doubleValue() == nv)
                return;
            currentZoom.value = nv;
            UserPrefs.prefs.setProperty("display.zoom", nv);
            getEventBus().fireEvent(new GenericEvent(GenericEvent.DISPLAY_ZOOM_CHANGED, nv));
        });
    }

    private Separator createSeparator() {
        final Separator result = new Separator();
        result.getStyleClass().add("group-separator");
        result.setStyle(result.getStyle().concat("-fx-padding: 1em 0;"));
        return result;
    }

    private Node createGroupLabel(String text) {
        final Label result = new Label(text);
        result.getStyleClass().add("group-label");
        return result;
    }
}
