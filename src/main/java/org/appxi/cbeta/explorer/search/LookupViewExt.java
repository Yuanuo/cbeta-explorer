package org.appxi.cbeta.explorer.search;

import javafx.scene.layout.StackPane;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.javafx.control.LookupViewEx;

import java.util.Collection;

public abstract class LookupViewExt<T> extends LookupViewEx<T> {
    public LookupViewExt(StackPane owner) {
        super(owner);
    }

    protected String prepareSearchText(String searchText) {
        if (!searchText.isEmpty() && (searchText.charAt(0) == '!' || searchText.charAt(0) == '！')) {
            // 为避免自动转换失误导致检索失败，此处特殊处理，允许以感叹号开始的字符串不自动转换简繁体
            return searchText.substring(1).strip();
        } else {
            // 由于CBETA数据是繁体汉字，此处转换以匹配目标文字
            return ChineseConvertors.hans2HantTW(searchText.strip());
        }
    }

    protected abstract Collection<T> search(String searchText, String[] searchWords, int resultLimit);

    protected abstract void convertSearchTermToCommands(String searchTerm, Collection<T> result);
}
