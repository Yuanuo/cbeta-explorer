package org.appxi.cbeta.app.recent;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.app.AppContext;
import org.appxi.cbeta.app.event.BookEvent;
import org.appxi.cbeta.app.event.GenericEvent;
import org.appxi.cbeta.app.explorer.BookLabelStyle;
import org.appxi.cbeta.app.explorer.BooksProfile;
import org.appxi.javafx.app.AppEvent;
import org.appxi.javafx.control.TreeViewEx;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.prefs.UserPrefs;
import org.appxi.timeago.TimeAgo;
import org.appxi.util.NumberHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class RecentItemsController extends WorkbenchPartController.SideView {
    private final Map<String, RecentBook> recentBooksMap = new LinkedHashMap<>(128);
    private TreeViewEx<Object> treeView;

    public RecentItemsController(WorkbenchPane workbench) {
        super(workbench);

        this.id.set("RECENT");
        this.tooltip.set("已读");
        this.graphic.set(MaterialIcon.HISTORY.graphic());
    }

    @Override
    public void postConstruct() {
        app.eventBus.addEventHandler(BookEvent.OPEN, event -> handleToSaveOrUpdateRecentBook(event.book));
        app.eventBus.addEventHandler(BookEvent.CLOSE, event -> handleToSaveOrUpdateRecentBook(event.book));
        app.eventBus.addEventHandler(BookEvent.VIEW, event -> handleToSaveOrUpdateRecentBook(event.book));
        app.eventBus.addEventHandler(AppEvent.STOPPING, event -> saveRecentBooks());
        if (!(UserPrefs.recents instanceof PreferencesInProperties)) {
            UserPrefs.recents = new PreferencesInProperties(UserPrefs.confDir().resolve(".recents"));
        }
        loadRecentBooks();
        // 当书名显示风格改变时需要同步更新treeView
        app.eventBus.addEventHandler(GenericEvent.BOOK_LABEL_STYLED, event -> Optional.ofNullable(this.treeView).ifPresent(TreeView::refresh));
    }

    private void handleToSaveOrUpdateRecentBook(Book book) {
        RecentBook item = recentBooksMap.get(book.id);
        if (null == item) {
            item = new RecentBook();
            item.id = book.id;
            item.name = book.title;
            recentBooksMap.put(book.id, item);
        } else item.updateAt = new Date();
    }

    private void loadRecentBooks() {
        final Preferences recent = AppContext.recentBooks = new PreferencesInProperties(UserPrefs.confDir().resolve(".recentbooks"), true);
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
        final Preferences recent = new PreferencesInProperties(UserPrefs.confDir().resolve(".recentbooks"), false);
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
    public void activeViewport(boolean firstTime) {
        if (firstTime) {
            this.treeView = new TreeViewEx<>();
            this.treeView.setRoot(new TreeItem<>("ROOT"));
            this.treeView.setEnterOrDoubleClickAction((e, treeItem) -> {
                if (!(treeItem.getValue() instanceof RecentBook rBook))
                    return;
                final Book book = BooksProfile.ONE.getBook(rBook.id);
                if (null != book)
                    app.eventBus.fireEvent(new BookEvent(BookEvent.OPEN, book));
            });
            this.treeView.setCellFactory(v -> new TreeCell<>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        this.setText(null);
                        this.setGraphic(null);
                        return;
                    }
                    String text;
                    if (item instanceof String str) {
                        text = str;
                        this.setGraphic(getTreeItem().isExpanded() ? MaterialIcon.FOLDER_OPEN.graphic() : MaterialIcon.FOLDER.graphic());
                    } else if (item instanceof RecentBook rBook) {
                        this.setGraphic(null);
                        final Book book = BooksProfile.ONE.getBook(rBook.id);
                        if (null != book) {
                            text = AppContext.hanText(BookLabelStyle.format(book));
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
            this.getViewport().setCenter(this.treeView);
            //
            app.eventBus.addEventHandler(GenericEvent.HAN_LANG_CHANGED, event -> this.treeView.refresh());
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
                final String timeAgo = TimeAgo.using(recent.updateAt.getTime(), timeAgoI18N());
                if (null == group || !Objects.equals(group.getValue(), timeAgo)) {
                    groups.add(group = new TreeItem<>(timeAgo));
                }
                group.getChildren().add(new TreeItem<>(recent));
            }
            treeRoot.getChildren().setAll(groups);
            treeRoot.getChildren().get(0).setExpanded(true);
        }
    }

    private static TimeAgo.Messages timeAgoI18N;
    private static final Object _initTimeAgoI18N = new Object();

    public static TimeAgo.Messages timeAgoI18N() {
        if (null == timeAgoI18N)
            synchronized (_initTimeAgoI18N) {
                if (null == timeAgoI18N)
                    timeAgoI18N = TimeAgo.MessagesBuilder.start().withLocale("zh").build();
            }
        return timeAgoI18N;
    }
}
