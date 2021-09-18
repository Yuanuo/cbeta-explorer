package org.appxi.cbeta.explorer.search;

import org.appxi.cbeta.explorer.model.BookList;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.cbeta.CbetaHelper;
import org.appxi.util.ext.FiPredicateX3;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;

class LookupInMemory implements LookupProvider {
    static final List<LookupItem> DATABASE = new ArrayList<>(10240);

    void setupInitialize() {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            long st = System.currentTimeMillis();
            final Collection<CbetaBook> books = new ArrayList<>(BookList.books.getDataMap().values());
            books.parallelStream().forEachOrdered(book -> DATABASE.add(new LookupItem(
                    book.path.startsWith("toc/"),
                    book.id, book.numberVols, book.title,
                    null, null,
                    book.authorInfo, null)));
            books.parallelStream().forEachOrdered(book -> {
                final Collection<LookupItem> items = new ArrayList<>();
                final boolean stdBook = book.path.startsWith("toc/");
                CbetaHelper.walkTocChaptersByXmlSAX(book, (href, text) -> items.add(new LookupItem(
                        stdBook, book.id, book.numberVols, book.title,
                        href, text,
                        null, null)));
                DATABASE.addAll(items);
                items.clear();
            });
//            DevtoolHelper.LOG.info("init lookup items used time: " + (System.currentTimeMillis() - st));
//            DevtoolHelper.LOG.info("lookup items size: " + DATABASE.size());
        }).whenComplete((o, err) -> {
            if (null != err)
                err.printStackTrace();
            System.gc();
        });
    }

    static final List<FiPredicateX3<LookupItem, String, String[]>> filters = List.of(
            (data, text, words) -> {
                if (data.stdBook() && null == data.chapterTitle()) {
                    return data.bookId().contains(text);
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
    public void search(String searchText, String[] searchWords, BiPredicate<Integer, LookupItem> handle) {
        int matches = 1;
        int filterIdx = searchText.matches("^[A-Za-z0-9].*") ? 0 : 1;
        final Set<Integer> matchedIdxSet = new HashSet<>();
        LookupItem data;
        for (; filterIdx < filters.size(); filterIdx++) {
            FiPredicateX3<LookupItem, String, String[]> filter = filters.get(filterIdx);
            for (int dataIdx = 0; dataIdx < DATABASE.size(); dataIdx++) {
                if (matchedIdxSet.contains(dataIdx))
                    continue;
                data = DATABASE.get(dataIdx);
                boolean matched = filter.test(data, searchText, searchWords);
                if (matched) {
                    matchedIdxSet.add(dataIdx);
                    if (handle.test(matches++, data))
                        return;
                }
            }
        }
    }
}
