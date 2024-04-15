package org.appxi.cbeta.app;

import javafx.application.Application;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.DateHelper;
import org.appxi.util.FileHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class AppLauncher {
    public static Preferences appConfig;
    static String dataDirName;

    protected static void beforeLaunch(String dataDirName) {
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
        // 0, set data home
        AppLauncher.dataDirName = null != dataDirName ? dataDirName : ".".concat(App.ID);
        UserPrefs.localDataDirectory(AppLauncher.dataDirName, null);
        // 由于在配置文件中不能使用动态变量作为路径，故在此设置日志文件路径
        if (!FxHelper.isDevMode) {
            final Path logFile = UserPrefs.dataDir().resolve(".logs")
                    .resolve(DateHelper.format3(new Date()).concat(".log"));
            FileHelper.makeParents(logFile);
            System.setProperty("org.slf4j.simpleLogger.logFile", logFile.toString());
            //
            try (Stream<Path> stream = Files.list(logFile.getParent()).limit(20)) {
                final List<Path> oldLogs = stream.toList();
                if (oldLogs.size() == 20) {
                    for (Path oldLog : oldLogs) {
                        Files.deleteIfExists(oldLog);
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        //
        upgradeConfDir(UserPrefs.dataDir());
        //
        appConfig = new PreferencesInProperties(UserPrefs.dataDir().resolve(".prefs"));
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

    public static void upgradeConfDir(Path dataDir) {
        final Path oldConfDir = dataDir.resolve(".config");
        if (FileHelper.exists(oldConfDir) && Files.isDirectory(oldConfDir)) {
            try (Stream<Path> stream = Files.list(oldConfDir)) {
                for (Path file : stream.toList()) {
                    Files.move(file, file.getParent().getParent().resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
                FileHelper.delete(oldConfDir);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
