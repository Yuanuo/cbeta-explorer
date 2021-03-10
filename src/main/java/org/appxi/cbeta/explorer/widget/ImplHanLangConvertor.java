package org.appxi.cbeta.explorer.widget;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.util.ext.HanLang;

class ImplHanLangConvertor extends Widget {
    ImplHanLangConvertor(WorkbenchViewController controller) {
        super(controller);
    }

    @Override
    String getName() {
        return "汉字简繁体转换";
    }

    @Override
    Node getViewport() {
        final Label info = new Label("将文字复制到以下文本框中，并选择下方按钮进行转换。\n");
        info.setWrapText(true);

        TextArea textArea = new TextArea();
        VBox.setVgrow(textArea, Priority.ALWAYS);

        FlowPane btnPane = new FlowPane();
        btnPane.setStyle("-fx-padding: 1em 0;-fx-spacing: 1em;");

        final EventHandler<ActionEvent> action = event -> {
            Button button = (Button) event.getSource();
            HanLang hanLang = (HanLang) button.getUserData();

            String text = ChineseConvertors.convert(textArea.getText(), null, hanLang);
            textArea.setText(text);
        };
        for (HanLang hanLang : HanLang.values()) {
            Button button = new Button("转：".concat(hanLang.text));
            btnPane.getChildren().add(button);
            button.setUserData(hanLang);
            button.setOnAction(action);
        }

        return new VBox(info, textArea, btnPane);
    }

    @Override
    void onViewportShow(boolean firstTime) {

    }
}
