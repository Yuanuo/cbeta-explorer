package org.appxi.cbeta.app;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.FlowPane;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPart;
import org.appxi.javafx.workbench.WorkbenchPartController;

import java.util.List;

public class AppLauncherDev extends AppLauncher {
    public static void main(String[] args) {
        try {
            beforeLaunch(".".concat(App.ID).concat(".dev"));
            Application.launch(AppDev.class, args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static class AppDev extends App {
        @Override
        protected List<WorkbenchPart> createWorkbenchParts(WorkbenchPane workbench) {
            final List<WorkbenchPart> result = super.createWorkbenchParts(workbench);
            result.add(new MaterialIcons(workbench));
            result.add(new ScenicView(workbench));
            return result;
        }
    }

    static class ScenicView extends WorkbenchPartController implements WorkbenchPart.SideTool {
        public ScenicView(WorkbenchPane workbench) {
            super(workbench);

            this.id.set("ScenicView");
            this.tooltip.set("ScenicView");
            this.graphic.set(MaterialIcon.ACCESSIBILITY.graphic());
        }

        @Override
        public void initialize() {
            app.getPrimaryScene().getAccelerators().put(new KeyCodeCombination(KeyCode.F12), () -> activeViewport(true));
        }

        @Override
        public void activeViewport(boolean firstTime) {
            app.toastError("disabled sceneview.jar");
            //            javafx.application.Platform.runLater(() -> org.scenicview.ScenicView.show(app.getPrimaryScene()));
        }
    }

    static class MaterialIcons extends WorkbenchPartController implements WorkbenchPart.SideTool {
        public MaterialIcons(WorkbenchPane application) {
            super(application);
            this.id.set("MaterialIcons");
            this.tooltip.set("MaterialIcons");
            this.graphic.set(MaterialIcon.PHOTO_LIBRARY.graphic());
        }

        @Override
        public void initialize() {
        }

        @Override
        public void activeViewport(boolean firstTime) {
            final FlowPane iconsPane = new FlowPane(5, 5);
            for (MaterialIcon icon : MaterialIcon.values()) {
                Label label = new Label(null, icon.graphic("-fx-font-size:3em;"));
                label.getGraphic().getStyleClass().add("dev");
                label.setTooltip(new Tooltip(icon.name()));
                label.setContentDisplay(ContentDisplay.TOP);
                iconsPane.getChildren().add(label);
            }

            final ScrollPane scrollPane = new ScrollPane(iconsPane);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);

            final DialogPane dialogPane = new DialogPane() {
                @Override
                protected Node createButtonBar() {
                    return null;
                }
            };
            dialogPane.setStyle("-fx-padding: 1em;");
            dialogPane.setPrefSize(1280, 800);
            dialogPane.setContent(scrollPane);
            dialogPane.getButtonTypes().add(ButtonType.OK);

            final Alert alert = new Alert(Alert.AlertType.NONE);
            alert.setTitle("MaterialIcons Total " + MaterialIcon.values().length + " Icons");
            alert.setDialogPane(dialogPane);
            alert.initOwner(app.getPrimaryStage());
            alert.show();
        }
    }
}