module appxi.cbetaExplorer {
    requires java.logging;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.jsobject;
    requires transitive javafx.swing;

    requires appxi.shared;
    requires appxi.cbeta;
    requires appxi.javafx;
    requires appxi.timeago;
    requires appxi.hanlp.convert;
    requires appxi.hanlp.pinyin;
    requires org.jsoup;

    requires java.sql;
    requires static com.h2database;
    requires static ormlite.core;
    requires static ormlite.jdbc;
    requires static org.json;

    requires appxi.search.solr;
    requires static appxi.search.solr.aio;
    requires static spring.core;
    requires static spring.context;
    requires static spring.beans;
    requires static spring.data.commons;
    requires static spring.data.solr;
    requires static spring.tx;
    requires static org.apache.logging.log4j.core;
    requires static org.apache.logging.log4j.slf4j;
    requires static org.apache.logging.log4j.web;
    requires org.apache.logging.log4j;
    requires org.slf4j;
    requires org.slf4j.simple;
    requires org.apache.commons.logging;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires java.management;
    requires java.naming;

    exports org.appxi.cbeta.explorer; // for application launch

    opens org.appxi.cbeta.explorer;
    opens org.appxi.cbeta.explorer.book; // for javafx
    opens org.appxi.cbeta.explorer.dao; // for ormlite, spring
    opens appxi.cbetaExplorer.themes;
}