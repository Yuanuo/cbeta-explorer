package org.appxi.cbeta.explorer;

import org.appxi.cbeta.explorer.event.StatusEvent;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.prefs.UserPrefs;
import org.appxi.timeago.TimeAgo;
import org.appxi.util.ext.HanLang;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;

public abstract class AppContext {
    private static final Object LOCK = new Object();

    private static AppWorkbench application;
    private static AnnotationConfigApplicationContext beans;
    private static final Object beansInit = new Object();

    static void setApplication(AppWorkbench app) {
        AppContext.application = AppContext.application == null ? app : AppContext.application;
    }

    public static AppWorkbench app() {
        return application;
    }

    public static BeanFactory beans() {
        if (null != beans)
            return beans;
        synchronized (beansInit) {
            if (null != beans)
                return beans;
            try {
                beans = new AnnotationConfigApplicationContext(AppConfig.class) {
                    @Override
                    public Resource[] getResources(String locationPattern) throws IOException {
                        Resource[] result = super.getResources(locationPattern);
                        if (result.length == 0 && locationPattern.equals("classpath*:org/appxi/cbeta/explorer/dao/**/*.class")) {
                            result = new Resource[]{
                                    new UrlResource(AppContext.class.getResource("/org/appxi/cbeta/explorer/dao/PiecesRepository.class"))
                            };
                        }
                        return result;
                    }
                };
            } catch (Throwable t) {
                FxHelper.alertError(app(), t);
            }
        }
        return beans;
    }

    static void setupInitialize() {
        beans();
        if (null != application) {
            application.eventBus.addEventHandler(ApplicationEvent.STOPPING, event -> {
                if (null != AppConfig.cachedSolrClient) {
                    try {
                        AppConfig.cachedSolrClient.close();
                    } catch (Throwable ignored) {
                    }
                }
            });
            application.eventBus.fireEvent(new StatusEvent(StatusEvent.BEANS_READY));
        }
    }

    private static TimeAgo.Messages timeAgoI18N;

    public static TimeAgo.Messages timeAgoI18N() {
        if (null == timeAgoI18N)
            synchronized (LOCK) {
                if (null == timeAgoI18N)
                    timeAgoI18N = TimeAgo.MessagesBuilder.start().withLocale("zh").build();
            }
        return timeAgoI18N;
    }

    public static HanLang getDisplayHanLang() {
        return HanLang.valueBy(UserPrefs.prefs.getString("display.han", HanLang.hantTW.lang));
    }

    public static double getDisplayZoomLevel() {
        double zoomLevel = UserPrefs.prefs.getDouble("display.zoom", 1.6);
        if (zoomLevel < 1.5 || zoomLevel > 3.0)
            zoomLevel = 1.6;
        return zoomLevel;
    }

    private AppContext() {
    }
}
