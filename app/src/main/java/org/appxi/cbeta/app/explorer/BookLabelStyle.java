package org.appxi.cbeta.app.explorer;

import org.appxi.cbeta.Book;
import org.appxi.util.StringHelper;

import java.util.function.Function;

public enum BookLabelStyle {
    name("书名，例：大智度论", item -> item.title),
    name_vols("书名+卷数，例：大智度论 (100卷)", item -> {
        String text = item.title;
        if (null != item.path && item.path.endsWith(".xml") && item.volumes.size() > 0) {
            return StringHelper.concat(text, "（", item.volumes.size(), "卷）");
        }
        return text;
    }),
    id_name("ID+书名，例：T1509 大智度论", item -> {
        String text = item.title;
        if (null != item.path && item.path.endsWith(".xml")) {
            return StringHelper.concat(item.id, ' ', text);
        }
        return text;
    }),
    id_name_vols("ID+书名+卷数，例：T1509 大智度论 (100卷)", item -> {
        String text = item.title;
        if (null != item.path && item.path.endsWith(".xml") && item.volumes.size() > 0) {
            return StringHelper.concat(item.id, ' ', text, "（", item.volumes.size(), "卷）");
        }
        return text;
    });
    final String title;
    final Function<Book, String> format;

    BookLabelStyle(String title, Function<Book, String> format) {
        this.title = title;
        this.format = format;
    }

    @Override
    public String toString() {
        return title;
    }

    public static BookLabelStyle valueBy(String name) {
        try {
            return valueOf(name);
        } catch (Throwable e) {
            return name_vols;
        }
    }

    public String format(Book book) {
        return this.format.apply(book);
    }
}