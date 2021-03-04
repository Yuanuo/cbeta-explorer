package org.appxi.cbeta.explorer.book;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.appxi.cbeta.explorer.model.ChapterTree;
import org.appxi.holder.RawHolder;
import org.appxi.javafx.control.TreeViewEx;
import org.appxi.javafx.control.TreeViewExt;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;
import org.appxi.prefs.UserPrefs;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.model.Chapter;
import org.appxi.util.StringHelper;

import java.util.Objects;
import java.util.function.Predicate;

public class BookBasicController extends WorkbenchSideViewController {
    final BookViewController bookView;

    Accordion accordion;
    TitledPane tocsPane, volsPane, infoPane;
    TreeViewExt<Chapter> tocsTree, volsTree;

    TreeItem<Chapter> selectedTreeItem;

    public BookBasicController(WorkbenchApplication application, BookViewController bookView) {
        super("BOOK-BASIC", "Book-Basic", application);
        this.bookView = bookView;
    }

    @Override
    public Node createToolIconGraphic(boolean sideToolOrElseViewTool) {
        return null;
    }

    @Override
    public void setupInitialize() {
    }

    @Override
    protected void onViewportInitOnce() {
        this.viewport.setTop(null);

        this.tocsPane = new TitledPane("目次", this.tocsTree = new TreeViewExt<>());
        this.volsPane = new TitledPane("卷次", this.volsTree = new TreeViewExt<>());
        this.infoPane = new TitledPane("信息", new Label("Coming soon..."));
        this.accordion = new Accordion(this.tocsPane, this.volsPane, this.infoPane);
        VBox.setVgrow(this.accordion, Priority.ALWAYS);

        this.viewport.setCenter(this.accordion);

        //
        this.tocsTree.setEnterOrDoubleClickAction(this::handleChaptersTreeViewEnterOrDoubleClickAction);
        this.volsTree.setEnterOrDoubleClickAction(this::handleChaptersTreeViewEnterOrDoubleClickAction);
    }

    void handleChaptersTreeViewEnterOrDoubleClickAction(final InputEvent event, final TreeItem<Chapter> treeItem) {
        if (null == treeItem || null != event && !treeItem.isLeaf()) return;
        bookView.openChapter(treeItem.getValue());
    }

    @Override
    public void onViewportShow(boolean firstTime) {
        if (firstTime) {
            // init nav-view
            ChapterTree.parseBookChaptersToTree(bookView.book, this.tocsTree, this.volsTree);
        }
    }

    public Chapter selectChapter(CbetaBook book, Chapter chapter) {
        if (null != selectedTreeItem && selectedTreeItem.getValue() == chapter)
            return selectedTreeItem.getValue();

        RawHolder<TitledPane> targetPane = new RawHolder<>();
        RawHolder<TreeViewEx<Chapter>> targetTree = new RawHolder<>();
        RawHolder<TreeItem<Chapter>> targetTreeItem = new RawHolder<>();

        if (null != chapter && null != chapter.path) {
            Predicate<TreeItem<Chapter>> findByPath = itm ->
                    itm.isLeaf() && chapter.path.equals(itm.getValue().path)
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

        if (null == targetTreeItem.value)
            return null; // avoid NPE

        this.accordion.setExpandedPane(targetPane.value);
        selectedTreeItem = targetTreeItem.value;
        selectedTreeItem.setExpanded(true);
        targetTree.value.getSelectionModel().select(selectedTreeItem);

        return selectedTreeItem.getValue();
    }

    private void detectAvailTarget(RawHolder<TitledPane> targetPane,
                                   RawHolder<TreeViewEx<Chapter>> targetTree,
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
