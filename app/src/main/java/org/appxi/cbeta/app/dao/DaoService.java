package org.appxi.cbeta.app.dao;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.logger.Level;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.appxi.javafx.app.BaseApp;
import org.appxi.util.StringHelper;

import java.nio.file.Path;
import java.sql.SQLException;

public class DaoService {
    private final Object LOCK = new Object();
    private final Object connSyncLock = new Object();
    private ConnectionSource connSource;
    private String databaseUrl = "";

    public DaoService(Path dataDir) {
        if (BaseApp.productionMode)
            Logger.setGlobalLogLevel(Level.WARNING);
        databaseUrl = StringHelper.concat("jdbc:h2:", dataDir.resolve("db"), ";database_to_upper=false");
    }

    private static Dao<Bookdata, Integer> bookdataDao;

    public Dao<Bookdata, Integer> getBookdataDao() {
        if (null == bookdataDao) {
            synchronized (LOCK) {
                if (null == bookdataDao)
                    bookdataDao = createDaoAndInitTableIfNotExists(Bookdata.class);
            }
        }
        return bookdataDao;
    }
    // //////////////////////////////////////////////////////////////////////////////////////////

    ConnectionSource getConnSource() {
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

    public <D extends Dao<T, ?>, T> D createDao(Class<T> clazz) {
        try {
            return DaoManager.createDao(getConnSource(), clazz);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <D extends Dao<T, ?>, T> D createDaoAndInitTableIfNotExists(Class<T> clazz) {
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
