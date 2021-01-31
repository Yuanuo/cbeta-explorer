package org.appxi.cbeta.explorer.book;

import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.appxi.holder.RawHolder;
import org.appxi.javafx.control.TreeViewExt;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.prefs.UserPrefs;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.model.Chapter;
import org.appxi.util.StringHelper;

import java.util.Objects;
import java.util.function.Predicate;

public class BookBasicView extends VBox {
    public final Accordion accordion;
    public final TitledPane tocsPane, volsPane, infoPane;
    public final TreeViewExt<Chapter> tocsTree, volsTree;

    public BookBasicView() {
        this.tocsPane = new TitledPane("目次", this.tocsTree = new TreeViewExt<>());
        this.volsPane = new TitledPane("卷次", this.volsTree = new TreeViewExt<>());
        this.infoPane = new TitledPane("信息", new Label("Coming soon..."));
        this.accordion = new Accordion(this.tocsPane, this.volsPane, this.infoPane);
        VBox.setVgrow(this.accordion, Priority.ALWAYS);

        this.getChildren().setAll(this.accordion);
    }

    public TreeItem<Chapter> prepareDefaultSelection(CbetaBook book, Chapter chapter) {
        RawHolder<TitledPane> targetPane = new RawHolder<>();
        RawHolder<TreeView<Chapter>> targetTree = new RawHolder<>();
        RawHolder<TreeItem<Chapter>> targetTreeItem = new RawHolder<>();

        if (null != chapter && null != chapter.path) {
            Predicate<TreeItem<Chapter>> findByPath = itm ->
                    chapter.path.equals(itm.getValue().path)
                            && (null == chapter.start || chapter.start.equals(itm.getValue().start));
            detectAvailTarget(targetPane, targetTree, targetTreeItem, findByPath);
        }
        if (null == targetTreeItem.value) {
            final String lastChapterId = UserPrefs.recents.getString(book.id + ".chapter", null);
            if (StringHelper.isNotBlank(lastChapterId)) {
                Predicate<TreeItem<Chapter>> findById = itm -> Objects.equals(lastChapterId, itm.getValue().id);
                detectAvailTarget(targetPane, targetTree, targetTreeItem, findById);
            }
        }
        if (null == targetTreeItem.value) {
            if (this.tocsTree.getRoot().getChildren().size() > 0) {
                targetPane.value = this.tocsPane;
                targetTree.value = this.tocsTree;
            } else {
                targetPane.value = this.volsPane;
                targetTree.value = this.volsTree;
            }
            ObservableList<TreeItem<Chapter>> tmpList = targetTree.value.getRoot().getChildren();
            targetTreeItem.value = tmpList.isEmpty() ? null : tmpList.get(0);
        }

        this.accordion.setExpandedPane(targetPane.value);
        targetTreeItem.value.setExpanded(true);
        targetTree.value.getSelectionModel().select(targetTreeItem.value);

        return targetTreeItem.value;
    }

    private void detectAvailTarget(RawHolder<TitledPane> targetPane,
                                   RawHolder<TreeView<Chapter>> targetTree,
                                   RawHolder<TreeItem<Chapter>> targetTreeItem,
                                   Predicate<TreeItem<Chapter>> findByExpr) {
        targetTreeItem.value = TreeHelper.findFirst(this.tocsTree.getRoot(), findByExpr);
        if (null != targetTreeItem.value) {
            targetPane.value = this.tocsPane;
            targetTree.value = this.tocsTree;
        } else {
            targetTreeItem.value = TreeHelper.findFirst(this.volsTree.getRoot(), findByExpr);
            if (null != targetTreeItem.value) {
                targetPane.value = this.volsPane;
                targetTree.value = this.volsTree;
            }
        }
    }
}
