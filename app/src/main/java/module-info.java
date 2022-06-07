module appxi.cbetaExplorer {
    requires java.desktop;

    requires appxi.cbeta;
    requires appxi.javafx;
    requires appxi.timeago;
    requires org.jsoup;
    requires org.json;

    requires java.sql;
    requires static com.h2database;
    requires static ormlite.jdbc;

    requires appxi.search.solr;

//    requires org.scenicview.scenicview; // for debug

    exports org.appxi.cbeta.app; // for application launch

    opens org.appxi.cbeta.app;
    opens org.appxi.cbeta.app.dao; // for ormlite, spring
    opens org.appxi.cbeta.app.reader; // for js-engine
}