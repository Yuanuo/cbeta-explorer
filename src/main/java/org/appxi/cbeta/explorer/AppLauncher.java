package org.appxi.cbeta.explorer;

import javafx.application.Application;

public class AppLauncher {
    protected static void beforeLaunch() {
        //
        System.setProperty("javafx.preloader", "org.appxi.cbeta.explorer.AppPreloader");
    }

    public static void main(String[] args) {
        try {
            beforeLaunch();
            Application.launch(AppWorkbench.class, args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
