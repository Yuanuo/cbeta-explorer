module appxi.cbetaExplorer {
    requires java.logging;
    requires javafx.base;
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.jsobject;

    requires appxi.shared;
    requires appxi.tome;
    requires appxi.javafx;
    requires appxi.timeago;
    requires appxi.hanlp.convert;
    requires org.jsoup;
    requires de.jensd.fx.fontawesomefx.materialicons;

    requires java.sql;
    requires static com.h2database;
    requires static ormlite.core;
    requires static ormlite.jdbc;
    requires static org.json;

    exports org.appxi.cbeta.explorer;
    exports org.appxi.cbeta.explorer.bak;

    opens org.appxi.cbeta.explorer.dao;// to ormlite.core
    opens org.appxi.cbeta.explorer.book;
}