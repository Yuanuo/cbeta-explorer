package org.appxi.cbeta.explorer;

import appxi.cbeta.BookMap;
import appxi.cbeta.Bookcase;
import appxi.cbeta.TripitakaMap;
import org.appxi.cbeta.explorer.book.BooklistProfile;
import org.appxi.cbeta.explorer.dao.DaoService;
import org.appxi.cbeta.explorer.event.GenericEvent;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.javafx.control.Notifications;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInMemory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.net.URL;

public abstract class AppContext {

    private static AppWorkbench application;

    static void setApplication(AppWorkbench app) {
        AppContext.application = AppContext.application == null ? app : AppContext.application;
    }

    public static AppWorkbench app() {
        return application;
    }

    private static Bookcase bookcase;
    private static BookMap bookMap;

    static void setupInitialize(Bookcase bookcase) {
        AppContext.bookcase = AppContext.bookcase == null ? bookcase : AppContext.bookcase;
        AppContext.bookMap = bookMap == null ? new BookMap(new TripitakaMap(AppContext.bookcase)) : bookMap;
//        // 如果以上执行出错，程序也尚未初始化完成，只有在基础数据正常时，再加载更多数据
        new Thread(() -> {
            AppContext.booksMap().data();
            DaoService.setupInitialize();
            ChineseConvertors.hans2HantTW("测试");
            DisplayHelper.prepareAscii("init");
//            beans();
        }).start();
    }

    public static Bookcase bookcase() {
        return bookcase;
    }

    public static BookMap booksMap() {
        return bookMap;
    }

    public static final BooklistProfile booklistProfile = new BooklistProfile();

    public static BooklistProfile.Profile profile() {
        return booklistProfile.profile();
    }

    private static AnnotationConfigApplicationContext beans;
    private static final Object beansInit = new Object();

    public static BeanFactory beans() {
        if (null != beans)
            return beans;
        synchronized (beansInit) {
            if (null != beans)
                return beans;
            try {
                beans = new AnnotationConfigApplicationContext(SpringConfig.class) {
                    @Override
                    public Resource[] getResources(String locationPattern) {
                        if ("classpath*:org/appxi/cbeta/explorer/dao/**/*.class".equals(locationPattern)) {
                            URL url = AppContext.class.getResource("/org/appxi/cbeta/explorer/dao/PiecesRepository.class");
                            return null == url ? new Resource[0] : new Resource[]{new UrlResource(url)};
                        }
                        return new Resource[0];
                    }
                };
                application.eventBus.fireEvent(new GenericEvent(GenericEvent.BEANS_READY));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return beans;
    }

    public static <T> T getBean(Class<T> requiredType) {
        try {
            return beans().getBean(requiredType);
        } catch (Throwable ignore) {
            return null;
        }
    }

    private AppContext() {
    }

    public static void runBlocking(Runnable runnable) {
        if (null != application)
            FxHelper.runBlocking(application.getPrimaryViewport(), runnable);
    }

    public static Notifications.Notification.Builder toast(String msg) {
        return Notifications.of().description(msg).owner(AppContext.app().getPrimaryStage());
    }

    public static Preferences recentBooks = new PreferencesInMemory();
}
