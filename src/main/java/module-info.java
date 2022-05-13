module appxi.cbetaExplorer {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.web;
    requires jdk.jsobject;
    requires javafx.swing;

    requires appxi.shared;
    requires appxi.cbeta;
    requires appxi.javafx;
    requires appxi.timeago;
    requires appxi.smartcn.convert;
    requires appxi.smartcn.pinyin;
    requires org.jsoup;
    requires org.json;

    requires java.sql;
    requires static com.h2database;
    requires static ormlite.jdbc;

    requires appxi.search.solr;

//    requires org.scenicview.scenicview; // for debug

    exports org.appxi.cbeta.explorer; // for application launch

    opens org.appxi.cbeta.explorer;
    opens org.appxi.cbeta.explorer.book; // for javafx
    opens org.appxi.cbeta.explorer.dao; // for ormlite, spring
}