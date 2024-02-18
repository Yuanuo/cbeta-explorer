package org.appxi.cbeta.app.home;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import org.appxi.cbeta.BookcaseInZip;
import org.appxi.cbeta.app.AppLauncher;
import org.appxi.cbeta.app.DataApp;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.settings.DefaultOption;
import org.appxi.javafx.settings.OptionEditorBase;
import org.appxi.javafx.settings.SettingsPane;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPart;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.prefs.Preferences;

import java.io.File;

public class PreferencesController extends WorkbenchPartController implements WorkbenchPart.SideTool {
    final DataApp dataApp;

    public PreferencesController(WorkbenchPane workbench, DataApp dataApp) {
        super(workbench);
        this.dataApp = dataApp;

        this.id.set("PREFERENCES");
        this.title.set("设置");
        this.tooltip.set("设置");
        this.graphic.set(MaterialIcon.TUNE.graphic());
    }

    @Override
    public void postConstruct() {
        app.settings.add(() -> {
            final Preferences appConfig = AppLauncher.appConfig;
            final String bookcase = appConfig.getString("bookcase", "");
            return new DefaultOption<String>("切换数据源", bookcase, "***数据源", true,
                    option -> new OptionEditorBase<>(option, new Button()) {
                        private StringProperty valueProperty;

                        @Override
                        public StringProperty valueProperty() {
                            if (this.valueProperty == null) {
                                this.valueProperty = new SimpleStringProperty();
                                getEditor().setText("选择数据包");
                                getEditor().setOnAction(evt -> {
                                    final FileChooser chooser = new FileChooser();
                                    chooser.setTitle("请选择CBETA Bookcase Zip数据包");
                                    chooser.getExtensionFilters().add(
                                            new FileChooser.ExtensionFilter("CBETA Bookcase Zip File", "cbeta.zip", "bookcase.zip", "bookcase_*.zip")
                                    );
                                    final File selected = chooser.showOpenDialog(app.getPrimaryStage());
                                    if (null == selected) {
                                        return;
                                    }
                                    try {
                                        String path = selected.getAbsolutePath();
                                        // 验证数据源有效性
                                        new BookcaseInZip(path);
                                        // 数据源有效，更新记录
                                        appConfig.setProperty("bookcase", path);
                                        appConfig.save();
                                        setValue(path);

                                        final Alert alert = new Alert(Alert.AlertType.INFORMATION, "已切换数据源，下次启动时生效！");
                                        alert.setHeaderText(null);
                                        alert.initOwner(app.getPrimaryStage());
                                        alert.show();
                                    } catch (Throwable ignore) {
                                        app.toastError("验证失败，请选择有效的数据源！");
                                    }
                                });
                            }
                            return this.valueProperty;
                        }

                        @Override
                        public void setValue(String value) {
                            getEditor().setTooltip(new Tooltip("当前数据源：" + value));
                        }
                    }).setValue(bookcase);
        });
    }

    @Override
    public void activeViewport(boolean firstTime) {
        SettingsPane settingsPane = new SettingsPane();

        dataApp.baseApp.settings.forEach(s -> settingsPane.getOptions().add(s.get()));
        app.settings.forEach(s -> settingsPane.getOptions().add(s.get()));

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
