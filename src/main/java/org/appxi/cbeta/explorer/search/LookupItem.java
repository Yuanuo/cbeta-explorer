package org.appxi.cbeta.explorer.search;

import org.appxi.util.StringHelper;

public record LookupItem(boolean stdBook, String bookId, String bookTitle,
                         String chapter, String chapterTitle,
                         String authorInfo, String extra) {

    public String toSearchableString() {
        if (null != chapterTitle)
            return StringHelper.concat(chapterTitle, extra);
        return StringHelper.concat(bookId, bookTitle, authorInfo, extra);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(bookId).append(" / ").append(bookTitle);
        if (null != chapterTitle)
            sb.append(" / ").append(chapterTitle);
        if (null != authorInfo)
            sb.append(" / ").append(authorInfo);
        return sb.toString();
    }
}
