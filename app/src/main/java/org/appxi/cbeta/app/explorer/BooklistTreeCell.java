package org.appxi.cbeta.app.explorer;

import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.app.AppContext;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.util.StringHelper;

import static org.appxi.cbeta.app.AppContext.DND_ITEM;

class BooklistTreeCell implements Callback<TreeView<Book>, TreeCell<Book>> {
    @Override
    public TreeCell<Book> call(TreeView<Book> param) {
        final TreeCell<Book> cell = new TreeCell<>() {
            @Override
            protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    this.setText(null);
                    this.setTooltip(null);
                    this.setGraphic(null);
                    return;
                }
                this.setText(AppContext.hanText(BookLabelStyle.format(item)));
                //
                this.setTooltip(new Tooltip(this.getText().concat(StringHelper.isBlank(item.authorInfo) ? ""
                        : "\n".concat(item.id).concat(" by ").concat(AppContext.hanText(item.authorInfo))
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

        cell.setOnDragDetected(event -> dragDetected(event, cell));
        return cell;
    }

    private void dragDetected(MouseEvent event, TreeCell<Book> treeCell) {
        final TreeItem<Book> draggedItem = treeCell.getTreeItem();

        // root can't be dragged
        if (null == draggedItem || draggedItem.getParent() == null) return;

        Book data = draggedItem.getValue().clone();

        ClipboardContent content = new ClipboardContent();
        content.putString((null == data.id ? "【目录】" : "【典籍】") + AppContext.hanText(BookLabelStyle.format(data)));
        data.path = "nav/" + BooksProfile.ONE.profile().template().name() + "/" + TreeHelper.path(draggedItem);
        content.put(DND_ITEM, data);

        Dragboard db = treeCell.startDragAndDrop(TransferMode.ANY);
        db.setContent(content);
        db.setDragView(treeCell.snapshot(null, null));
        event.consume();
    }
}
