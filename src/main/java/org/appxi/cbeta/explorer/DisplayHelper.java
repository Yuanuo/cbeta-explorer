package org.appxi.cbeta.explorer;

import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.prefs.UserPrefs;
import org.appxi.timeago.TimeAgo;
import org.appxi.util.ext.HanLang;

public abstract class DisplayHelper {
    private DisplayHelper() {
    }

    private static HanLang displayHan;

    public static void setDisplayHan(HanLang displayHan) {
        DisplayHelper.displayHan = displayHan;
    }

    public static HanLang getDisplayHan() {
        if (null == displayHan)
            displayHan = HanLang.valueBy(UserPrefs.prefs.getString("display.han", HanLang.hantTW.lang));
        return displayHan;
    }

    public static String displayText(String text) {
        return ChineseConvertors.convert(text, HanLang.hantTW, getDisplayHan());
    }

    public static double getDisplayZoom() {
        double zoomLevel = UserPrefs.prefs.getDouble("display.zoom", 1.6);
        if (zoomLevel < 1.5 || zoomLevel > 3.0)
            zoomLevel = 1.6;
        return zoomLevel;
    }

    private static final Object timeAgoI18NInit = new Object();
    private static TimeAgo.Messages timeAgoI18N;

    public static TimeAgo.Messages timeAgoI18N() {
        if (null == timeAgoI18N)
            synchronized (timeAgoI18NInit) {
                if (null == timeAgoI18N)
                    timeAgoI18N = TimeAgo.MessagesBuilder.start().withLocale("zh").build();
            }
        return timeAgoI18N;
    }
}
