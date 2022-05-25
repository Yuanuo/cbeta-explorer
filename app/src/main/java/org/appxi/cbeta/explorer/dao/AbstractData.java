package org.appxi.cbeta.explorer.dao;

import com.j256.ormlite.field.DatabaseField;

import java.util.Date;

public abstract class AbstractData {
    @DatabaseField(generatedId = true)
    public int id;

    @DatabaseField
    public Date createAt;

    @DatabaseField
    public Date updateAt;

    @DatabaseField
    public String color;

    @DatabaseField
    public String data;

    @DatabaseField(width = 1024)
    public String extra;

    public AbstractData() {
    }
}
