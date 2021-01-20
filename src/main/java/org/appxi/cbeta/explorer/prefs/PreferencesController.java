package org.appxi.cbeta.explorer.prefs;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.appxi.javafx.control.DialogPaneEx;
import org.appxi.javafx.theme.Theme;
import org.appxi.javafx.workbench.views.WorkbenchWorktoolController;
import org.appxi.prefs.UserPrefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PreferencesController extends WorkbenchWorktoolController {
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
    public Node getViewport() {
        return null;
    }

    @Override
    public void setupInitialize() {
    }

    @Override
    public void onViewportSelected(boolean firstTime) {
        final Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(viewName);

        final List<Node> nodes = new ArrayList<>();

        buildThemeConfig(nodes);

        final DialogPaneEx pane = new DialogPaneEx();
        pane.setStyle("-fx-padding: 1em;");
        pane.setContent(new VBox(nodes.toArray(new Node[0])));
        pane.setPrefSize(300, 400);
        pane.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        pane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        alert.setDialogPane(pane);

        showAlertWithThemeAndWaitForNothing(alert);
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

            final FontAwesomeIconView faicon = new FontAwesomeIconView(FontAwesomeIcon.CIRCLE);
            faicon.setFill(Color.valueOf(t.accentColor));
            btn.setGraphic(faicon);

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

    private Separator createSeparator() {
        final Separator result = new Separator();
        result.setStyle("-fx-padding: .5em 0;");
        return result;
    }
}
