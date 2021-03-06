package org.appxi.cbeta.explorer.search;

import javafx.scene.layout.StackPane;
import org.appxi.hanlp.convert.ChineseConvertors;

import java.util.ArrayList;
import java.util.Collection;

public abstract class LookupViewEx<T> extends LookupView<T> {
    public LookupViewEx(StackPane primaryViewport) {
        super(primaryViewport);
    }

    @Override
    protected final Collection<T> search(String inputText, int resultLimit) {
        String searchText = inputText.replaceAll("[,，]$", "").strip();
        if (!searchText.isEmpty() && searchText.charAt(0) == '#') {
            String[] searchTerms = searchText.substring(1).split("[;；]");
            Collection<T> result = new ArrayList<>(searchTerms.length);
            for (String searchTerm : searchTerms) {
                convertSearchTermToCommands(searchTerm.strip(), result);
            }
            return result;
        } else if (!searchText.isEmpty() && (searchText.charAt(0) == '!' || searchText.charAt(0) == '！')) {
            // 为避免自动转换失误导致检索失败，此处特殊处理，允许以感叹号开始的字符串不自动转换简繁体
            searchText = searchText.substring(1).strip();
        } else {
            // 由于CBETA数据是繁体汉字，此处转换以匹配目标文字
            searchText = ChineseConvertors.hans2HantTW(searchText.strip());
        }
        String[] searchWords = searchText.split("[,，]");
        if (searchWords.length == 1)
            searchWords = null;
        return search(searchText, searchWords, resultLimit);
    }

    protected abstract Collection<T> search(String searchText, String[] searchWords, int resultLimit);

    protected abstract void convertSearchTermToCommands(String searchTerm, Collection<T> result);
}
