package org.appxi.cbeta.explorer.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SearchEngineMem implements SearchEngine {
    public static final List<SearchRecord> DATABASE = new ArrayList<>(10240);

    @Override
    public void addSearchRecord(SearchRecord record) {
        DATABASE.add(record);
    }

    @Override
    public void addSearchRecords(Collection<? extends SearchRecord> records) {
        DATABASE.addAll(records);
    }

    @Override
    public List<SearchRecord> search(String query, int offset, int size) {
        return null;
    }

}
