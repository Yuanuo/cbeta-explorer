package org.appxi.cbeta.explorer.home;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.event.GenericEvent;
import org.appxi.cbeta.explorer.search.SearchEngineStart;
import org.appxi.javafx.settings.DefaultOption;
import org.appxi.javafx.settings.DefaultOptions;
import org.appxi.javafx.settings.Option;
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
        this.graphic.set(MaterialIcon.TUNE.graphic());
    }

    @Override
    public void initialize() {
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        SettingsPane settingsPane = new SettingsPane();

        settingsPane.getOptions().add(app.visualProvider.optionForFontSmooth());
        settingsPane.getOptions().add(app.visualProvider.optionForFontName());
        settingsPane.getOptions().add(app.visualProvider.optionForFontSize());
        settingsPane.getOptions().add(app.visualProvider.optionForTheme());
        settingsPane.getOptions().add(app.visualProvider.optionForSwatch());
        settingsPane.getOptions().add(app.visualProvider.optionForWebFontName());
        settingsPane.getOptions().add(app.visualProvider.optionForWebFontSize());
        settingsPane.getOptions().add(app.visualProvider.optionForWebPageColor());
        settingsPane.getOptions().add(app.visualProvider.optionForWebTextColor());

        settingsPane.getOptions().add(optionForDisplayHan());

        settingsPane.getOptions().add(optionForSearchEngineStart());

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
        dialog.initOwner(app.getPrimaryStage());
        dialog.show();
    }

    private Option<HanLang> optionForDisplayHan() {
        final ObjectProperty<HanLang> valueProperty = new SimpleObjectProperty<>(AppContext.getDisplayHan());
        valueProperty.addListener((o, ov, nv) -> {
            if (null == ov || Objects.equals(ov, nv)) return;
            //
            UserPrefs.prefs.setProperty("display.han", nv.lang);
            app.eventBus.fireEvent(new GenericEvent(GenericEvent.DISPLAY_HAN_CHANGED, nv));
        });
        return new DefaultOptions<HanLang>("简繁体", "以 简体/繁体 显示经名标题、阅读视图等经藏数据", "UI", true)
                .setValues(HanLang.hans, HanLang.hant, HanLang.hantHK, HanLang.hantTW)
                .setValueProperty(valueProperty);
    }

    private Option<SearchEngineStart> optionForSearchEngineStart() {
        final ObjectProperty<SearchEngineStart> valueProperty = new SimpleObjectProperty<>(SearchEngineStart.value());
        valueProperty.addListener((o, ov, nv) -> {
            if (null == ov || Objects.equals(ov, nv)) return;
            UserPrefs.prefs.setProperty("search.engine.start", nv.name());
        });
        return new DefaultOption<SearchEngineStart>("搜索引擎初始化时机", null, "性能", true)
                .setValueProperty(valueProperty);
    }
}
