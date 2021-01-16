package org.appxi.cbeta.explorer.model;

import javafx.scene.control.TreeItem;
import org.appxi.tome.cbeta.BookMap;
import org.appxi.tome.cbeta.BookTreeBase;
import org.appxi.tome.cbeta.BookTreeMode;
import org.appxi.tome.cbeta.CbetaBook;

public class BookTree extends BookTreeBase<TreeItem<CbetaBook>> {
    public BookTree(BookMap bookMap, BookTreeMode mode) {
        super(bookMap, new TreeItem<>(null), mode);
    }

    @Override
    protected CbetaBook createCbetaBook() {
        return new CbetaBook();
    }

    @Override
    protected TreeItem<CbetaBook> createTreeItem(TreeItem<CbetaBook> parent, CbetaBook book) {
        final TreeItem<CbetaBook> node = new TreeItem<>(book);
        parent.getChildren().add(node);
        return node;
    }
}
