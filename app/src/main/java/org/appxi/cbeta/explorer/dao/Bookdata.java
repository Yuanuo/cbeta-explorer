package org.appxi.cbeta.explorer.dao;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "bookdata")
public class Bookdata extends AbstractData {
    @DatabaseField(index = true, columnName = "dataType")
    public BookdataType type;

    @DatabaseField(index = true)
    public String book;

    @DatabaseField
    public String volume;

    @DatabaseField
    public String location;

    @DatabaseField
    public String anchor;

    public Bookdata() {
    }
}
