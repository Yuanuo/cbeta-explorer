package org.appxi.cbeta.explorer.search;

import org.appxi.util.StringHelper;

public record LookupItem(boolean stdBook, String bookId, int bookVols, String bookTitle,
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
