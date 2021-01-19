package org.appxi.cbeta.explorer.recent;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.DataEvent;
import org.appxi.cbeta.explorer.reader.BookviewController;
import org.appxi.cbeta.explorer.search.SearchHelper;
import org.appxi.cbeta.explorer.workbench.WorkbenchWorkpartControllerExt;
import org.appxi.holder.RawHolder;
import org.appxi.javafx.control.TreeViewEx;
import org.appxi.javafx.control.TreeViewExt;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.prefs.UserPrefs;
import org.appxi.timeago.TimeAgo;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.util.NumberHelper;

import java.util.*;

public class RecentController extends WorkbenchWorkpartControllerExt {
    private final Map<String, RecentBook> recentBooksMap = new LinkedHashMap<>(128);
    private TreeViewEx<Object> treeView;

    public RecentController() {
        super("RECENT", "近期阅读");
    }

    @Override
    public Label getViewpartInfo() {
        return new Label(this.viewName, new FontAwesomeIconView(FontAwesomeIcon.HISTORY));
    }

    @Override
    public void setupInitialize() {
        getEventBus().addEventHandler(BookEvent.OPEN, event -> {
            final CbetaBook book = event.book;
            RecentBook item = recentBooksMap.get(book.id);
            if (null == item) {
                item = new RecentBook();
                item.id = book.id;
                item.name = book.title;
                recentBooksMap.put(book.id, item);
            } else item.updateAt = new Date();
        });
        getEventBus().addEventHandler(ApplicationEvent.STOPPING, event -> {
            saveRecentBooks();
            saveRecentViews();
        });
        loadRecentBooks();
        getEventBus().addEventHandler(DataEvent.BOOKS_READY, event -> Platform.runLater(this::loadRecentViews));
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
        final RawHolder<BookviewController> selectedController = new RawHolder<>();
        recent.getPropertyKeys().forEach(key -> {
            final CbetaBook book = SearchHelper.searchById(key);
            if (null == book)
                return;
            final BookviewController controller = new BookviewController(book);
            if (recent.getBoolean(key, false))
                selectedController.value = controller;
            getWorkbenchController().addWorkbenchOpenpartController(controller, true);
            controller.setupInitialize();
        });
        if (null != selectedController.value) {
            getWorkbenchViewport().selectOpenpart(selectedController.value.viewId);
        }
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
        getWorkbenchViewport().getOpentools().forEach(tab -> {
            if (tab.getUserData() instanceof BookviewController controller)
                recent.setProperty(controller.book.id, tab.isSelected());
        });
        recent.save();
    }

    private TimeAgo.Messages timeAgoMsgs;

    @Override
    public void onViewportSelected(boolean firstTime) {
        if (firstTime) {
            this.treeView = new TreeViewExt<>((e, treeItem) -> {
                if (!(treeItem.getValue() instanceof RecentBook rBook))
                    return;
                final CbetaBook book = SearchHelper.searchById(rBook.id);
                if (null != book)
                    getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, book));
            });
            this.treeView.setRoot(new TreeItem<>("ROOT"));
            this.viewpartVbox.getChildren().add(this.treeView);
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
                final String timeago = TimeAgo.using(recent.updateAt.getTime(), timeAgoMsgs);
                if (null == group || !Objects.equals(group.getValue(), timeago)) {
                    groups.add(group = new TreeItem<>(timeago));
                }
                group.getChildren().add(new TreeItem<>(recent));
            }
            treeRoot.getChildren().setAll(groups);
            treeRoot.getChildren().get(0).setExpanded(true);
        }
    }
}
