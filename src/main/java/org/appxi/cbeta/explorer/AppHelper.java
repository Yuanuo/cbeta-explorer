package org.appxi.cbeta.explorer;

import org.appxi.timeago.TimeAgo;

public abstract class AppHelper {
    static AppWorkbench primaryApp;
    private static TimeAgo.Messages timeAgoI18N;

    private AppHelper() {
    }

    public static AppWorkbench primaryApp() {
        return primaryApp;
    }

    public static TimeAgo.Messages timeAgoI18N() {
        if (null == timeAgoI18N)
            timeAgoI18N = TimeAgo.MessagesBuilder.start().withLocale("zh").build();
        return timeAgoI18N;
    }
}
