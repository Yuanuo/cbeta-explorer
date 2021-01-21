package org.appxi.cbeta.explorer;

import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.appxi.prefs.UserPrefs;
import org.appxi.tome.cbeta.BookMap;
import org.appxi.tome.cbeta.CbetaHelper;

import java.io.File;

public abstract class CbetaxHelper {
    public static final BookMap books = new BookMap();

    static void setDataDirectory(Stage primaryStage) {
        String dir = UserPrefs.prefs.getString("cbeta.dir", "");
        while (true) {
            if (CbetaHelper.setDataDirectory(dir)) {
                UserPrefs.prefs.setProperty("cbeta.dir", CbetaHelper.getDataDirectory());
                return;
            }
            final DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("请选择CBETA数据主目录（cbeta 或 cbeta/Bookcase/CBETA）");
            final File selectedDir = dirChooser.showDialog(primaryStage);
            if (null == selectedDir) {
                System.exit(-1);
                return;
            }
            dir = selectedDir.getAbsolutePath();
        }
    }

    public static String stripUnexpected(String text) {
        return text.replaceAll("\\p{Punct}", "").strip();
    }

    private CbetaxHelper() {
    }
}
