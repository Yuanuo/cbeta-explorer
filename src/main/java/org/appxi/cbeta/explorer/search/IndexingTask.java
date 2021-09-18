package org.appxi.cbeta.explorer.search;

import javafx.application.Platform;
import javafx.event.EventHandler;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.dao.PiecesRepository;
import org.appxi.cbeta.explorer.event.ProgressEvent;
import org.appxi.holder.IntHolder;
import org.appxi.holder.StringHolder;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.desktop.DesktopApplication;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.prefs.UserPrefs;
import org.appxi.tome.BookHelper;
import org.appxi.tome.cbeta.BookMap;
import org.appxi.tome.cbeta.BookTree;
import org.appxi.tome.cbeta.BookTreeMode;
import org.appxi.tome.cbeta.Tripitaka;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class IndexingTask implements Runnable {
    private final EventHandler<ApplicationEvent> handleEventToBreaking = this::handleEventToBreaking;

    final DesktopApplication application;

    public IndexingTask(DesktopApplication application) {
        this.application = application;
    }

    private boolean breaking;

    @Override
    public void run() {
        if (IndexingHelper.indexedIsValid())
            return; // no need to reindex

        final String indexedVersion = IndexingHelper.indexedVersion();
        final String currentVersion = IndexingHelper.currentVersion();

        final PiecesRepository repository = AppContext.beans().getBean(PiecesRepository.class);
        if (null != indexedVersion) {
            // delete old version
            repository.deleteAllByProjectAndVersion(IndexingHelper.PROJECT, indexedVersion);
        }

        StringHolder indexingPos = new StringHolder(UserPrefs.prefs.getString("indexing.pos", null));
        final String indexingVer = UserPrefs.prefs.getString("indexing.ver", null);
        if (null != indexingVer && !indexingVer.equals(currentVersion)) {
            // delete uncompleted
            repository.deleteAllByProjectAndVersion(IndexingHelper.PROJECT, indexingVer);
            indexingPos.value = null;
        }
        UserPrefs.prefs.setProperty("indexing.ver", currentVersion);

        final BookMap books = new BookMap();
        books.getDataMap();
        final BookTree bookTree = new BookTree(books, BookTreeMode.simple);
        bookTree.getDataTree();
        final BookTree bookTreeAdvance = new BookTree(books, BookTreeMode.advance);
        bookTreeAdvance.getDataTree();
        final BookTree bookTreeCatalog = new BookTree(books, BookTreeMode.catalog);
        bookTreeCatalog.getDataTree();

        // collect max steps for progress
        final IntHolder indexSteps = new IntHolder(0);
        bookTree.getDataTree().traverse((level, node, book) -> indexSteps.value++);
        //
        application.eventBus.addEventHandler(ApplicationEvent.STOPPING, handleEventToBreaking);
        try {
            final IntHolder indexStep = new IntHolder(0);
            bookTree.getDataTree().traverse((level, node, book) -> {
                indexStep.value++;
                if (null == book || null == book.path)
                    return;

                if (breaking)
                    throw new RuntimeException();
                //
                if (null != indexingPos.value) {
                    if (Objects.equals(book.id, indexingPos.value)) {
                        // reset it for next step
                        indexingPos.value = null;
                    }
                    return;
                }

                try {
                    Tripitaka tripitaka = null == book.tripitakaId ? null : books.tripitakaMap.getDataMap().get(book.tripitakaId);
                    IndexingHelper.prepareBook(IndexingHelper.PROJECT, currentVersion, tripitaka, book, false);
                    final Map<String, String> category = book.attr("category");
                    category.put("nav/simple/".concat(IndexingHelper.titledPath(node)), "");
                    Optional.ofNullable(bookTreeAdvance.getDataTree().findFirst(n -> n.value != null && Objects.equals(n.value.id, book.id)))
                            .ifPresent(n -> category.put("nav/advance/".concat(IndexingHelper.titledPath(n)), ""));
                    Optional.ofNullable(bookTreeCatalog.getDataTree().findFirst(n -> n.value != null && Objects.equals(n.value.id, book.id)))
                            .ifPresent(n -> category.put("nav/catalog/".concat(IndexingHelper.titledPath(n)), ""));
                    //
                    BookHelper.prepareBook(book);
                    //
                    repository.saveCbetaBook(book);
                    // release memory?
                    book.chapters.children().clear();
                } catch (Throwable e) {
                    // 忽略过程中的任何错误，因为出错原因可能是程序逻辑或数据异常，除了升级或修复外此过程会一直出现错误，在此中断亦无意义
                    if (!FxHelper.productionMode)
                        e.printStackTrace();
                }

                UserPrefs.prefs.setProperty("indexing.pos", book.id);
                AppContext.app().eventBus.fireEvent(new ProgressEvent(ProgressEvent.INDEXING, indexStep.value, indexSteps.value, book.title));
            });

            //
            IndexingHelper.indexedVersion(currentVersion);
            UserPrefs.prefs.removeProperty("indexing.ver");
            UserPrefs.prefs.removeProperty("indexing.pos");
            // unbind
            application.eventBus.removeEventHandler(ApplicationEvent.STOPPING, handleEventToBreaking);
            //
            Platform.runLater(() -> AppContext.toast("索引成功完成！").showInformation());
        } catch (RuntimeException ignored) {
        }
    }

    private void handleEventToBreaking(ApplicationEvent event) {
        this.breaking = true;
    }
}
