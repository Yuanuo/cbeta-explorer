package org.appxi.cbeta.explorer.home;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.event.GenericEvent;
import org.appxi.javafx.settings.DefaultOption;
import org.appxi.javafx.settings.OptionEditorBase;
import org.appxi.javafx.settings.SettingsPane;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.views.WorkbenchSideToolController;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.ext.HanLang;

import java.util.Objects;

public class PreferencesController extends WorkbenchSideToolController {
    public PreferencesController(WorkbenchPane workbench) {
        super("PREFERENCES", workbench);
        this.setTitles("设置");
        this.viewGraphic.set(MaterialIcon.TUNE.graphic());
    }

    @Override
    public void initialize() {
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        SettingsPane settingsPane = new SettingsPane();

        settingsPane.getOptions().add(app.visualProvider.settingOptionForFontSize());
        settingsPane.getOptions().add(app.visualProvider.settingOptionForTheme());
        settingsPane.getOptions().add(app.visualProvider.settingOptionForSwatch());
        settingsPane.getOptions().add(app.visualProvider.settingOptionForWebZoom());

        settingsPane.getOptions().add(new DefaultOption<>("简繁体",
                "以 简体/繁体 显示经名标题、阅读视图等经藏数据", "UI",
                AppContext.getDisplayHan(), true,
                option -> new OptionEditorBase<HanLang, ChoiceBox<HanLang>>(option, new ChoiceBox<>()) {
                    private ObjectProperty<HanLang> valueProperty;

                    @Override
                    public Property<HanLang> valueProperty() {
                        if (this.valueProperty == null) {
                            this.valueProperty = new SimpleObjectProperty<>();
                            this.getEditor().getItems().setAll(HanLang.hans, HanLang.hant, HanLang.hantHK, HanLang.hantTW);
                            this.getEditor().getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
                                if (ov == null || Objects.equals(ov, nv)) return;
                                this.valueProperty.set(nv);
                                //
                                AppContext.setDisplayHan(nv);
                                UserPrefs.prefs.setProperty("display.han", nv.lang);
                                app.eventBus.fireEvent(new GenericEvent(GenericEvent.DISPLAY_HAN_CHANGED, nv));
                            });
                            this.valueProperty.addListener((obs, ov, nv) -> this.setValue(nv));
                        }
                        return this.valueProperty;
                    }

                    @Override
                    public void setValue(HanLang value) {
                        if (getEditor().getItems().isEmpty()) return;
                        getEditor().setValue(value);
                    }
                }));

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
        dialog.setTitle(viewTitle.get());
        dialog.setDialogPane(dialogPane);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.initOwner(app.getPrimaryStage());
        dialog.show();
    }
}
