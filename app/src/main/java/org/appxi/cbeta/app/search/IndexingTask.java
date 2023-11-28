package org.appxi.cbeta.app.search;

import javafx.scene.control.TreeItem;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.BookHelper;
import org.appxi.cbeta.BooksList;
import org.appxi.cbeta.BooksMap;
import org.appxi.cbeta.Tripitaka;
import org.appxi.cbeta.TripitakaMap;
import org.appxi.cbeta.app.AppContext;
import org.appxi.cbeta.app.dao.PiecesRepository;
import org.appxi.cbeta.app.event.ProgressEvent;
import org.appxi.cbeta.app.explorer.BooksProfile;
import org.appxi.event.EventHandler;
import org.appxi.holder.BoolHolder;
import org.appxi.holder.IntHolder;
import org.appxi.javafx.app.AppEvent;
import org.appxi.javafx.app.DesktopApp;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.workbench.WorkbenchApp;
import org.appxi.search.solr.Piece;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

record IndexingTask(WorkbenchApp app) implements Runnable {
    @Override
    public void run() {
        final PiecesRepository repository = AppContext.getBean(PiecesRepository.class);
        if (null == repository) return;

        final BoolHolder breaking = new BoolHolder(false);
        final EventHandler<AppEvent> handleEventToBreaking = event -> breaking.value = true;
        app.eventBus.addEventHandler(AppEvent.STOPPING, handleEventToBreaking);
        app.eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, -1, 1, ""));
        try {
            running(repository, breaking);
        } finally {
            // unbind
            app.eventBus.removeEventHandler(AppEvent.STOPPING, handleEventToBreaking);
            app.eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, 1, 1, ""));
        }
    }

    private void running(PiecesRepository repository, BoolHolder breaking) {
        final BooksProfile.Profile profile = BooksProfile.ONE.profile();
        final TripitakaMap tripitakaMap = new TripitakaMap(AppContext.bookcase());
        final BooksMap booksMap = new BooksMap(tripitakaMap);
        boolean updated = false;

        // 更新默认3个书单
        if (!profile.isManaged()) {
            repository.deleteAll();

            final BooksList<TreeItem<Book>> mainBooks, simpleBooks, advanceBooks;
            mainBooks = new BooksProfile.BooksListTree(booksMap, BooksProfile.Profile.bulei);
            simpleBooks = new BooksProfile.BooksListTree(booksMap, BooksProfile.Profile.simple);
            advanceBooks = new BooksProfile.BooksListTree(booksMap, BooksProfile.Profile.advance);

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
                        BookHelper.prepareBook(book);
                        List<Piece> pieces = IndexingHelper.buildBookToPieces(book);
                        if (!pieces.isEmpty()) {
                            repository.saveAll(pieces);
                        }
                    } catch (Throwable e) {
                        // 忽略过程中的任何错误，因为出错原因可能是程序逻辑或数据异常，除了升级或修复外此过程会一直出现错误，在此中断亦无意义
                        if (!DesktopApp.productionMode)
                            e.printStackTrace();
                    }
                    app.eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, step.value, steps.value, book.title));
                });
                updated = true;
            } catch (RuntimeException e) {
                if (breaking.value)
                    e.printStackTrace();
            }
        } else if (profile.isManaged()) {
            repository.deleteAll();

            final BooksProfile.Profile basedProfile = profile.template();
            final String basedProfileName = basedProfile.name();
            final BooksList<TreeItem<Book>> basedBooks = new BooksProfile.BooksListTree(booksMap, basedProfile);

            // 仅针对已管理的书单做索引
            final BooksList<TreeItem<Book>> mainBooks = new BooksProfile.BooksListFilteredTree(booksMap, profile);
            final IntHolder step = new IntHolder(0);
            final IntHolder steps = new IntHolder(1);
            TreeHelper.walkLeafs(mainBooks.tree(), (treeItem, book) -> steps.value++);
            try {
                final Map<String, String> basedBookPaths = new HashMap<>(512);
                TreeHelper.walkLeafs(basedBooks.tree(), (treeItem, book) -> {
                    if (null == book || null == book.path) return;
                    basedBookPaths.put(book.id, "nav/" + basedProfileName + "/".concat(TreeHelper.path(treeItem)));
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
                        Optional.ofNullable(basedBookPaths.get(book.id)).ifPresent(v -> category.put(v, ""));
                        //
                        BookHelper.prepareBook(book);
                        List<Piece> pieces = IndexingHelper.buildBookToPieces(book);
                        if (!pieces.isEmpty()) {
                            repository.saveAll(pieces);
                        }
                    } catch (Throwable e) {
                        // 忽略过程中的任何错误，因为出错原因可能是程序逻辑或数据异常，除了升级或修复外此过程会一直出现错误，在此中断亦无意义
                        if (!DesktopApp.productionMode)
                            e.printStackTrace();
                    }
                    app.eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, step.value, steps.value, book.title));
                });
                updated = true;
            } catch (RuntimeException e) {
                if (breaking.value)
                    e.printStackTrace();
            }
        }
        if (updated) {
            IndexedManager.saveIndexedVersions();
            app.toast("全文索引已更新！");
        }
    }
}
