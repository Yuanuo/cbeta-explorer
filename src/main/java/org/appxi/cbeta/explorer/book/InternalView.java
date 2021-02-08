package org.appxi.cbeta.explorer.book;

import javafx.scene.layout.BorderPane;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.util.ext.Attributes;

abstract class InternalView extends Attributes {
    static final Object AK_FIRST_TIME = new Object();
    protected BorderPane viewport;

    final BookViewController bookView;
    final CbetaBook book;

    protected InternalView(BookViewController bookView) {
        this.bookView = bookView;
        this.book = bookView.book;
    }

    public final BorderPane getViewport() {
        if (null == this.viewport) {
            this.viewport = new BorderPane();
            //
            onViewportInitOnce();
        }
        return viewport;
    }

    protected abstract void onViewportInitOnce();

    public final void onViewportInit() {
        boolean firstTime = !this.hasAttr(AK_FIRST_TIME);
        if (firstTime)
            this.attr(AK_FIRST_TIME, true);
        onViewportInit(firstTime);
    }

    protected abstract void onViewportInit(boolean firstTime);
}
