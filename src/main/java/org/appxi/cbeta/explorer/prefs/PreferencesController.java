package org.appxi.cbeta.explorer.prefs;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.paint.Color;
import org.appxi.cbeta.explorer.workbench.WorkbenchOpenpartControllerExt;
import org.appxi.javafx.theme.Theme;
import org.appxi.prefs.UserPrefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PreferencesController extends WorkbenchOpenpartControllerExt {
    private final ToggleGroup configThemeGroup = new ToggleGroup();

    public PreferencesController() {
        super("PREFERENCES", "偏好设置");
    }

    @Override
    public Label getViewpartInfo() {
        final Label info = new Label(this.viewName, new FontAwesomeIconView(FontAwesomeIcon.SLIDERS));
        info.setAlignment(Pos.CENTER_RIGHT);
        return info;
    }

    @Override
    public boolean isWorktoolSupport() {
        return true;
    }

    @Override
    public void setupInitialize() {
    }

    @Override
    public void onViewportSelected(boolean firstTime) {
        if (firstTime) {
            final List<Node> nodes = new ArrayList<>();

            // init ui
            nodes.add(new Label("主题/颜色"));
            final Theme currTheme = this.getThemeProvider().getTheme();
            configThemeGroup.setUserData(currTheme);
            getThemeProvider().getThemes().forEach(t -> {
                final RadioButton btn = new RadioButton(t.title);
                btn.setToggleGroup(configThemeGroup);
                btn.setUserData(t);

                final FontAwesomeIconView faicon = new FontAwesomeIconView(FontAwesomeIcon.CIRCLE);
                faicon.setFill(Color.valueOf(t.accentColor));
                btn.setGraphic(faicon);

                btn.setOnAction(this::handleConfigThemeSelect);
                nodes.add(btn);
                //
                if (Objects.equals(currTheme, t))
                    btn.setSelected(true);
            });

            // update ui
            this.viewpartVbox.getChildren().addAll(nodes);
        }
    }

    @FXML
    void handleConfigThemeSelect(ActionEvent event) {
        final ToggleButton themeBtn = (ToggleButton) event.getSource();
        final Theme currTheme = (Theme) themeBtn.getUserData();
        final Theme lastTheme = (Theme) configThemeGroup.getUserData();
        if (currTheme == lastTheme)
            return;

        configThemeGroup.setUserData(currTheme);
        this.getThemeProvider().applyTheme(currTheme);
        UserPrefs.prefs.setProperty("ui.theme", currTheme.name);
    }
}
