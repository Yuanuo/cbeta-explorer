package org.appxi.cbeta.explorer.search;

import appxi.cbeta.Book;
import appxi.cbeta.BookHelper;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.book.BooklistProfile;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

class LookupDatabase {
    static final Logger logger = LoggerFactory.getLogger(LookupDatabase.class);
    static final List<LookupData> cachedDatabase = new ArrayList<>(10240);
    static final Map<String, String> cachedAsciiMap = new HashMap<>(10240);

    public void reload() {
        long st = System.currentTimeMillis();
        new Thread(() -> {
            cachedDatabase.clear();
            FxHelper.sleepSilently(500);
            final Collection<Book> books = new ArrayList<>(BooklistProfile.ONE.getManagedBooks());
            books.parallelStream().forEachOrdered(book -> {
                cachedDatabase.add(new LookupData(
                        book.path.startsWith("toc/"),
                        book.id, book.volumes.size(), book.title,
                        null, null,
                        book.authorInfo, null));
                if (null != book.id)
                    cachedAsciiMap.put(book.id, book.id.toLowerCase());
                if (null != book.title)
                    cachedAsciiMap.computeIfAbsent(book.title, AppContext::ascii);
                if (null != book.authorInfo)
                    cachedAsciiMap.computeIfAbsent(book.authorInfo, AppContext::ascii);
            });
            books.parallelStream().forEachOrdered(book -> {
                final Collection<LookupData> items = new ArrayList<>();
                final boolean stdBook = book.path.startsWith("toc/");
                BookHelper.walkTocChaptersByXmlSAX(AppContext.bookcase(), book, (href, text) -> {
                    items.add(new LookupData(
                            stdBook, book.id, book.volumes.size(), book.title,
                            href, text, null, null));
                    if (null != text)
                        cachedAsciiMap.computeIfAbsent(text, AppContext::ascii);
                });
                cachedDatabase.addAll(items);
                items.clear();
            });
            // 统一生成卷数的拼音（目前似乎最大为600卷，但仍生成1-999）
            IntStream.range(1, 999).mapToObj(String::valueOf)
                    .forEach(i -> cachedAsciiMap.put(i.concat("卷"), i.concat("juan")));
            logger.warn("init lookup-in-memory items used time: " + (System.currentTimeMillis() - st));
            logger.warn("init lookup-in-memory items size: " + cachedDatabase.size());
            System.gc();
        }).start();
    }

    static final class LookupData {
        public final boolean stdBook;
        public final String bookId;
        public final int bookVols;
        public final String bookTitle;
        public final String chapter;
        public final String chapterTitle;
        public final String authorInfo;
        public final String extra;
        public final String bookVolsLabel;

        LookupData(boolean stdBook, String bookId, int bookVols, String bookTitle,
                   String chapter, String chapterTitle,
                   String authorInfo, String extra) {
            this.stdBook = stdBook;
            this.bookId = bookId;
            this.bookVols = bookVols;
            this.bookTitle = bookTitle;
            this.chapter = chapter;
            this.chapterTitle = chapterTitle;
            this.authorInfo = authorInfo;
            this.extra = extra;
            this.bookVolsLabel = String.valueOf(bookVols).concat("卷");
        }

        public String toSearchableString() {
            if (null != chapterTitle)
                return StringHelper.concat(chapterTitle, extra);
            return StringHelper.concat(bookId, bookTitle, authorInfo, extra);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            if (null != bookId)
                sb.append(" / ").append(bookId);
            if (null != bookTitle)
                sb.append(" / ").append(bookTitle);
            if (bookVols > 0)
                sb.append("（").append(bookVols).append("卷）");
            if (null != chapterTitle)
                sb.append(" / ").append(chapterTitle);
            if (StringHelper.isNotBlank(authorInfo))
                sb.append(" / ").append(authorInfo);

            final String str = sb.toString();
            return str.length() > 3 ? str.substring(3) : str;
        }
    }
}
