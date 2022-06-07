package org.appxi.cbeta.app.dao;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.appxi.util.StringHelper;

import java.nio.file.Path;
import java.sql.SQLException;

public abstract class DaoHelper {
    private DaoHelper() {
    }

    private static final Object connSyncLock = new Object();
    private static ConnectionSource connSource;
    private static String databaseUrl = "";

    public static void setupDatabaseService(Path dataDir) {
        databaseUrl = StringHelper.concat("jdbc:h2:", dataDir.resolve("db"), ";database_to_upper=false");
    }

    static ConnectionSource getConnSource() {
        if (null != connSource)
            return connSource;
        synchronized (connSyncLock) {
            if (null == connSource) {
                try {
                    connSource = new JdbcPooledConnectionSource(databaseUrl);
                    // only keep the connections open for 5 minutes
                    ((JdbcPooledConnectionSource) connSource).setMaxConnectionAgeMillis(5 * 60 * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return connSource;
    }

    public static <D extends Dao<T, ?>, T> D createDao(Class<T> clazz) {
        try {
            return DaoManager.createDao(getConnSource(), clazz);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <D extends Dao<T, ?>, T> D createDaoAndInitTableIfNotExists(Class<T> clazz) {
        D dao = createDao(clazz);
        try {
            if (null != dao && !dao.isTableExists())
                TableUtils.createTableIfNotExists(getConnSource(), clazz);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dao;
    }
}
