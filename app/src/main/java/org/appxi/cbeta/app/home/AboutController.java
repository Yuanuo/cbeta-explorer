package org.appxi.cbeta.app.home;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.appxi.cbeta.app.App;
import org.appxi.cbeta.app.AppContext;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPart;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.util.OSVersions;

import java.util.Optional;

public class AboutController extends WorkbenchPartController implements WorkbenchPart.SideTool {
    public AboutController(WorkbenchPane workbench) {
        super(workbench);

        this.id.set("ABOUT");
        this.title.set("关于");
        this.tooltip.set("关于");
        this.graphic.set(MaterialIcon.INFO_OUTLINE.graphic());
    }

    @Override
    public void postConstruct() {
    }

    @Override
    public void activeViewport(boolean firstTime) {
        final Label head = new Label(App.NAME);
        head.setStyle("-fx-font-size: 2em; -fx-padding: .5em 0;");
        head.setMaxWidth(Double.MAX_VALUE);
        head.setAlignment(Pos.CENTER);
        Optional.ofNullable(App.class.getResource("icon-256.png"))
                .ifPresent(url -> {
                    final ImageView graphic = new ImageView(url.toExternalForm());
                    graphic.setFitWidth(48);
                    graphic.setFitHeight(48);
                    head.setGraphic(graphic);
                    head.setGraphicTextGap(20);
                });

        final HBox headBox = new HBox(head);
        HBox.setHgrow(head, Priority.ALWAYS);

        final Label desc = new Label("经藏典籍阅读器");
        desc.setStyle("-fx-font-size: 1.5em; -fx-padding: .5em 0 2em 0;");
        desc.setMaxWidth(Double.MAX_VALUE);
        desc.setAlignment(Pos.CENTER);
        final HBox descBox = new HBox(desc);
        HBox.setHgrow(desc, Priority.ALWAYS);

        final TextArea info = new TextArea();
        VBox.setVgrow(info, Priority.ALWAYS);
        info.setWrapText(true);
        info.setEditable(false);
        info.setPrefRowCount(15);

        final StringBuilder buf = new StringBuilder();
        buf.append("Product Version").append("\n");
        buf.append(App.NAME).append(" ").append(App.VERSION).append("\n\n");

        buf.append("DATA Info").append("\n");
        buf.append(AppContext.bookcase().getVersion())
                .append(" / ").append(AppContext.bookcase().getQuarterlyVersion())
                .append(" / ").append(AppContext.bookcase().getPath())
                .append("\n\n");

        buf.append("Workspace").append("\n");
        buf.append(app.workspace)
                .append("\n\n");

        buf.append("Java Version").append("\n");
        buf.append(System.getProperty("java.runtime.version")).append("\n\n");

        buf.append("JavaFX Version").append("\n");
        buf.append(System.getProperty("javafx.runtime.version")).append("\n\n");

        buf.append("Platform Info").append("\n");
        buf.append(System.getProperty("os.name")).append(", ");
        buf.append(System.getProperty("os.arch")).append(", ");
        buf.append(System.getProperty("os.version")).append("\n\n");

        WebView web = new WebView();
        web.getEngine().setUserDataDirectory(app.workspace.resolve(".cached").toFile());

        buf.append("Webview Info").append("\n");
        buf.append(web.getEngine().getUserAgent());

        info.setText(buf.toString());

        //
        final DialogPane dialogPane = new DialogPane() {
            @Override
            protected Node createButtonBar() {
                return null;
            }
        };
        dialogPane.setContent(new VBox(headBox, info));
        dialogPane.getButtonTypes().add(ButtonType.OK);
        //
        final Dialog<?> dialog = new Dialog<>();
        dialog.setTitle(title.get());
        if (OSVersions.isLinux) {
            dialogPane.setPrefSize(540, 720);
            dialog.setResizable(true);
        }
        dialog.setDialogPane(dialogPane);
        dialog.initOwner(app.getPrimaryStage());
        dialog.show();
    }
}
