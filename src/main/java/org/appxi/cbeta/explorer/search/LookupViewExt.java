package org.appxi.cbeta.explorer.search;

import javafx.scene.layout.StackPane;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.javafx.control.LookupView;

public abstract class LookupViewExt extends LookupView {
    public LookupViewExt(StackPane owner) {
        super(owner);
    }

    @Override
    protected String prepareLookupText(String lookupText) {
        if (lookupText.matches("[!！].*")) {
            lookupText = lookupText.substring(1);
            return !lookupText.isEmpty() ? lookupText : null;// 此时无法搜索，保持现状
        } else return ChineseConvertors.hans2HantTW(lookupText);
    }
}
