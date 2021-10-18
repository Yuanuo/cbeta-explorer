package org.appxi.cbeta.explorer;

import javafx.application.Application;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.DateHelper;
import org.appxi.util.FileHelper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

public class AppLauncher {
    protected static void beforeLaunch(String dataDirName) {
        // 0, set data home
        UserPrefs.localDataDirectory(null != dataDirName ? dataDirName : ".".concat(AppInfo.ID), null);
        // 由于在配置文件中不能使用动态变量作为路径，故在此设置日志文件路径
        if (FxHelper.productionMode) {
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
        //
        System.setProperty("javafx.preloader", "org.appxi.cbeta.explorer.AppPreloader");
    }

    public static void main(String[] args) {
        try {
            beforeLaunch(null);
            Application.launch(AppWorkbench.class, args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
