package org.appxi.cbeta.explorer.recent;

import appxi.cbeta.Book;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.GenericEvent;
import org.appxi.javafx.app.AppEvent;
import org.appxi.javafx.control.TreeViewEx;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.prefs.UserPrefs;
import org.appxi.timeago.TimeAgo;
import org.appxi.util.NumberHelper;
import org.appxi.util.StringHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RecentItemsController extends WorkbenchSideViewController {
    private final Map<String, RecentBook> recentBooksMap = new LinkedHashMap<>(128);
    private TreeViewEx<Object> treeView;

    public RecentItemsController(WorkbenchPane workbench) {
        super("RECENT", workbench);
        this.setTitles("已读");
        this.viewGraphic.set(MaterialIcon.HISTORY.graphic());
    }

    @Override
    public void initialize() {
        app.eventBus.addEventHandler(BookEvent.OPEN, event -> handleToSaveOrUpdateRecentBook(event.book));
        app.eventBus.addEventHandler(BookEvent.CLOSE, event -> handleToSaveOrUpdateRecentBook(event.book));
        app.eventBus.addEventHandler(AppEvent.STOPPING, event -> saveRecentBooks());
        if (!(UserPrefs.recents instanceof PreferencesInProperties))
            UserPrefs.recents = new PreferencesInProperties(UserPrefs.confDir().resolve(".recents"));
        loadRecentBooks();
    }

    private void handleToSaveOrUpdateRecentBook(Book book) {
        RecentBook item = recentBooksMap.get(book.id);
        if (null == item) {
            item = new RecentBook();
            item.id = book.id;
            item.name = book.title;
            recentBooksMap.put(book.id, item);
            AppContext.recentBooks.setProperty(book.id, "");
        } else item.updateAt = new Date();
    }

    private Preferences createRecentBooks(boolean load) {
        return new PreferencesInProperties(UserPrefs.confDir().resolve(".recentbooks"), load);
    }

    private void loadRecentBooks() {
        final Preferences recent = AppContext.recentBooks = createRecentBooks(true);
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

    @Override
    protected void onViewportInitOnce() {
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        if (firstTime) {
            this.treeView = new TreeViewEx<>();
            this.treeView.setRoot(new TreeItem<>("ROOT"));
            this.treeView.setEnterOrDoubleClickAction((e, treeItem) -> {
                if (!(treeItem.getValue() instanceof RecentBook rBook))
                    return;
                final Book book = AppContext.booklistProfile.getBook(rBook.id);
                if (null != book)
                    app.eventBus.fireEvent(new BookEvent(BookEvent.OPEN, book));
            });
            this.treeView.setCellFactory(v -> new TreeCell<>() {
                Object updatedItem;

                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        updatedItem = null;
                        this.setText(null);
                        this.setGraphic(null);
                        return;
                    }
                    if (item == updatedItem)
                        return;//
                    updatedItem = item;
                    String text = null;
                    if (item instanceof String str) {
                        text = str;
                        this.setGraphic(getTreeItem().isExpanded() ? MaterialIcon.FOLDER_OPEN.graphic() : MaterialIcon.FOLDER.graphic());
                    } else if (item instanceof RecentBook rBook) {
                        this.setGraphic(null);
                        final Book book = AppContext.booklistProfile.getBook(rBook.id);
                        if (null != book) {
                            text = book.title;
                            if (null != book.path && book.volumes.size() > 0) {
                                text = StringHelper.concat(text, "（", book.volumes.size(), "卷）");
                            }
                            text = AppContext.displayText(text);
                        } else {
                            text = rBook.id;
                        }
                    } else {
                        text = item.toString();
                        this.setGraphic(null);
                    }
                    this.setText(text);
                }
            });
            this.viewport.setCenter(this.treeView);
            //
            app.eventBus.addEventHandler(GenericEvent.DISPLAY_HAN_CHANGED, event -> this.treeView.refresh());
        }

        final TreeItem<Object> treeRoot = this.treeView.getRoot();
        final List<RecentBook> recents = new ArrayList<>(this.recentBooksMap.values());
        if (recents.isEmpty()) {
            treeRoot.getChildren().setAll(new TreeItem<>("似乎还没有阅读过"));
        } else {
            recents.sort((v1, v2) -> v2.updateAt.compareTo(v1.updateAt));
            TreeItem<Object> group = null;
            final List<TreeItem<Object>> groups = new ArrayList<>();

            for (RecentBook recent : recents) {
                final String timeAgo = TimeAgo.using(recent.updateAt.getTime(), AppContext.timeAgoI18N());
                if (null == group || !Objects.equals(group.getValue(), timeAgo)) {
                    groups.add(group = new TreeItem<>(timeAgo));
                }
                group.getChildren().add(new TreeItem<>(recent));
            }
            treeRoot.getChildren().setAll(groups);
            treeRoot.getChildren().get(0).setExpanded(true);
        }
    }

    @Override
    public void onViewportHiding() {
    }
}