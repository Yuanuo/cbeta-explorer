package org.appxi.cbeta.app.search;

import org.appxi.cbeta.app.AppContext;
import org.appxi.cbeta.app.explorer.BooksProfile;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.DigestHelper;
import org.appxi.util.FileHelper;

import java.util.Objects;

public final class IndexedManager {
    private static final String BOOKCASE_V = "22.03.17.1";
    private static final String BOOKLIST_V = "21.10.24.1";
    private static final Preferences config = new PreferencesInProperties(UserPrefs.confDir().resolve(".indexed"));

    private static String currentBookcaseVersion() {
        return DigestHelper.crc32c(AppContext.bookcase().getVersion(), BOOKCASE_V);
    }

    private static String currentBooklistVersion() {
        return DigestHelper.crc32c(BooksProfile.ONE.profile().version(), BOOKLIST_V);
    }

    public static boolean isBooklistIndexable() {
        final BooksProfile.Profile profile = BooksProfile.ONE.profile();

        if (FileHelper.notExists(UserPrefs.dataDir().resolve("." + profile.indexesId()))) {
            return true;
        }

        // 如果Profile文件内容有变化（或算法有变）均需要更新索引
        String indexed = config.getString(profile.indexesId(), null);
        if (!Objects.equals(currentBooklistVersion(), indexed)) {
            return true;
        }

        // 如果Bookcase数据有变化（或算法有变）均需要更新索引
        indexed = config.getString(profile.indexesId().concat(".based"), null);
        return !Objects.equals(currentBookcaseVersion(), indexed);
    }

    static void saveIndexedVersions() {
        final BooksProfile.Profile profile = BooksProfile.ONE.profile();
        config.setProperty(profile.indexesId(), currentBooklistVersion());
        config.setProperty(profile.indexesId().concat(".based"), currentBookcaseVersion());
        config.save();
    }

    private IndexedManager() {
    }
}
