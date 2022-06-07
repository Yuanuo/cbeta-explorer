package org.appxi.cbeta.app.home;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.settings.SettingsList;
import org.appxi.javafx.settings.SettingsPane;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.views.WorkbenchSideToolController;

public class PreferencesController extends WorkbenchSideToolController {
    public PreferencesController(WorkbenchPane workbench) {
        super("PREFERENCES", workbench);
        this.setTitles("设置");
        this.graphic.set(MaterialIcon.TUNE.graphic());
    }

    @Override
    public void initialize() {
        app.visualProvider.addSettings();
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        SettingsPane settingsPane = new SettingsPane();

        SettingsList.get().forEach(s -> settingsPane.getOptions().add(s.get()));

        final DialogPane dialogPane = new DialogPane() {
            @Override
            protected Node createButtonBar() {
                return null;
            }
        };
//        dialogPane.setPrefSize(480, 640);
        dialogPane.setContent(settingsPane);
        dialogPane.getButtonTypes().add(ButtonType.OK);
        //
        Dialog<?> dialog = new Dialog<>();
        dialog.setTitle(title.get());
        dialog.setDialogPane(dialogPane);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.setResizable(true);
        dialog.initOwner(app.getPrimaryStage());
        dialog.setOnShown(evt -> FxHelper.runThread(100, () -> {
            dialog.setHeight(800);
            dialog.setY(dialog.getOwner().getY() + (dialog.getOwner().getHeight() - dialog.getHeight()) / 2);
            if (dialog.getX() < 0) dialog.setX(0);
            if (dialog.getY() < 0) dialog.setY(0);
        }));
        dialog.show();
    }
}
