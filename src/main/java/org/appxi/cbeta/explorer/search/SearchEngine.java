package org.appxi.cbeta.explorer.search;

import java.util.Collection;
import java.util.function.BiPredicate;

public interface SearchEngine {
    void setupInitialize();

    void addSearchRecord(SearchRecord record);

    void addSearchRecords(Collection<? extends SearchRecord> records);

    void search(String searchText, String[] searchWords, BiPredicate<Integer, SearchRecord> handle);
}
