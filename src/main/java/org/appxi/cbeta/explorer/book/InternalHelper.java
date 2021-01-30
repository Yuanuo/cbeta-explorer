package org.appxi.cbeta.explorer.book;

import org.appxi.cbeta.explorer.AppInfo;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.FileHelper;
import org.appxi.util.StringHelper;

import java.nio.file.Path;

class InternalHelper {
    private static final String[] htmlIncludeNames = {"jquery.0.js",
            "jquery.isinviewport.js",
            "jquery.highlight.js",
            "jquery.scrollto.js",
            "jquery.finder.js",
            "app.css",
            "app.js"};
    static final String[] htmlIncludes = new String[htmlIncludeNames.length];

    static void initHtmlIncludes() {
        FileHelper.extractFiles(
                file -> InternalHelper.class.getResourceAsStream(StringHelper.concat("/appxi/cbetaExplorer/incl/", file)),
                InternalHelper::buildHtmlIncludeTarget,
                htmlIncludeNames);
        for (int i = 0; i < htmlIncludeNames.length; i++) {
            htmlIncludes[i] = buildHtmlIncludeTarget(htmlIncludeNames[i]).toUri().toString();
        }
    }

    private static Path buildHtmlIncludeTarget(String file) {
        return UserPrefs.dataDir().resolve(".html-incl").resolve(StringHelper.concat(AppInfo.VERSION, "-", file));
    }

}
