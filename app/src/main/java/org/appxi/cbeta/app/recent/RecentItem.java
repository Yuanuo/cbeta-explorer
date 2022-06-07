package org.appxi.cbeta.app.recent;

import java.io.Serializable;
import java.util.Date;

class RecentItem implements Serializable {
    private static final long serialVersionUID = 2880605747323682823L;

    public String id, name;
    public Date createAt = new Date();
    public Date updateAt = new Date();

    @Override
    public String toString() {
        return this.name;
    }
}