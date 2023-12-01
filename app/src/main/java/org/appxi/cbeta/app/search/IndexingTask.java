package org.appxi.cbeta.app.search;

import javafx.scene.control.TreeItem;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.BookHelper;
import org.appxi.cbeta.BookList;
import org.appxi.cbeta.BookMap;
import org.appxi.cbeta.Profile;
import org.appxi.cbeta.Tripitaka;
import org.appxi.cbeta.TripitakaMap;
import org.appxi.cbeta.app.AppContext;
import org.appxi.cbeta.app.DataApp;
import org.appxi.cbeta.app.DataContext;
import org.appxi.cbeta.app.SpringConfig;
import org.appxi.cbeta.app.dao.PiecesRepository;
import org.appxi.cbeta.app.event.ProgressEvent;
import org.appxi.event.EventHandler;
import org.appxi.holder.BoolHolder;
import org.appxi.holder.IntHolder;
import org.appxi.javafx.app.AppEvent;
import org.appxi.javafx.app.BaseApp;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.search.solr.Piece;
import org.appxi.util.StringHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

record IndexingTask(DataApp dataApp) implements Runnable {
    @Override
    public void run() {
        final PiecesRepository repository = SpringConfig.getBean(PiecesRepository.class);
        if (null == repository) return;

        final BoolHolder breaking = new BoolHolder(false);
        final EventHandler<AppEvent> handleEventToBreaking = event -> breaking.value = true;
        dataApp.eventBus.addEventHandler(AppEvent.STOPPING, handleEventToBreaking);
        dataApp.eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, -1, 1, ""));
        try {
            running(repository, breaking);
        } finally {
            // unbind
            dataApp.eventBus.removeEventHandler(AppEvent.STOPPING, handleEventToBreaking);
            dataApp.eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, 1, 1, ""));
        }
    }

    private void running(PiecesRepository repository, BoolHolder breaking) {
        final Profile profile = dataApp.profile;
        final TripitakaMap tripitakaMap = AppContext.tripitakaMap();
        final BookMap bookMap = AppContext.bookMap();
        boolean updated = false;

        // 更新默认书单
        if (!profile.isManaged() && StringHelper.indexOf(profile.id(), "bulei", "simple", "advance")) {
            repository.deleteAllByProjects(profile.id());

            final BookList<TreeItem<Book>> mainBooks, simpleBooks, advanceBooks;
            mainBooks = new DataContext.BookListTree(bookMap, dataApp.basedApp.profileMgr.getProfile("bulei"));
            simpleBooks = new DataContext.BookListTree(bookMap, dataApp.basedApp.profileMgr.getProfile("simple"));
            advanceBooks = new DataContext.BookListTree(bookMap, dataApp.basedApp.profileMgr.getProfile("advance"));

            final IntHolder step = new IntHolder(0);
            final IntHolder steps = new IntHolder(1);
            TreeHelper.walkLeafs(mainBooks.tree(), (treeItem, book) -> steps.value++);
            try {
                final Map<String, String> simpleBookPaths = new HashMap<>(512);
                TreeHelper.walkLeafs(simpleBooks.tree(), (treeItem, book) -> {
                    if (null == book || null == book.path) return;
                    simpleBookPaths.put(book.id, "nav/simple/".concat(TreeHelper.path(treeItem)));
                });
                final Map<String, String> advanceBookPaths = new HashMap<>(512);
                TreeHelper.walkLeafs(advanceBooks.tree(), (treeItem, book) -> {
                    if (null == book || null == book.path) return;
                    advanceBookPaths.put(book.id, "nav/advance/".concat(TreeHelper.path(treeItem)));
                });

                TreeHelper.walkLeafs(mainBooks.tree(), (treeItem, book) -> {
                    step.value++;
                    if (null == book || null == book.path) return;
                    if (breaking.value) throw new RuntimeException();
                    //
                    try {
                        Tripitaka tripitaka = null == book.library ? null : tripitakaMap.data().get(book.library);
                        IndexingHelper.prepareBookBasic(null, null, tripitaka, book);
                        IndexingHelper.prepareBookChapters(book);
                        IndexingHelper.prepareBookContents(book, false);
                        //
                        final Map<String, String> category = book.attr("category");
                        category.put("nav/bulei/".concat(TreeHelper.path(treeItem)), "");
                        Optional.ofNullable(simpleBookPaths.get(book.id)).ifPresent(v -> category.put(v, ""));
                        Optional.ofNullable(advanceBookPaths.get(book.id)).ifPresent(v -> category.put(v, ""));
                        //
                        category.put("project/bulei", "");
                        category.put("project/simple", "");
                        category.put("project/advance", "");
                        //
                        BookHelper.prepareBook(book);
                        List<Piece> pieces = IndexingHelper.buildBookToPieces(book);
                        if (!pieces.isEmpty()) {
                            repository.saveAll(pieces);
                        }
                    } catch (Throwable e) {
                        // 忽略过程中的任何错误，因为出错原因可能是程序逻辑或数据异常，除了升级或修复外此过程会一直出现错误，在此中断亦无意义
                        if (!BaseApp.productionMode)
                            e.printStackTrace();
                    }
                    dataApp.eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, step.value, steps.value, book.title));
                });
                updated = true;
            } catch (RuntimeException e) {
                if (breaking.value)
                    e.printStackTrace();
            }
        } else if (!profile.isManaged()) {
            repository.deleteAllByProjects(profile.id());

            final BookList<TreeItem<Book>> mainBooks;
            mainBooks = new DataContext.BookListTree(bookMap, profile);

            final IntHolder step = new IntHolder(0);
            final IntHolder steps = new IntHolder(1);
            TreeHelper.walkLeafs(mainBooks.tree(), (treeItem, book) -> steps.value++);
            try {
                final String projectPath = "project/" + profile.id();
                final String navPath = "nav/" + profile.id() + "/";
                TreeHelper.walkLeafs(mainBooks.tree(), (treeItem, book) -> {
                    step.value++;
                    if (null == book || null == book.path) return;
                    if (breaking.value) throw new RuntimeException();
                    //
                    try {
                        Tripitaka tripitaka = null == book.library ? null : tripitakaMap.data().get(book.library);
                        IndexingHelper.prepareBookBasic(null, null, tripitaka, book);
                        IndexingHelper.prepareBookChapters(book);
                        IndexingHelper.prepareBookContents(book, false);
                        //
                        final Map<String, String> category = book.attr("category");
                        category.put(navPath + TreeHelper.path(treeItem), "");
                        //
                        category.put(projectPath, "");
                        //
                        BookHelper.prepareBook(book);
                        List<Piece> pieces = IndexingHelper.buildBookToPieces(book);
                        if (!pieces.isEmpty()) {
                            repository.saveAll(pieces);
                        }
                    } catch (Throwable e) {
                        // 忽略过程中的任何错误，因为出错原因可能是程序逻辑或数据异常，除了升级或修复外此过程会一直出现错误，在此中断亦无意义
                        if (!BaseApp.productionMode)
                            e.printStackTrace();
                    }
                    dataApp.eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, step.value, steps.value, book.title));
                });
                updated = true;
            } catch (RuntimeException e) {
                if (breaking.value)
                    e.printStackTrace();
            }
        } else if (profile.isManaged()) {
            repository.deleteAllByProjects(profile.id());

            final Profile basedProfile = dataApp.basedApp.profileMgr.getProfile(profile.template());
            final String basedProfileId = basedProfile.id();
            final BookList<TreeItem<Book>> basedBooks = new DataContext.BookListTree(bookMap, basedProfile);

            // 仅针对已管理的书单做索引
            final BookList<TreeItem<Book>> mainBooks = new DataContext.BookListFilteredTree(bookMap, profile);
            final IntHolder step = new IntHolder(0);
            final IntHolder steps = new IntHolder(1);
            TreeHelper.walkLeafs(mainBooks.tree(), (treeItem, book) -> steps.value++);
            try {
                final String projectPath = "project/" + profile.id();
                final String navPath = "nav/" + profile.id() + "/";
                TreeHelper.walkLeafs(mainBooks.tree(), (treeItem, book) -> {
                    step.value++;
                    if (null == book || null == book.path) return;
                    if (breaking.value) throw new RuntimeException();
                    //
                    try {
                        Tripitaka tripitaka = null == book.library ? null : tripitakaMap.data().get(book.library);
                        IndexingHelper.prepareBookBasic(null, null, tripitaka, book);
                        IndexingHelper.prepareBookChapters(book);
                        IndexingHelper.prepareBookContents(book, false);
                        //
                        final Map<String, String> category = book.attr("category");
                        category.put(navPath + TreeHelper.path(treeItem), "");
                        //
                        category.put(projectPath, "");
                        //
                        BookHelper.prepareBook(book);
                        List<Piece> pieces = IndexingHelper.buildBookToPieces(book);
                        if (!pieces.isEmpty()) {
                            repository.saveAll(pieces);
                        }
                    } catch (Throwable e) {
                        // 忽略过程中的任何错误，因为出错原因可能是程序逻辑或数据异常，除了升级或修复外此过程会一直出现错误，在此中断亦无意义
                        if (!BaseApp.productionMode)
                            e.printStackTrace();
                    }
                    dataApp.eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, step.value, steps.value, book.title));
                });
                updated = true;
            } catch (RuntimeException e) {
                if (breaking.value)
                    e.printStackTrace();
            }
        }
        if (updated) {
            dataApp.indexedManager.saveIndexedVersions();
            dataApp.toast("全文索引已更新！");
        }
    }
}
