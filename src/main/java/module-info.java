module appxi.cbetaexplorer {

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
    requires hanlp.convert;
    requires org.jsoup;
    requires de.jensd.fx.fontawesomefx.fontawesome;

    exports org.appxi.cbeta.explorer;

    opens org.appxi.cbeta.explorer.reader;
}