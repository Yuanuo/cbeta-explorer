package org.appxi.cbeta.app;

import javafx.application.Application;
import org.appxi.javafx.app.DesktopApp;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.DateHelper;
import org.appxi.util.FileHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppLauncher {
    static String dataDirName;

    protected static void beforeLaunch(String dataDirName) {
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
        // 0, set data home
        AppLauncher.dataDirName = null != dataDirName ? dataDirName : ".".concat(App.ID);
        UserPrefs.localDataDirectory(AppLauncher.dataDirName, null);
        // 由于在配置文件中不能使用动态变量作为路径，故在此设置日志文件路径
        if (DesktopApp.productionMode) {
            final Path logFile = UserPrefs.dataDir().resolve(".logs")
                    .resolve(DateHelper.format3(new Date()).concat(".log"));
            FileHelper.makeParents(logFile);
            System.setProperty("org.slf4j.simpleLogger.logFile", logFile.toString());
            //
            try {
                final List<Path> oldLogs = Files.list(logFile.getParent()).limit(20).toList();
                if (oldLogs.size() == 20) {
                    for (Path oldLog : oldLogs) {
                        Files.deleteIfExists(oldLog);
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        UserPrefs.prefs = new PreferencesInProperties(UserPrefs.confDir().resolve(".prefs"));
        //
        System.setProperty("com.j256.simplelogger.backend", "SLF4J");
        System.setProperty("javafx.preloader", "org.appxi.cbeta.app.AppPreloader");
    }

    public static void main(String[] args) {
        try {
            beforeLaunch(null);
            Application.launch(App.class, args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
