package org.appxi.cbeta.explorer.prefs;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.appxi.cbeta.explorer.event.DataEvent;
import org.appxi.holder.StringHolder;
import org.appxi.javafx.control.DialogPaneEx;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.theme.Theme;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideToolController;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.ext.HanLang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PreferencesController extends WorkbenchSideToolController {
    public PreferencesController(WorkbenchApplication application) {
        super("PREFERENCES", "设置", application);
    }

    @Override
    public Node createToolIconGraphic(Boolean placeInSideViews) {
        return new MaterialIconView(MaterialIcon.TUNE);
    }

    @Override
    public void onViewportShow(boolean firstTime) {
        final List<Node> nodes = new ArrayList<>();

        buildThemeConfig(nodes);
        buildDisplayHanConfig(nodes);

        final ScrollPane scrollPane = new ScrollPane(new VBox(nodes.toArray(new Node[0])));
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        final DialogPaneEx dialogPane = new DialogPaneEx();
        dialogPane.setStyle("-fx-padding: 1em;");
        dialogPane.setPrefSize(480, 640);
        dialogPane.setContent(scrollPane);

        final Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(viewName);
        alert.setDialogPane(dialogPane);
        FxHelper.withTheme(getApplication(), alert).show();
    }

    private void buildThemeConfig(List<Node> nodes) {
        if (nodes.size() > 0)
            nodes.add(createSeparator());
        //
        nodes.add(new Label("主题/颜色"));

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
        nodes.add(new Label("以 简体/繁体 显示内容"));

        final StringHolder currentLang = new StringHolder(UserPrefs.prefs.getString("display.han", HanLang.hant.lang));
        final ToggleGroup btnGroup = new ToggleGroup();

        Arrays.asList(HanLang.hans, HanLang.hant, HanLang.hantHK, HanLang.hantTW).forEach(t -> {
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
            UserPrefs.prefs.setProperty("display.han", selHan.lang);
            getEventBus().fireEvent(new DataEvent(DataEvent.DISPLAY_HAN));
        }));
    }

    private Separator createSeparator() {
        final Separator result = new Separator();
        result.setStyle("-fx-padding: .5em 0;");
        return result;
    }
}
