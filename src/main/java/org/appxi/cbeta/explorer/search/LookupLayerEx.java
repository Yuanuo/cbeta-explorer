package org.appxi.cbeta.explorer.search;

import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Labeled;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.javafx.control.LookupLayer;
import org.appxi.smartcn.convert.ChineseConvertors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class LookupLayerEx extends LookupLayer<Object> {
    private Set<String> usedKeywords;

    public LookupLayerEx(StackPane owner) {
        super(owner);
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
            textFlow.getChildren().forEach(t -> ((Text) t).setText(AppContext.displayText(((Text) t).getText())));
        } else {
            labeled.setText(text);
            labeled.setContentDisplay(ContentDisplay.TEXT_ONLY);
        }
    }

    @Override
    protected final Collection<Object> lookupByKeywords(String lookupText, int resultLimit) {
        if (!lookupText.isEmpty() && lookupText.matches("[(（“\"]+")) return null;

        if (lookupText.matches("[!！].*")) {
            lookupText = lookupText.substring(1);
            lookupText = !lookupText.isBlank() ? lookupText : null;// 此时无法搜索，保持现状
        } else lookupText = ChineseConvertors.hans2HantTW(lookupText);
        // 如果此时的输入并无必要进行搜索，允许子类实现中返回null以中断并保持现状
        if (lookupText == null) return null;

        List<LookupResultItem> lookupResult = new ArrayList<>(resultLimit);

        lookupByKeywords(lookupText, resultLimit, lookupResult, usedKeywords = new LinkedHashSet<>());
        if (!lookupText.isEmpty()) {
            // 默认时无输入，不需对结果进行排序
            lookupResult.sort(Comparator.comparingDouble(LookupResultItem::score).reversed());
            if (lookupResult.size() > resultLimit + 1)
                lookupResult = lookupResult.subList(0, resultLimit + 1);
        }
        return lookupResult.stream().map(LookupResultItem::data).toList();
    }

    protected abstract void lookupByKeywords(String lookupText, int resultLimit,
                                             List<LookupResultItem> result,
                                             Set<String> usedKeywords);

    public record LookupResultItem(Object data, double score) {
    }
}
