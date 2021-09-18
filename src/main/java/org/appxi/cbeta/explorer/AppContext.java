package org.appxi.cbeta.explorer;

import org.appxi.cbeta.explorer.event.StatusEvent;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInMemory;
import org.appxi.tome.cbeta.BookTreeMode;
import org.controlsfx.control.Notifications;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;

public abstract class AppContext {

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

    private AppContext() {
    }

    public static void runBlocking(Runnable runnable) {
        if (null != application)
            FxHelper.runBlocking(application.getPrimaryViewport(), runnable);
    }

    public static Notifications toast(String msg) {
        return Notifications.create(msg).owner(AppContext.app().getPrimaryStage());
    }

    public static Preferences recentBooks = new PreferencesInMemory();

    public static BookTreeMode bookTreeMode = BookTreeMode.catalog;
}
