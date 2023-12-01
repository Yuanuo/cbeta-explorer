package org.appxi.cbeta.app;

import javafx.scene.input.DataFormat;
import org.appxi.cbeta.BookMap;
import org.appxi.cbeta.Bookcase;
import org.appxi.cbeta.TripitakaMap;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.UserPrefs;
import org.appxi.smartcn.convert.ChineseConvertors;
import org.appxi.smartcn.pinyin.PinyinHelper;

import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AppContext {
    public static final DataFormat DND_ITEM = new DataFormat("application/x-item-serialized-object");
    static Path workspace;
    static Bookcase bookcase;
    static BookMap bookMap;
    static TripitakaMap tripitakaMap;

    static void setupBookcase(Bookcase bookcase, Preferences config) {
        try {
            // 尝试优先使用绿色版数据
            final Path bookcaseDir = Path.of(bookcase.getPath()).getParent().resolve(AppLauncher.dataDirName);
            if (Files.exists(bookcaseDir) && Files.isDirectory(bookcaseDir) && Files.isWritable(bookcaseDir)) {
                // 如果是App内置或便携版数据源，则不更新全局配置
                if (null != config) {
                    config.save();
                }
                workspace = bookcaseDir;
            } else {
                workspace = UserPrefs.dataDir();
            }
        } catch (Throwable ignore) {
        }
        //
        AppContext.bookcase = AppContext.bookcase == null ? bookcase : AppContext.bookcase;
        tripitakaMap = new TripitakaMap(AppContext.bookcase);
        bookMap = bookMap == null ? new BookMap(tripitakaMap) : bookMap;
//        // 如果以上执行出错，程序也尚未初始化完成，只有在基础数据正常时，再加载更多数据

        new Thread(() -> {
            bookMap.data();
            ChineseConvertors.hans2HantTW("测试");
            PinyinHelper.pinyin("init");
        }).start();
    }

    public static Bookcase bookcase() {
        return bookcase;
    }

    public static BookMap bookMap() {
        return bookMap;
    }

    public static TripitakaMap tripitakaMap() {
        return tripitakaMap;
    }

    private AppContext() {
    }
}
