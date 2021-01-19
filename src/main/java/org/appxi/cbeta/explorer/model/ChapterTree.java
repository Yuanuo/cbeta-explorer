package org.appxi.cbeta.explorer.model;

import javafx.scene.control.TreeItem;
import org.appxi.javafx.control.TreeViewExt;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.cbeta.ChapterTreeParser;
import org.appxi.tome.model.Chapter;
import org.appxi.util.ext.Node;

import java.util.Comparator;

public class ChapterTree extends ChapterTreeParser<TreeItem<Chapter>> {
    public ChapterTree(CbetaBook book) {
        super(book, new TreeItem<>(new Chapter()), new TreeItem<>(new Chapter()), null);
    }

    public ChapterTree(CbetaBook book, TreeItem<Chapter> tocChapters, TreeItem<Chapter> volChapters) {
        super(book, tocChapters, volChapters, null);
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

    public static void parseBookChaptersToTree(CbetaBook book, TreeViewExt<Chapter> tocTree, TreeViewExt<Chapter> volTree) {
        final ChapterTree chapterTree = new ChapterTree(book);
        tocTree.setRoot(chapterTree.getTocChapters());
        volTree.setRoot(chapterTree.getVolChapters());
    }


    public static void buildBookChaptersToTree(CbetaBook book, TreeViewExt<Chapter> tocTree, TreeViewExt<Chapter> volTree) {
        final org.appxi.tome.cbeta.ChapterTree chapterTree = org.appxi.tome.cbeta.ChapterTree.getOrInitBookChapters(book);
        buildBookChaptersToTree(chapterTree.getTocChapters(), tocTree);
        buildBookChaptersToTree(chapterTree.getVolChapters(), volTree);
    }

    public static void buildBookChaptersToTree(Node<Chapter> chapters, TreeViewExt<Chapter> tree) {
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
