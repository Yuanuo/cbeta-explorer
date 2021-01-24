package org.appxi.cbeta.explorer.search;

import org.appxi.util.StringHelper;

public record SearchRecord(boolean stdBook, String bookId, String bookTitle,
                           String chapter, String chapterTitle,
                           String authorInfo, String content) {

    public String toSearchableString() {
        if (null != chapterTitle)
            return StringHelper.concat(chapterTitle, content);
        return StringHelper.concat(bookId, bookTitle, authorInfo, content);
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
