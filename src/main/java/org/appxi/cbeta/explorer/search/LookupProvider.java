package org.appxi.cbeta.explorer.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiPredicate;

interface LookupProvider {

    void search(String searchText, String[] searchWords, BiPredicate<Integer, LookupItem> handle);

    default Collection<LookupItem> search(String searchText, String[] searchWords, int size) {
        final Collection<LookupItem> result = new ArrayList<>(size);
        search(searchText, searchWords, (idx, item) -> {
            result.add(item);
            return idx > size;
        });
        return result;
    }
}
