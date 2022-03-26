package org.appxi.cbeta.explorer.search;

import org.appxi.prefs.UserPrefs;

public enum SearchEngineStart {
    start("程序启动时"),
    needs("初次使用时");
    final String title;

    SearchEngineStart(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return title;
    }

    public static SearchEngineStart valueBy(String name) {
        try {
            return valueOf(name);
        } catch (Throwable e) {
            return needs;
        }
    }

    public static SearchEngineStart value() {
        return SearchEngineStart.valueBy(UserPrefs.prefs.getString("search.engine.start", "needs"));
    }
}