package org.appxi.cbeta.explorer.search;

import appxi.cbeta.Book;
import appxi.cbeta.BookHelper;
import appxi.cbeta.BookMap;
import appxi.cbeta.Booklist;
import appxi.cbeta.Tripitaka;
import appxi.cbeta.TripitakaMap;
import javafx.event.EventHandler;
import javafx.scene.control.TreeItem;
import org.appxi.cbeta.explorer.App;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.book.BooklistProfile;
import org.appxi.cbeta.explorer.dao.PiecesRepository;
import org.appxi.cbeta.explorer.event.ProgressEvent;
import org.appxi.holder.BoolHolder;
import org.appxi.holder.IntHolder;
import org.appxi.javafx.app.AppEvent;
import org.appxi.javafx.app.BaseApp;
import org.appxi.javafx.app.DesktopApp;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.prefs.UserPrefs;
import org.appxi.search.solr.Piece;
import org.appxi.util.StringHelper;
import org.springframework.data.solr.core.SolrTemplate;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

record IndexingTask(BaseApp app) implements Runnable {
    @Override
    public void run() {
        final PiecesRepository repository = AppContext.getBean(PiecesRepository.class);
        if (null == repository) return;

        final BoolHolder breaking = new BoolHolder(false);
        final EventHandler<AppEvent> handleEventToBreaking = event -> breaking.value = true;
        app.eventBus.addEventHandler(AppEvent.STOPPING, handleEventToBreaking);
        App.app().eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, -1, 1, ""));
        try {
            running(repository, breaking);
        } finally {
            // unbind
            app.eventBus.removeEventHandler(AppEvent.STOPPING, handleEventToBreaking);
            App.app().eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, 1, 1, ""));
        }
    }

    private void running(PiecesRepository repository, BoolHolder breaking) {
        final BooklistProfile.Profile profile = AppContext.profile();
        final TripitakaMap tripitakaMap = new TripitakaMap(AppContext.bookcase());
        final BookMap bookMap = new BookMap(tripitakaMap);
        boolean updated = false;
        if (IndexedManager.isBookcaseIndexable()) {
            repository.deleteAll();

            final Booklist<TreeItem<Book>> buleiBooklist, simpleBooklist, advanceBooklist;
            buleiBooklist = new BooklistProfile.BooklistTree(bookMap, BooklistProfile.Profile.bulei);
            simpleBooklist = new BooklistProfile.BooklistTree(bookMap, BooklistProfile.Profile.simple);
            advanceBooklist = new BooklistProfile.BooklistTree(bookMap, BooklistProfile.Profile.advance);

            final IntHolder step = new IntHolder(0);
            final IntHolder steps = new IntHolder(1);
            TreeHelper.walkLeafs(buleiBooklist.tree(), (treeItem, book) -> steps.value++);
            try {
                final Map<String, String> simpleBookPaths = new HashMap<>(512);
                TreeHelper.walkLeafs(simpleBooklist.tree(), (treeItem, book) -> {
                    if (null == book || null == book.path) return;
                    simpleBookPaths.put(book.id, "nav/simple/".concat(TreeHelper.path(treeItem)));
                });
                final Map<String, String> advanceBookPaths = new HashMap<>(512);
                TreeHelper.walkLeafs(advanceBooklist.tree(), (treeItem, book) -> {
                    if (null == book || null == book.path) return;
                    advanceBookPaths.put(book.id, "nav/advance/".concat(TreeHelper.path(treeItem)));
                });
                // 根据当前书单处理，如果是默认书单（非自定义）则不需要做过滤处理
                final HashSet<String> managedBooks = new HashSet<>(512);
                final boolean profileManaged = profile.isManaged();
                if (profileManaged) {
                    Booklist<TreeItem<Book>> booklist = new BooklistProfile.BooklistFilteredTree(bookMap, profile);
                    TreeHelper.walkLeafs(booklist.tree(), (treeItem, book) -> managedBooks.add(book.id));
                }

                Preferences cacheProfiles = new PreferencesInProperties(UserPrefs.confDir().resolve(".profiles"), false);
                String cacheProfilesStr = StringHelper.join(",",
                        new HashSet<>(Arrays.asList("bulei", "simple", "advance", profile.name())));
                TreeHelper.walkLeafs(buleiBooklist.tree(), (treeItem, book) -> {
                    step.value++;
                    if (null == book || null == book.path) return;
                    if (breaking.value) throw new RuntimeException();
                    //
                    try {
                        Tripitaka tripitaka = null == book.tripitakaId ? null : tripitakaMap.data().get(book.tripitakaId);
                        IndexingHelper.prepareBookBasic(null, null, tripitaka, book);
                        IndexingHelper.prepareBookChapters(book);
                        IndexingHelper.prepareBookContents(book, false);
                        //
                        final Map<String, String> category = book.attr("category");
                        category.put("nav/bulei/".concat(TreeHelper.path(treeItem)), "");
                        Optional.ofNullable(simpleBookPaths.get(book.id)).ifPresent(v -> category.put(v, ""));
                        Optional.ofNullable(advanceBookPaths.get(book.id)).ifPresent(v -> category.put(v, ""));

                        category.put("profile/bulei", "");
                        category.put("profile/simple", "");
                        category.put("profile/advance", "");
                        // 未在当前书单中，不能关联
                        if (profileManaged && managedBooks.contains(book.id))
                            category.put("profile/".concat(profile.name()), "");
                        //
                        BookHelper.prepareBook(book);
                        List<Piece> pieces = IndexingHelper.buildBookToPieces(book);
                        if (!pieces.isEmpty()) {
                            repository.saveAll(pieces);
                            pieces.forEach(p -> cacheProfiles.setProperty(p.id, book.id.concat("|").concat(cacheProfilesStr)));
                        }
                    } catch (Throwable e) {
                        // 忽略过程中的任何错误，因为出错原因可能是程序逻辑或数据异常，除了升级或修复外此过程会一直出现错误，在此中断亦无意义
                        if (!DesktopApp.productionMode)
                            e.printStackTrace();
                    }
                    App.app().eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, step.value, steps.value, book.title));
                });
                cacheProfiles.save();
                updated = true;
            } catch (RuntimeException e) {
                if (breaking.value)
                    e.printStackTrace();
            }
        } else if (IndexedManager.isBooklistIndexable()) {
            SolrTemplate solrTemplate = AppContext.getBean(SolrTemplate.class);
            if (null == solrTemplate) return;
            App.app().eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, -1, 1, "正在更新。。。"));

            final BooklistProfile.BooklistFilteredTree booklist;
            booklist = new BooklistProfile.BooklistFilteredTree(bookMap, AppContext.profile());
            try {
                final HashSet<String> managedBooks = new HashSet<>(512);
                TreeHelper.walkLeafs(booklist.tree(), (treeItem, book) -> managedBooks.add(book.id));

                final Preferences cachedProfiles = new PreferencesInProperties(UserPrefs.confDir().resolve(".profiles"));
                final List<Map.Entry<String, Map.Entry<String, Object>[]>> updates = new ArrayList<>(512);
                cachedProfiles.getPropertyKeys().forEach(id -> {
                    String[] info = cachedProfiles.getString(id, "").split("\\|", 2);
                    if (info.length != 2) return;
                    String bookId = info[0];
                    Set<String> projects = new HashSet<>(Arrays.asList(info[1].split(",")));
                    boolean changed;
                    if (managedBooks.contains(bookId)) changed = projects.add(profile.name());
                    else changed = projects.remove(profile.name());
                    if (changed) {
                        cachedProfiles.setProperty(id, bookId.concat("|").concat(StringHelper.join(",", projects)));
//                        repository.updateInAtomicSet(solrTemplate, Piece.REPO, id, new AbstractMap.SimpleEntry<>("project_ss", projects));
                        updates.add(new AbstractMap.SimpleEntry<String, Map.Entry<String, Object>[]>(
                                id, new AbstractMap.SimpleEntry[]{
                                new AbstractMap.SimpleEntry<String, Object>("project_ss", projects)
                        }));
                    }
                });
                repository.updateInAtomicsSet(solrTemplate, Piece.REPO, updates.toArray(new Map.Entry[0]));
                cachedProfiles.save();
                updated = true;
            } catch (RuntimeException e) {
                if (breaking.value)
                    e.printStackTrace();
            }
        }
        if (updated) {
            IndexedManager.saveIndexedVersions();
            AppContext.toast("全文索引已更新！");
        }
    }
}
