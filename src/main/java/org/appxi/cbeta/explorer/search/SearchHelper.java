package org.appxi.cbeta.explorer.search;

import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.tome.cbeta.CbetaBook;

import java.util.function.Function;

public abstract class SearchHelper {
    private SearchHelper() {
    }

    public static Function<String, CbetaBook> searchById;

    public static CbetaBook searchById(String id) {
        return null == searchById ? null : searchById.apply(id);
    }

    private static SearchEngine searchEngine;
    private static SearchService searchService;

    public static void setupSearchService(WorkbenchApplication application) {
        if (null != searchService)
            return;
        searchEngine = new SearchEngineMem(application);
        searchService = new SearchService(application, searchEngine);
        searchService.setupInitialize();
        searchEngine.setupInitialize();
    }
}
