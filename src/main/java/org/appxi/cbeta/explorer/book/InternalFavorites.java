package org.appxi.cbeta.explorer.book;

import org.appxi.cbeta.explorer.dao.BookdataType;

class InternalFavorites extends InternalBookdata {

    protected InternalFavorites(BookViewController bookView) {
        super(bookView, BookdataType.favorite);
    }

    @Override
    protected String getBookdataAnchorInfo() {
        return bookView.webViewer.executeScript("getFavoriteAnchorInfo()");
    }
}
