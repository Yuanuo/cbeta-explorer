package org.appxi.cbeta.explorer;

import org.appxi.prefs.UserPrefs;
import org.appxi.timeago.TimeAgo;
import org.controlsfx.control.Notifications;

public abstract class AppHelper {
    private static TimeAgo.Messages timeAgoI18N;

    private AppHelper() {
    }

    public static TimeAgo.Messages timeAgoI18N() {
        if (null == timeAgoI18N)
            timeAgoI18N = TimeAgo.MessagesBuilder.start().withLocale("zh").build();
        return timeAgoI18N;
    }

    public static Notifications toast() {
        final Notifications notification = Notifications.create();
        if (UserPrefs.prefs.getString("ui.theme", "").contains("dark"))
            return notification.darkStyle();
        return notification;
    }
}
