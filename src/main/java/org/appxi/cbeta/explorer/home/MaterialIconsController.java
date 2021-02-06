package org.appxi.cbeta.explorer.home;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import org.appxi.javafx.control.DialogPaneEx;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideToolController;

public class MaterialIconsController extends WorkbenchSideToolController {
    public MaterialIconsController(WorkbenchApplication application) {
        super("MaterialIcons", "MaterialIcons", application);
    }

    @Override
    public Node createToolIconGraphic(Boolean placeInSideViews) {
        return new MaterialIconView(MaterialIcon.WALLPAPER);
    }

    @Override
    public void onViewportShow(boolean firstTime) {
        final FlowPane iconsPane = new FlowPane(5, 5);
        for (MaterialIcon icon : MaterialIcon.values()) {
            Label label = new Label(null, new MaterialIconView(icon, "3em"));
            label.setTooltip(new Tooltip(icon.name()));
            label.setContentDisplay(ContentDisplay.TOP);
            iconsPane.getChildren().add(label);
        }

        final ScrollPane scrollPane = new ScrollPane(iconsPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        final DialogPaneEx dialogPane = new DialogPaneEx();
        dialogPane.setStyle("-fx-padding: 1em;");
        dialogPane.setPrefSize(600, 800);
        dialogPane.setContent(scrollPane);

        final Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(MaterialIcon.values().length + " Icons");
        alert.setDialogPane(dialogPane);
        showAlertWithThemeAndWaitForNothing(alert);
    }
}
