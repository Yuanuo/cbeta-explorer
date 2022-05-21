package org.appxi.cbeta.explorer.dict;

import appxi.dict.DictionaryApi;
import appxi.dict.SearchMode;
import appxi.dict.SearchResultEntry;
import appxi.dict.doc.DictEntry;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.appxi.javafx.control.LookupLayer;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchApp;
import org.appxi.smartcn.convert.ChineseConvertors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class DictionaryLookupLayer extends LookupLayer<DictEntry> {
    final WorkbenchApp app;
    final DictionaryController controller;
    String inputQuery, finalQuery;

    DictionaryLookupLayer(DictionaryController controller) {
        super(controller.app.getPrimaryGlass());
        this.app = controller.app;
        this.controller = controller;
    }

    @Override
    protected int getPaddingSizeOfParent() {
        return 200;
    }

    @Override
    protected String getHeaderText() {
        return "查词典";
    }

    @Override
    protected String getUsagesText() {
        return """
                >> 当前支持自动简繁汉字和英文词！
                >> 匹配规则：默认1）以词开始：输入 或 输入*；2）以词结尾：*输入；3）以词存在：*输入*；4）以双引号包含精确查词："输入"；
                >> 快捷键：Ctrl+D 开启；ESC 或 点击透明区 退出此界面；上/下方向键选择列表项；回车键打开；
                """;
    }

    @Override
    protected Collection<DictEntry> lookupByKeywords(String lookupText, int resultLimit) {
        inputQuery = lookupText;

        // detect
        SearchMode searchMode = SearchMode.TitleStartsWith;
        if (lookupText.length() > 2) {
            if (lookupText.charAt(0) == '"' && lookupText.charAt(lookupText.length() - 1) == '"'
                    || lookupText.charAt(0) == '“' && lookupText.charAt(lookupText.length() - 1) == '”') {
                searchMode = SearchMode.TitleEquals;
                lookupText = lookupText.substring(1, lookupText.length() - 1);
            } else if (lookupText.charAt(0) == '*' && lookupText.charAt(lookupText.length() - 1) == '*') {
                searchMode = SearchMode.TitleContains;
                lookupText = lookupText.substring(1, lookupText.length() - 1);
            } else if (lookupText.charAt(0) == '*') {
                searchMode = SearchMode.TitleEndsWith;
                lookupText = lookupText.substring(1);
            } else if (lookupText.charAt(lookupText.length() - 1) == '*') {
                searchMode = SearchMode.TitleStartsWith;
                lookupText = lookupText.substring(0, lookupText.length() - 1);
            }
        } else if (lookupText.equals("*")) {
            lookupText = "";
            searchMode = SearchMode.TitleStartsWith;
        }

        lookupText = lookupText.isBlank() ? "" : ChineseConvertors.toHans(lookupText);
        finalQuery = lookupText;

        resultLimit += 1;
        final boolean finalQueryIsBlank = null == finalQuery || finalQuery.isBlank();
        List<SearchResultEntry> result = new ArrayList<>(1024);
        try {
            Iterator<SearchResultEntry> iter = DictionaryApi.api().searchTitle(searchMode, finalQuery, null, null);
            while (iter.hasNext()) {
                result.add(iter.next());
                if (finalQueryIsBlank && result.size() > resultLimit) {
                    break;
                }
            }
            Collections.sort(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result.size() > resultLimit) {
            result = result.subList(0, resultLimit);
        }
        return result.stream().map(e -> e.dictEntry).collect(Collectors.toList());
    }

    protected void lookupByCommands(String searchTerm, Collection<DictEntry> result) {
    }

    @Override
    protected void updateItemLabel(Labeled labeled, DictEntry data) {
        //
        Label title = new Label(data.title);
        title.getStyleClass().addAll("primary", "plaintext");

        Label detail = new Label(data.dictionary.getName(), MaterialIcon.LOCATION_ON.graphic());
        detail.getStyleClass().add("secondary");

        HBox.setHgrow(title, Priority.ALWAYS);
        HBox pane = new HBox(5, title, detail);

        labeled.setText(title.getText());
        labeled.setGraphic(pane);
        labeled.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        if (!finalQuery.isBlank()) {
            FxHelper.highlight(title, Set.of(finalQuery));
        }
    }

    @Override
    protected void handleEnterOrDoubleClickActionOnSearchResultList(InputEvent event, DictEntry item) {
        controller.showDictEntryViewer(item);
    }
}
