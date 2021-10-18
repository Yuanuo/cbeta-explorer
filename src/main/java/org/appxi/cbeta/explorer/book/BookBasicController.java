package org.appxi.cbeta.explorer.book;

import appxi.cbeta.Book;
import appxi.cbeta.Chapter;
import javafx.collections.ObservableList;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.appxi.holder.RawHolder;
import org.appxi.javafx.control.TreeViewEx;
import org.appxi.javafx.control.TreeViewExt;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.DigestHelper;
import org.appxi.util.StringHelper;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Predicate;

public class BookBasicController extends WorkbenchSideViewController {
    final BookXmlViewer bookView;

    Accordion accordion;
    TitledPane tocsPane, volsPane, infoPane;
    TreeViewExt<Chapter> tocsTree, volsTree;

    TreeItem<Chapter> selectedTreeItem;

    public BookBasicController(WorkbenchApplication application, BookXmlViewer bookView) {
        super("BOOK-BASIC", application);
        this.bookView = bookView;
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
    public void onViewportShowing(boolean firstTime) {
        if (firstTime) {
            // init nav-view
            ChapterTree.parseBookChaptersToTree(bookView.book, this.tocsTree, this.volsTree);
        }
    }

    @Override
    public void onViewportHiding() {
    }

    public Chapter selectChapter(Book book, Chapter chapter) {
        if (null != selectedTreeItem && selectedTreeItem.getValue() == chapter)
            return selectedTreeItem.getValue();

        RawHolder<TitledPane> targetPane = new RawHolder<>();
        RawHolder<TreeViewEx<Chapter>> targetTree = new RawHolder<>();
        RawHolder<TreeItem<Chapter>> targetTreeItem = new RawHolder<>();

        // 此处用于根据#命令时指定的行号或卷号找到真正的（Volume）path，然后再在后面匹配到树形菜单上的对应位置
        if (null != chapter && "#".equals(chapter.id) && null != chapter.path) {
            String path = chapter.path;
//            chapter.path = null;// reset
            // 按指定行号处理
            if (path.startsWith("p")) {
                final String pathExpr = "#".concat(path);
                RawHolder<Chapter> found = new RawHolder<>(), prev = new RawHolder<>();
                TreeHelper.filterLeafs(tocsTree.getRoot(), (treeItem, itemValue) -> {
                    if (null == itemValue || null == itemValue.anchor) return false;
                    int compared = itemValue.anchor.toString().compareToIgnoreCase(pathExpr);
                    if (compared > 0) {
                        found.value = null != prev.value ? prev.value : itemValue;
                        return true;
                    } else if (compared == 0) {
                        found.value = itemValue;
                        return true;
                    }
                    prev.value = itemValue;
                    return false;
                });
                if (null != found.value) {
                    chapter.path = found.value.path;
                }
                chapter.attr("position.selector", pathExpr);
            } else { // 按指定卷号处理
                final String pathExpr = path.concat(".xml");
                Chapter found = TreeHelper.findFirstValue(this.volsTree.getRoot(),
                        itm -> null != itm.getValue() && null != itm.getValue().path && itm.getValue().path.endsWith(pathExpr));
                if (null != found) {
                    chapter.path = found.path;
                }
            }
        }

        if (null != chapter && null != chapter.path) {
            Predicate<TreeItem<Chapter>> findByPath = itm ->
                    itm.isLeaf() && chapter.path.equals(itm.getValue().path)
                            && (null == chapter.anchor || chapter.anchor.equals(itm.getValue().anchor));
            detectAvailTarget(targetPane, targetTree, targetTreeItem, findByPath);
        }
        if (null == targetTreeItem.value) {
            final String lastChapterId = UserPrefs.recents.getString(book.id + ".chapter", null);
            if (StringHelper.isNotBlank(lastChapterId)) {
                final Predicate<TreeItem<Chapter>> findById;
                if (lastChapterId.contains("-"))
                    findById = itm -> {
                        if (null == itm.getValue() || null == itm.getValue().path)
                            return false;
                        final String oldStyleId = DigestHelper.crc32c(
                                itm.getValue().path.concat(null == itm.getValue().anchor ? "" : itm.getValue().anchor.toString()),
                                itm.getValue().title,
                                Charset.forName("GBK"));
                        return lastChapterId.equals(book.id.concat("-").concat(oldStyleId));
                    };
                else findById = itm -> Objects.equals(lastChapterId, itm.getValue().id);
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
