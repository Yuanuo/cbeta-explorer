package org.appxi.cbeta.explorer.dict;

import appxi.dict.Dictionary;
import appxi.dict.DictionaryApi;
import appxi.dict.SearchMode;
import appxi.dict.SearchResultEntry;
import appxi.dict.doc.DictEntry;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.appxi.cbeta.explorer.App;
import org.appxi.javafx.app.AppEvent;
import org.appxi.javafx.app.DesktopApp;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.views.WorkbenchSideToolController;

import java.nio.file.Path;
import java.util.Iterator;

public class DictionaryController extends WorkbenchSideToolController {
    public DictionaryController(WorkbenchPane workbench) {
        super("DICTIONARY", workbench);
        this.setTitles("查词典", "查词典 (Ctrl+D)");
        this.attr(Pos.class, Pos.CENTER_LEFT);
        this.graphic.set(MaterialIcon.TRANSLATE.graphic());
    }

    @Override
    public void initialize() {
        app.eventBus.addEventHandler(AppEvent.STARTED, event -> {
            final Path dictRepo;
            if (DesktopApp.productionMode) {
                dictRepo = DesktopApp.appDir().resolve("template/dict");
            } else {
                dictRepo = Path.of("../appxi-dictionary/repo");
            }
            DictionaryApi.setupDefaultApi(dictRepo);
        });

        app.getPrimaryScene().getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN),
                () -> this.onViewportShowing(false));

        app.eventBus.addEventHandler(DictionaryEvent.SEARCH,
                event -> this.onViewportShowing(null != event.text ? event.text.strip() : null));

        app.eventBus.addEventHandler(DictionaryEvent.SEARCH_EXACT, event -> {
            Dictionary dictionary = DictionaryApi.api().get(event.dictionary);
            Iterator<SearchResultEntry> iterator = dictionary.entries.search(SearchMode.TitleEquals, event.text, null);
            if (iterator.hasNext()) {
                showDictEntryViewer(iterator.next().dictEntry);
            }
        });
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        onViewportShowing(null);
    }

    private DictionaryLookupLayer lookupLayer;

    private void onViewportShowing(String text) {
        if (null == lookupLayer) {
            lookupLayer = new DictionaryLookupLayer(this);
        }
        lookupLayer.show(text != null ? text : lookupLayer.inputQuery);
    }

    void showDictEntryViewer(DictEntry item) {
        final String windowId = item.dictionary.id + " /" + item.title;

        //
        Window window = Window.getWindows().stream().filter(w -> windowId.equals(w.getScene().getUserData())).findFirst().orElse(null);
        if (null != window) {
            window.requestFocus();
            return;
        }
        //
        final DictionaryViewer dictViewer = new DictionaryViewer(app, item);
        //
        final DialogPane dialogPane = new DialogPane() {
            @Override
            protected Node createButtonBar() {
                return null;
            }
        };
        dialogPane.setContent(dictViewer);
        dialogPane.getButtonTypes().add(ButtonType.OK);
        //
        Dialog<?> dialog = new Dialog<>();
        dialog.setTitle(item.title + " -- " + item.dictionary.getName() + "  -  " + App.NAME);
        dialog.setDialogPane(dialogPane);
        dialog.getDialogPane().setPrefWidth(800);
        dialog.setResizable(true);
        dialog.initModality(Modality.NONE);
        dialog.initOwner(app.getPrimaryStage());
        dialog.getDialogPane().getScene().setUserData(windowId);
        dialog.setOnShown(evt -> FxHelper.runThread(100, () -> {
            dialog.setHeight(600);
            dialog.setY(dialog.getOwner().getY() + (dialog.getOwner().getHeight() - dialog.getHeight()) / 2);
            if (dialog.getX() < 0) dialog.setX(0);
            if (dialog.getY() < 0) dialog.setY(0);
            //
            dictViewer.onViewportShowing();
        }));
        dialog.setOnHidden(evt -> {
            dictViewer.onViewportClosing();
        });
        dialog.show();
    }
}
