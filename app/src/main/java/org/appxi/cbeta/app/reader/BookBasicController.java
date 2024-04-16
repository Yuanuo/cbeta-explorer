package org.appxi.cbeta.app.reader;

import javafx.collections.ObservableList;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.input.InputEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.appxi.book.Chapter;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.app.explorer.ChapterTree;
import org.appxi.holder.RawHolder;
import org.appxi.javafx.control.TreeViewEx;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.util.DigestHelper;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.HanLang;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Predicate;

public class BookBasicController extends WorkbenchPartController.SideView {
    final BookXmlReader reader;

    Accordion accordion;
    TitledPane tocPane, volPane;
    TreeViewEx<Chapter> tocTree, volTree;

    TreeItem<Chapter> selectedTreeItem;

    public BookBasicController(WorkbenchPane workbench, BookXmlReader reader) {
        super(workbench);

        this.id.set("BOOK-BASIC");

        this.reader = reader;
    }

    @Override
    public void postConstruct() {
    }

    @Override
    protected void createViewport(BorderPane viewport) {
        super.createViewport(viewport);
        //
        viewport.setTop(null);

        this.tocPane = new TitledPane("目次", this.tocTree = new TreeViewEx<>());
        this.volPane = new TitledPane("卷次", this.volTree = new TreeViewEx<>());
        this.accordion = new Accordion(this.tocPane, this.volPane);
        VBox.setVgrow(this.accordion, Priority.ALWAYS);

        viewport.setCenter(this.accordion);

        //
        this.tocTree.setEnterOrDoubleClickAction((event, treeItem) -> onTreeItemAction(event, treeItem, true));
        this.volTree.setEnterOrDoubleClickAction((event, treeItem) -> onTreeItemAction(event, treeItem, false));

        //
        this.tocTree.setCellFactory(v -> new TreeCell<>() {
            @Override
            protected void updateItem(Chapter item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    this.setText(null);
                    return;
                }
                this.setText(reader.dataApp.hanTextToShow(item.title));
            }
        });
        // 当显示汉字类型改变时需要同步更新treeView
        app.eventBus.addEventHandler(HanLang.Event.CHANGED, event -> this.tocTree.refresh());
    }

    void onTreeItemAction(final InputEvent event, final TreeItem<Chapter> treeItem, boolean toc) {
        if (null == treeItem || null != event && !treeItem.isLeaf()) return;
        selectedTreeItem = treeItem;

        Chapter treeItemValue = treeItem.getValue();
        // 通过双击章节目录打开时，总是定位到该标题处
        if (toc && null != treeItemValue.anchor) {
            treeItemValue.attr("anchor", treeItemValue.anchor);
        }
        reader.navigate(treeItemValue);
    }

    @Override
    public void activeViewport(boolean firstTime) {
        if (firstTime) {
            // init nav-view
            ChapterTree.parseBookChaptersToTree(reader.book, this.tocTree, this.volTree);
        }
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
                TreeHelper.filterLeafs(tocTree.getRoot(), (treeItem, itemValue) -> {
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
                // 未找到时，假设在最后一个章节
                if (null == found.value) {
                    found.value = prev.value;
                }
                if (null != found.value) {
                    chapter.path = found.value.path;
                }
                chapter.attr("position.selector", pathExpr);
            } else { // 按指定卷号处理
                final String pathExpr = path.concat(".xml");
                Chapter found = TreeHelper.findFirstValue(this.volTree.getRoot(),
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

            //
            if (null == targetTreeItem.value) {
                findByPath = itm -> itm.isLeaf() && chapter.path.equals(itm.getValue().path);
                detectAvailTarget(targetPane, targetTree, targetTreeItem, findByPath);
            }
        }
        if (null == targetTreeItem.value) {
            final String lastChapterId = app.recents.getString(book.id + ".chapter", null);
            if (StringHelper.isNotBlank(lastChapterId)) {
                final Predicate<TreeItem<Chapter>> findById;
                if (lastChapterId.contains("-"))
                    findById = itm -> {
                        if (null == itm.getValue() || null == itm.getValue().path)
                            return false;
                        final String oldStyleId = DigestHelper.crc32c(
                                itm.getValue().path.concat(null == itm.getValue().anchor ? "" : itm.getValue().anchor.toString()),
                                itm.getValue().title,
                                StandardCharsets.UTF_8);
                        return lastChapterId.equals(book.id.concat("-").concat(oldStyleId));
                    };
                else findById = itm -> Objects.equals(lastChapterId, itm.getValue().id);
                detectAvailTarget(targetPane, targetTree, targetTreeItem, findById);
            }
        }
        if (null == targetTreeItem.value) {
            if (this.tocTree.getRoot().getChildren().size() > 0) {
                targetPane.value = this.tocPane;
                targetTree.value = this.tocTree;
            } else {
                targetPane.value = this.volPane;
                targetTree.value = this.volTree;
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
        TreeViewEx<Chapter>[] detectTargets = new TreeViewEx[2];
        if (accordion.getExpandedPane() != null && accordion.getExpandedPane().getContent() instanceof TreeViewEx tree) {
            detectTargets[0] = tree;
        } else {
            detectTargets[0] = this.tocTree;
        }
        detectTargets[1] = detectTargets[0] != tocTree ? tocTree : volTree;
        //
        for (TreeViewEx<Chapter> detectTarget : detectTargets) {
            targetTreeItem.value = TreeHelper.findFirst(detectTarget.getRoot(), findByExpr);
            if (null != targetTreeItem.value) {
                targetPane.value = detectTarget == tocTree ? tocPane : volPane;
                targetTree.value = detectTarget;
                return;
            }
        }
    }
}
