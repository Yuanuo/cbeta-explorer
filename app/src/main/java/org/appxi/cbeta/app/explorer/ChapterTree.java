package org.appxi.cbeta.app.explorer;

import javafx.scene.control.TreeItem;
import org.appxi.book.Chapter;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.ChapterTreeParser;
import org.appxi.cbeta.app.AppContext;
import org.appxi.javafx.control.TreeViewEx;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.util.ext.Node;

import java.util.Comparator;

public class ChapterTree extends ChapterTreeParser<TreeItem<Chapter>> {
    public ChapterTree(Book book) {
        super(AppContext.bookcase(), book, new TreeItem<>(new Chapter()), new TreeItem<>(new Chapter()), null);
    }

    @Override
    protected TreeItem<Chapter> createTreeItem(TreeItem<Chapter> parent, Chapter chapter) {
        final TreeItem<Chapter> node = new TreeItem<>(chapter);
        parent.getChildren().add(node);
        return node;
    }

    @Override
    protected boolean existsItemInTree(String path, TreeItem<Chapter> tree) {
        return null != TreeHelper.findFirst(tree, item -> path.equals(item.getValue().path));
    }

    @Override
    protected void sortChildrenOfTree(TreeItem<Chapter> tree) {
        tree.getChildren().sort(Comparator.comparing(item -> item.getValue().path));
    }

    public static void parseBookChaptersToTree(Book book, TreeViewEx<Chapter> tocTree, TreeViewEx<Chapter> volTree) {
        final ChapterTree chapterTree = new ChapterTree(book);
        tocTree.setRoot(chapterTree.getTocChapters());
        volTree.setRoot(chapterTree.getVolChapters());
        // 由于原数据中卷次列表并不一定连续，其中有修复填补的部分，在其显示文字上加上“暂无”可能对用户更友好
        TreeHelper.filterLeafs(volTree.getRoot(), (treeItem, itemValue) -> {
            if (itemValue != null && "title".equals(itemValue.type))
                itemValue.title = itemValue.title.concat("（暂无）");
            return false;
        });
    }


    public static void buildBookChaptersToTree(Book book, TreeViewEx<Chapter> tocTree, TreeViewEx<Chapter> volTree) {
        final org.appxi.cbeta.ChapterTree chapterTree = org.appxi.cbeta.ChapterTree.getOrInitBookChapters(AppContext.bookcase(), book);
        buildBookChaptersToTree(chapterTree.getTocChapters(), tocTree);
        buildBookChaptersToTree(chapterTree.getVolChapters(), volTree);
    }

    public static void buildBookChaptersToTree(Node<Chapter> chapters, TreeViewEx<Chapter> tree) {
        chapters.relinkChildren();
        TreeItem<Chapter> treeRoot = new TreeItem<>(chapters.value);
        walkAndBuildChapterTree(treeRoot, chapters);
        tree.setRoot(treeRoot);
    }

    private static void walkAndBuildChapterTree(TreeItem<Chapter> parentTree, Node<Chapter> parentVal) {
        for (Node<Chapter> childVal : parentVal.children()) {
            TreeItem<Chapter> childTree = new TreeItem<>(childVal.value);
            parentTree.getChildren().add(childTree);

            if (childVal.hasChildren())
                walkAndBuildChapterTree(childTree, childVal);
        }
    }
}
