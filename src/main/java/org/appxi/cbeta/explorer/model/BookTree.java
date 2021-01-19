package org.appxi.cbeta.explorer.model;

import javafx.scene.control.TreeItem;
import org.appxi.tome.cbeta.BookMap;
import org.appxi.tome.cbeta.BookTreeBase;
import org.appxi.tome.cbeta.BookTreeMode;
import org.appxi.tome.cbeta.CbetaBook;

import java.util.List;

public class BookTree extends BookTreeBase<TreeItem<CbetaBook>> {
    public BookTree(BookMap books, BookTreeMode mode) {
        super(books, new TreeItem<>(null), mode);
    }

    @Override
    protected TreeItem<CbetaBook> createTreeItem(CbetaBook itemValue) {
        return new TreeItem<>(itemValue);
    }

    @Override
    protected void setTreeChildren(TreeItem<CbetaBook> parent, List<TreeItem<CbetaBook>> children) {
        parent.getChildren().addAll(children);
    }
}
