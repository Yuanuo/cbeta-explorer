package org.appxi.cbeta.explorer.dao;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.logger.Level;
import com.j256.ormlite.logger.Logger;
import org.appxi.javafx.helper.FxHelper;

public abstract class DaoService {
    private static final Object LOCK = new Object();

    private DaoService() {
    }

    public static void setupInitialize() {
        if (FxHelper.productionMode)
            Logger.setGlobalLogLevel(Level.WARNING);
        getBookdataDao();
    }

    private static Dao<Bookdata, Integer> bookdataDao;

    public static Dao<Bookdata, Integer> getBookdataDao() {
        if (null == bookdataDao) {
            synchronized (LOCK) {
                if (null == bookdataDao)
                    bookdataDao = DaoHelper.createDaoAndInitTableIfNotExists(Bookdata.class);
            }
        }
        return bookdataDao;
    }
    // //////////////////////////////////////////////////////////////////////////////////////////
}
