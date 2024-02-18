package org.appxi.cbeta.app.search;

import org.appxi.cbeta.Bookcase;
import org.appxi.cbeta.Profile;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.util.DigestHelper;
import org.appxi.util.FileHelper;

import java.nio.file.Path;
import java.util.Objects;

public class IndexedManager {
    private static final String BOOKCASE_V = "22.03.17.1";
    private static final String BOOKLIST_V = "21.10.24.1";

    final Bookcase bookcase;
    final Profile profile;
    Preferences config;
    final Path configFile;

    public IndexedManager(Bookcase bookcase, Profile profile) {
        this.bookcase = bookcase;
        this.profile = profile;
        //
        this.configFile = profile.workspace().resolve(".idx");
        reload();
    }

    public void reload() {
        this.config = new PreferencesInProperties(configFile);
    }

    private String currentBookcaseVersion() {
        return DigestHelper.crc32c(bookcase.getVersion(), BOOKCASE_V);
    }

    private String currentBookListVersion() {
        return DigestHelper.crc32c(profile.version(), BOOKLIST_V);
    }

    public boolean isBookListIndexable() {
        if (FileHelper.notExists(configFile)) {
            return true;
        }

        // 如果Profile文件内容有变化（或算法有变）均需要更新索引
        String indexed = config.getString("index.ver", null);
        if (!Objects.equals(currentBookListVersion(), indexed)) {
            return true;
        }

        // 如果Bookcase数据有变化（或算法有变）均需要更新索引
        indexed = config.getString("index.based", null);
        return !Objects.equals(currentBookcaseVersion(), indexed);
    }

    public void saveIndexedVersions() {
        config.setProperty("index.ver", currentBookListVersion());
        config.setProperty("index.based", currentBookcaseVersion());
        config.save();
    }
}
