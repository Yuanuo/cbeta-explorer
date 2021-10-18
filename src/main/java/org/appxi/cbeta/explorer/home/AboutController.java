package org.appxi.cbeta.explorer.home;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.appxi.cbeta.explorer.AppInfo;
import org.appxi.javafx.control.DialogPaneEx;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.iconfont.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideToolController;
import org.appxi.prefs.UserPrefs;

public class AboutController extends WorkbenchSideToolController {
    public AboutController(WorkbenchApplication application) {
        super("ABOUT", application);
        this.setTitles("关于");
        this.viewIcon.set(MaterialIcon.INFO_OUTLINE.iconView());
    }

    @Override
    public void setupInitialize() {
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        final Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(viewTitle.get());

        final ImageView graphic = new ImageView(
                getClass().getResource("/appxi/cbetaExplorer/icons/icon-256.png").toExternalForm());
        graphic.setFitWidth(48);
        graphic.setFitHeight(48);
        graphic.setSmooth(true);
        final Label head = new Label(AppInfo.NAME, graphic);
        head.setStyle("-fx-font-size: 2em; -fx-padding: .5em 0;");
        head.setMaxWidth(Double.MAX_VALUE);
        head.setAlignment(Pos.CENTER);
        head.setGraphicTextGap(20);
        final HBox headBox = new HBox(head);
        HBox.setHgrow(head, Priority.ALWAYS);

        final Label desc = new Label("CBETA经藏阅读器");
        desc.setStyle("-fx-font-size: 1.5em; -fx-padding: .5em 0 2em 0;");
        desc.setMaxWidth(Double.MAX_VALUE);
        desc.setAlignment(Pos.CENTER);
        final HBox descBox = new HBox(desc);
        HBox.setHgrow(desc, Priority.ALWAYS);

        final TextArea info = new TextArea();
        VBox.setVgrow(info, Priority.ALWAYS);
        info.setWrapText(true);
        info.setEditable(false);

        final StringBuilder buf = new StringBuilder();
        buf.append("Product Version").append("\n");
        buf.append(AppInfo.NAME).append(" ").append(AppInfo.VERSION).append("\n\n");

        buf.append("Java Version").append("\n");
        buf.append(System.getProperty("java.runtime.version")).append("\n\n");

        buf.append("JavaFX Version").append("\n");
        buf.append(System.getProperty("javafx.runtime.version")).append("\n\n");

        buf.append("OS Info").append("\n");
        buf.append(System.getProperty("os.name")).append(", ");
        buf.append(System.getProperty("os.arch")).append(", ");
        buf.append(System.getProperty("os.version")).append("\n\n");

        WebView web = new WebView();
        web.getEngine().setUserDataDirectory(UserPrefs.cacheDir().toFile());

        buf.append("Embedded Webkit Info").append("\n");
        buf.append(web.getEngine().getUserAgent());

        info.setText(buf.toString());

        final DialogPaneEx pane = new DialogPaneEx();
        pane.setContent(new VBox(headBox, descBox, info));
        alert.setDialogPane(pane);

        FxHelper.withTheme(getApplication(), alert).show();
    }
}
