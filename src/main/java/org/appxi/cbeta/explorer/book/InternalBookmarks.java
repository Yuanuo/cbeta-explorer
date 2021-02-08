package org.appxi.cbeta.explorer.book;

import org.appxi.cbeta.explorer.dao.Bookdata;
import org.appxi.cbeta.explorer.dao.BookdataType;

class InternalBookmarks extends InternalBookdata {

    protected InternalBookmarks(BookViewController bookView) {
        super(bookView, BookdataType.bookmark);
    }

    protected String buildLabelText(Bookdata item) {
        return "...".concat(item.data).concat("...");
    }

    @Override
    protected String getBookdataAnchorInfo() {
        return bookView.webViewer.executeScript("getBookmarkAnchorInfo()");
    }

    @Override
    protected boolean isDataTextEditable() {
        return false;
    }
}
