package org.appxi.cbeta.explorer.book;

import appxi.cbeta.Book;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.util.Callback;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.util.StringHelper;

class BooklistTreeCell implements Callback<TreeView<Book>, TreeCell<Book>> {
    @Override
    public TreeCell<Book> call(TreeView<Book> param) {
        return new TreeCell<>() {
            Book updatedItem;

            @Override
            protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    updatedItem = null;
                    this.setText(null);
                    this.setTooltip(null);
                    this.setGraphic(null);
                    return;
                }
                if (item == updatedItem)
                    return;//
                updatedItem = item;
                String text = item.title;
                if (null != item.path && item.volumes.size() > 0) {
                    text = StringHelper.concat(text, "（", item.volumes.size(), "卷）");
                }
                this.setText(AppContext.displayText(text));
                //
                this.setTooltip(new Tooltip(this.getText().concat(StringHelper.isBlank(item.authorInfo) ? ""
                        : "\n".concat(item.id).concat(" by ").concat(AppContext.displayText(item.authorInfo))
                )));
                //
                this.setGraphic((this.getTreeItem().isLeaf() ? MaterialIcon.ARTICLE
                        : (this.getTreeItem().isExpanded() ? MaterialIcon.FOLDER_OPEN : MaterialIcon.FOLDER)).graphic());
                //
                this.getStyleClass().remove("visited");
                if (null != item.path && null != AppContext.recentBooks.getProperty(item.id)) {
                    this.getStyleClass().add("visited");
                }
            }
        };
    }
}
