package org.appxi.cbeta.explorer.search;

import org.appxi.cbeta.explorer.CbetaxHelper;
import org.appxi.cbeta.explorer.event.DataEvent;
import org.appxi.javafx.workbench.WorkbenchController;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.cbeta.CbetaHelper;
import org.appxi.util.DevtoolHelper;
import org.appxi.util.ext.FiPredicateX3;

import java.util.*;
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
            Collection<CbetaBook> books = CbetaxHelper.books.getDataMap().values();
            books.parallelStream().forEachOrdered(book -> {
                DATABASE.add(new SearchRecord(book.path.startsWith("toc/"), book.id, book.title, null, null, book.authorInfo, null));
            });
            books.parallelStream().forEachOrdered(book -> {
                final Collection<SearchRecord> records = new ArrayList<>();
                final boolean stdBook = book.path.startsWith("toc/");
                CbetaHelper.walkTocChaptersByXmlSAX(book, (href, text) ->
                        records.add(new SearchRecord(stdBook, book.id, book.title, href, text, null, null)));
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

    static final List<FiPredicateX3<SearchRecord, String, String[]>> searchFilters = List.of(
            (data, text, words) -> {
                if (data.stdBook() && null == data.chapterTitle()) {
                    if (data.bookId().contains(text)) return true;
                }
                return false;
            }
            , (data, text, words) -> {
                if (data.stdBook() && null == data.chapterTitle()) {
                    if (data.bookTitle().startsWith(text)) return true;
                    if (data.authorInfo().startsWith(text)) return true;
                    if (null != words)
                        for (String word : words) {
                            if (data.bookTitle().startsWith(word)) return true;
                            if (data.authorInfo().startsWith(word)) return true;
                        }
                }
                return false;
            }
            , (data, text, words) -> {
                if (data.stdBook() && null == data.chapterTitle()) {
                    if (data.bookTitle().contains(text)) return true;
                    if (data.authorInfo().contains(text)) return true;
                    if (null != words)
                        for (String word : words) {
                            if (data.bookTitle().contains(word)) return true;
                            if (data.authorInfo().contains(word)) return true;
                        }
                }
                return false;
            }
            , (data, text, words) -> {
                if (data.stdBook() && null != data.chapterTitle()) {
                    if (data.chapterTitle().contains(text)) return true;
                    if (null != words)
                        for (String word : words) {
                            if (data.chapterTitle().contains(word)) return true;
                        }
                }
                return false;
            }
            , (data, text, words) -> {
                String dataContent = data.toSearchableString();
                if (dataContent.contains(text)) return true;
                if (null != words)
                    for (String word : words) {
                        if (dataContent.contains(word)) return true;
                    }
                return false;
            }
    );

    @Override
    public void search(String searchText, String[] searchWords, BiPredicate<Integer, SearchRecord> handle) {
        int matches = 1;
        int filterIdx = searchText.matches("^[A-Za-z0-9].*") ? 0 : 1;
        final Set<Integer> matchedIdxSet = new HashSet<>();
        SearchRecord data;
        for (; filterIdx < searchFilters.size(); filterIdx++) {
            FiPredicateX3<SearchRecord, String, String[]> searchFilter = searchFilters.get(filterIdx);
            for (int dataIdx = 0; dataIdx < DATABASE.size(); dataIdx++) {
                if (matchedIdxSet.contains(dataIdx))
                    continue;
                data = DATABASE.get(dataIdx);
                boolean matched = searchFilter.test(data, searchText, searchWords);
                if (matched) {
                    matchedIdxSet.add(dataIdx);
                    if (handle.test(matches++, data))
                        return;
                }
            }
        }
    }
}
