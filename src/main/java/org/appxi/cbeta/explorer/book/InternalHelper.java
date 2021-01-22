package org.appxi.cbeta.explorer.book;

import org.appxi.cbeta.explorer.AppInfo;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.FileHelper;
import org.appxi.util.StringHelper;

import java.nio.file.Path;
import java.util.List;

class InternalHelper {
    private static final String[] includeNames = {"jquery.0.js",
            "jquery.isinviewport.js",
            "jquery.highlight.js",
            "jquery.scrollto.js",
            "jquery.finder.js",
            "app.css",
            "app.js"};
    static String[] htmlIncludes;

    static void initHtmlIncludes() {
        final List<Path> files = FileHelper.extractFiles(
                file -> InternalHelper.class.getResourceAsStream(StringHelper.concat("/appxi/cbetaExplorer/incl/", file)),
                file -> UserPrefs.dataDir().resolve(".html-incl").resolve(StringHelper.concat(AppInfo.VERSION, "-", file)),
                includeNames);
        htmlIncludes = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            htmlIncludes[i] = files.get(i).toUri().toString();
        }
    }

}
