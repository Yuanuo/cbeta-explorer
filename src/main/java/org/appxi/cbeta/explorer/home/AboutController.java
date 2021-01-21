package org.appxi.cbeta.explorer.home;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.appxi.cbeta.explorer.AppInfo;
import org.appxi.javafx.control.DialogPaneEx;
import org.appxi.javafx.workbench.views.WorkbenchWorktoolController;
import org.appxi.prefs.UserPrefs;

public class AboutController extends WorkbenchWorktoolController {
    public AboutController() {
        super("ABOUT", "关于");
    }

    @Override
    public Label getViewpartInfo() {
        final Label info = new Label(this.viewName, new FontAwesomeIconView(FontAwesomeIcon.QUESTION_CIRCLE));
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

        final Label head = new Label(AppInfo.NAME, new ImageView(getClass().getResource("/appxi/cbetaExplorer/icons/icon-64.png").toExternalForm()));
        head.setStyle("-fx-font-size: 2em; -fx-padding: .5em 1em 1em .5em;");

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
        web.getEngine().setUserDataDirectory(UserPrefs.confDir().toFile());

        buf.append("Embedded Webkit Info").append("\n");
        buf.append(web.getEngine().getUserAgent());

        info.setText(buf.toString());

        final DialogPaneEx pane = new DialogPaneEx();
        pane.setContent(new VBox(head, info));
        alert.setDialogPane(pane);

        showAlertWithThemeAndWaitForNothing(alert);
    }
}
