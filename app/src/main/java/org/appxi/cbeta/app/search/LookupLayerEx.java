package org.appxi.cbeta.app.search;

import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Labeled;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.appxi.cbeta.app.DataApp;
import org.appxi.javafx.control.LookupLayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class LookupLayerEx extends LookupLayer<Object> {
    private final DataApp dataApp;
    private Set<String> usedKeywords;
    protected String inputQuery;

    public LookupLayerEx(DataApp dataApp, StackPane owner) {
        super(owner);
        this.dataApp = dataApp;
    }

    @Override
    protected void updateItemLabel(Labeled labeled, Object item) {
        String text = labeled.getText();
        if (null == text) text = null == item ? "<TEXT>" : item.toString();
        //
        if (null != usedKeywords && !usedKeywords.isEmpty()) {
            List<String> lines = new ArrayList<>(List.of(text));
            for (String keyword : usedKeywords) {
                for (int i = 0; i < lines.size(); i++) {
                    final String line = lines.get(i);
                    if (line.startsWith("§§#§§")) continue;

                    List<String> list = List.of(line
                            .replace(keyword, "\n§§#§§".concat(keyword).concat("\n"))
                            .split("\n"));
                    if (list.size() > 1) {
                        lines.remove(i);
                        lines.addAll(i, list);
                        i++;
                    }
                }
            }
            List<Text> texts = new ArrayList<>(lines.size());
            for (String line : lines) {
                if (line.startsWith("§§#§§")) {
                    Text text1 = new Text(line.substring(5));
                    text1.getStyleClass().add("highlight");
                    texts.add(text1);
                } else {
                    final Text text1 = new Text(line);
                    text1.getStyleClass().add("plaintext");
                    texts.add(text1);
                }
            }
            TextFlow textFlow = new TextFlow(texts.toArray(new Node[0]));
            textFlow.getStyleClass().add("text-flow");
            labeled.setText(text);
            labeled.setGraphic(textFlow);
            labeled.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            //
            textFlow.getChildren().forEach(t -> ((Text) t).setText(dataApp.hanTextToShow(((Text) t).getText())));
        } else {
            labeled.setText(text);
            labeled.setContentDisplay(ContentDisplay.TEXT_ONLY);
        }
    }

    @Override
    protected final LookupResult<Object> lookupByKeywords(String lookupText, int resultLimit) {
        if (!lookupText.isEmpty() && lookupText.matches("[(（“\"]+")) return null;

        //
        inputQuery = lookupText;

        if (lookupText.matches("[!！].*")) {
            lookupText = lookupText.substring(1);
            lookupText = !lookupText.isBlank() ? lookupText : null;// 此时无法搜索，保持现状
        } else {
            lookupText = dataApp.hanTextToBase(lookupText);
        }
        // 如果此时的输入并无必要进行搜索，允许子类实现中返回null以中断并保持现状
        if (lookupText == null) return null;

        List<LookupResultItem> result = new ArrayList<>(resultLimit);

        lookupByKeywords(lookupText, resultLimit, result, usedKeywords = new LinkedHashSet<>());
        if (!lookupText.isEmpty()) {
            // 默认时无输入，不需对结果进行排序
            result.sort(Comparator.comparingDouble(LookupResultItem::score).reversed());
            if (result.size() > resultLimit + 1)
                result = result.subList(0, resultLimit + 1);
        }
        List<Object> resultList = result.stream().map(LookupResultItem::data).toList();
        return new LookupResult<>(resultList.size(), resultList.size(), resultList);
    }

    protected abstract void lookupByKeywords(String lookupText, int resultLimit,
                                             List<LookupResultItem> result,
                                             Set<String> usedKeywords);

    public record LookupResultItem(Object data, double score) {
    }
}
