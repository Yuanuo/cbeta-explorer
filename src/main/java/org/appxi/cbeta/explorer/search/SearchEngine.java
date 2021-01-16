package org.appxi.cbeta.explorer.search;

import java.util.Collection;
import java.util.List;

public interface SearchEngine {
    List<SearchRecord> search(String query, int offset, int size);

    void addSearchRecord(SearchRecord record);

    void addSearchRecords(Collection<? extends SearchRecord> records);
}
