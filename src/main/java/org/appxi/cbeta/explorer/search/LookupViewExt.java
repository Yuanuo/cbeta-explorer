package org.appxi.cbeta.explorer.search;

import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Labeled;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.javafx.control.LookupView;
import org.appxi.util.StringHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public abstract class LookupViewExt extends LookupView<Object> {
    private LookupRequest lookupRequest;

    public LookupViewExt(StackPane owner) {
        super(owner);
    }

    @Override
    protected void updateItemLabel(Labeled labeled, Object item) {
        String text = labeled.getText();
        if (null == text) text = null == item ? "<TEXT>" : item.toString();
        //
        if (null != lookupRequest && !lookupRequest.keywords.isEmpty()) {
            List<String> lines = new ArrayList<>(List.of(text));
            for (LookupKeyword keyword : lookupRequest.keywords()) {
                for (int i = 0; i < lines.size(); i++) {
                    final String line = lines.get(i);
                    if (line.startsWith("§§#§§")) continue;

                    List<String> list = List.of(line
                            .replace(keyword.text, "\n§§#§§".concat(keyword.text).concat("\n"))
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

        lookupRequest = LookupRequest.of(lookupText, resultLimit);
        List<LookupResultItem> result = lookupByKeywords(lookupRequest);
        if (!lookupText.isEmpty()) {
            // 默认时无输入，不需对结果进行排序
            result.sort(Comparator.<LookupResultItem>comparingDouble(v -> v.score).reversed());
            if (result.size() > resultLimit + 1)
                result = result.subList(0, resultLimit + 1);
        }
        return result.stream().map(v -> v.data).toList();
    }

    protected abstract List<LookupResultItem> lookupByKeywords(LookupRequest lookupRequest);

    public record LookupResultItem(Object data, double score) {
    }

    public record LookupKeyword(String text, boolean isFullAscii) {
    }

    public record LookupRequest(List<LookupKeyword> keywords, String text, int resultLimit) {
        public static LookupRequest of(String lookupText, int resultLimit) {
            return new LookupRequest(
                    Stream.of(StringHelper.split(lookupText, " ", "[()（）“”\"]"))
                            .map(str -> str.replaceAll("[()（）“”\"]", "")
                                    .replaceAll("\s+", " ").toLowerCase().strip())
                            .filter(str -> !str.isEmpty())
                            .map(str -> new LookupKeyword(str, str.matches("[a-zA-Z0-9\s]+")))
                            .toList(),
                    lookupText, resultLimit);
        }
    }
}
