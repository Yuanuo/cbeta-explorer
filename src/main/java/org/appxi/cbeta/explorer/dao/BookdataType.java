package org.appxi.cbeta.explorer.dao;

public enum BookdataType {
    bookmark("书签"),
    favorite("收藏"),
    booknote("笔记");

    public final String title;

    BookdataType(String title) {
        this.title = title;
    }
}
