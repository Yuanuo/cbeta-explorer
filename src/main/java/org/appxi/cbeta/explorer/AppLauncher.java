package org.appxi.cbeta.explorer;

import javafx.application.Application;

public class AppLauncher {

    public static void main(String[] args) {
        System.setProperty("javafx.preloader", "org.appxi.cbeta.explorer.AppPreloader");
        try {
            Application.launch(AppWorkbench.class, args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
