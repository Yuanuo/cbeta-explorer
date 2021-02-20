package org.appxi.cbeta.explorer.recent;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import org.appxi.cbeta.explorer.book.BookViewController;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.StatusEvent;
import org.appxi.cbeta.explorer.model.BookList;
import org.appxi.javafx.control.TreeViewEx;
import org.appxi.javafx.control.TreeViewExt;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.prefs.UserPrefs;
import org.appxi.timeago.TimeAgo;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.util.NumberHelper;

import java.util.*;

public class RecentController extends WorkbenchSideViewController {
    private final Map<String, RecentBook> recentBooksMap = new LinkedHashMap<>(128);
    private TreeViewEx<Object> treeView;

    public RecentController(WorkbenchApplication application) {
        super("RECENT", "已读", application);
    }

    @Override
    public Node createToolIconGraphic(Boolean placeInSideViews) {
        return new MaterialIconView(MaterialIcon.HISTORY);
    }

    @Override
    public void setupInitialize() {
        getEventBus().addEventHandler(BookEvent.OPEN, event -> handleToSaveOrUpdateRecentBook(event.book));
        getEventBus().addEventHandler(BookEvent.CLOSE, event -> handleToSaveOrUpdateRecentBook(event.book));
        getEventBus().addEventHandler(ApplicationEvent.STOPPING, event -> {
            saveRecentBooks();
            saveRecentViews();
        });
        loadRecentBooks();
        getEventBus().addEventHandler(StatusEvent.BOOKS_READY, event -> Platform.runLater(this::loadRecentViews));
    }

    private void handleToSaveOrUpdateRecentBook(CbetaBook book) {
        RecentBook item = recentBooksMap.get(book.id);
        if (null == item) {
            item = new RecentBook();
            item.id = book.id;
            item.name = book.title;
            recentBooksMap.put(book.id, item);
        } else item.updateAt = new Date();
    }

    private Preferences createRecentBooks(boolean load) {
        return new PreferencesInProperties(UserPrefs.confDir().resolve(".recentbooks"), load);
    }

    private Preferences createRecentViews(boolean load) {
        return new PreferencesInProperties(UserPrefs.confDir().resolve(".recentviews"), load);
    }

    private void loadRecentBooks() {
        final Preferences recent = createRecentBooks(true);
        recent.getPropertyKeys().forEach(key -> {
            try {
                final RecentBook rBook = new RecentBook();
                rBook.id = key;
                final String[] arr = recent.getString(key, "0|0|").split("\\|", 3);
                rBook.createAt = new Date(NumberHelper.toLong(arr[0], 0L));
                rBook.updateAt = new Date(NumberHelper.toLong(arr[1], 0L));
                rBook.name = arr[2];
                recentBooksMap.put(rBook.id, rBook);
            } catch (Exception e) {
                //
            }
        });
    }

    private void loadRecentViews() {
        final Preferences recent = createRecentViews(true);
        WorkbenchViewController selectedController = null, addedController = null;
        for (String key : recent.getPropertyKeys()) {
            final CbetaBook book = BookList.getById(key);
            if (null == book)
                continue;
            addedController = new BookViewController(book, getApplication());
            if (recent.getBoolean(key, false))
                selectedController = addedController;
            getPrimaryViewport().addWorkbenchViewAsMainView(addedController, true);
            addedController.setupInitialize();
        }
        if (null == selectedController)
            selectedController = addedController;
        if (null != selectedController)
            getPrimaryViewport().selectMainView(selectedController.viewId);
    }

    private void saveRecentBooks() {
        final Preferences recent = createRecentBooks(false);
        this.recentBooksMap.values().forEach(rBook -> {
            final StringBuilder buf = new StringBuilder();
            buf.append(rBook.createAt.getTime()).append('|');
            buf.append(rBook.updateAt.getTime()).append('|');
            buf.append(rBook.name);
            recent.setProperty(rBook.id, buf.toString());
        });
        recent.save();
    }

    private void saveRecentViews() {
        final Preferences recent = createRecentViews(false);
        getPrimaryViewport().getMainViewsTabs().forEach(tab -> {
            if (tab.getUserData() instanceof BookViewController bookView) {
                recent.setProperty(bookView.book.id, tab.isSelected());
            }
        });
        recent.save();
    }

    private TimeAgo.Messages timeAgoMsgs;

    @Override
    protected void onViewportInitOnce() {
    }

    @Override
    public void onViewportShow(boolean firstTime) {
        if (firstTime) {
            this.treeView = new TreeViewExt<>((e, treeItem) -> {
                if (!(treeItem.getValue() instanceof RecentBook rBook))
                    return;
                final CbetaBook book = BookList.getById(rBook.id);
                if (null != book)
                    getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, book));
            });
            this.treeView.setRoot(new TreeItem<>("ROOT"));
            this.viewportVBox.getChildren().add(this.treeView);
        }

        final TreeItem<Object> treeRoot = this.treeView.getRoot();
        final List<RecentBook> recents = new ArrayList<>(this.recentBooksMap.values());
        if (recents.isEmpty()) {
            treeRoot.getChildren().setAll(new TreeItem<>("似乎还没有阅读过"));
        } else {
            recents.sort((v1, v2) -> v2.updateAt.compareTo(v1.updateAt));
            TreeItem<Object> group = null;
            final List<TreeItem<Object>> groups = new ArrayList<>();
            if (null == this.timeAgoMsgs)
                this.timeAgoMsgs = TimeAgo.MessagesBuilder.start().withLocale("zh").build();

            for (RecentBook recent : recents) {
                final String timeAgo = TimeAgo.using(recent.updateAt.getTime(), timeAgoMsgs);
                if (null == group || !Objects.equals(group.getValue(), timeAgo)) {
                    groups.add(group = new TreeItem<>(timeAgo));
                }
                group.getChildren().add(new TreeItem<>(recent));
            }
            treeRoot.getChildren().setAll(groups);
            treeRoot.getChildren().get(0).setExpanded(true);
        }
    }
}
