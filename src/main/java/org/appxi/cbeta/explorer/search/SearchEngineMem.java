package org.appxi.cbeta.explorer.search;

import org.appxi.cbeta.explorer.CbetaxHelper;
import org.appxi.cbeta.explorer.event.DataEvent;
import org.appxi.javafx.workbench.WorkbenchController;
import org.appxi.tome.cbeta.CbetaHelper;
import org.appxi.util.DevtoolHelper;
import org.appxi.util.StringHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

class SearchEngineMem implements SearchEngine {
    public static final List<SearchRecord> DATABASE = new ArrayList<>(10240);

    private final WorkbenchController workbenchController;

    public SearchEngineMem(WorkbenchController workbenchController) {
        this.workbenchController = workbenchController;
    }

    @Override
    public void setupInitialize() {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long st = System.currentTimeMillis();
            CbetaxHelper.books.getDataMap().values().parallelStream().forEachOrdered(book -> {
                final Collection<SearchRecord> records = new ArrayList<>();
                records.add(new SearchRecord(book.id, book.title, null, null, book.authorInfo, null));
                CbetaHelper.walkTocChaptersByXmlSAX(book, (href, text) -> records.add(new SearchRecord(book.id, book.title, href, text, null, null)));
                DATABASE.addAll(records);
                records.clear();
            });
            workbenchController.getEventBus().fireEvent(new DataEvent(DataEvent.SEARCH_READY));
            DevtoolHelper.LOG.info("init search records used time: " + (System.currentTimeMillis() - st));
            DevtoolHelper.LOG.info("searchable records size: " + DATABASE.size());
        }).whenComplete((o, err) -> {
            System.gc();
        });
    }

    @Override
    public void addSearchRecord(SearchRecord record) {
        DATABASE.add(record);
    }

    @Override
    public void addSearchRecords(Collection<? extends SearchRecord> records) {
        DATABASE.addAll(records);
    }

    @Override
    public void search(String searchText, String[] searchWords, BiPredicate<Integer, SearchRecord> handle) {
        int matches = 1;
        for (SearchRecord record : DATABASE) {
            final String searchableString = record.toSearchableString();
            boolean matched = searchableString.contains(searchText);
            if (!matched && null != searchWords)
                matched = StringHelper.containsAny(searchableString, searchWords);
            if (matched && handle.test(matches++, record))
                break;
        }
    }
}
