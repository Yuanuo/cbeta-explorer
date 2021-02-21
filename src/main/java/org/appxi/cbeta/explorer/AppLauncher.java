package org.appxi.cbeta.explorer;

import javafx.application.Application;

import java.util.Locale;

public class AppLauncher {

    public static void main(String[] args) {
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
        System.setProperty("javafx.preloader", "org.appxi.cbeta.explorer.AppPreloader");
        try {
            Application.launch(AppWorkbench.class, args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
