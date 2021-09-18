package org.appxi.cbeta.explorer.dao;

public abstract class IndexHelper {
    private IndexHelper() {
    }

    public static String wrapWhitespace(String str) {
        return str.replace(" ", "\\ ");
    }
}
