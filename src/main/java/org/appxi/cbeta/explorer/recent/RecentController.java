package org.appxi.cbeta.explorer.recent;

import appxi.cbeta.Book;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.DisplayHelper;
import org.appxi.cbeta.explorer.book.BookXmlViewer;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.GenericEvent;
import org.appxi.holder.RawHolder;
import org.appxi.javafx.control.TreeViewEx;
import org.appxi.javafx.control.TreeViewExt;
import org.appxi.javafx.desktop.ApplicationEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.iconfont.MaterialIcon;
import org.appxi.javafx.views.ViewController;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchMainViewController;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.prefs.UserPrefs;
import org.appxi.timeago.TimeAgo;
import org.appxi.util.NumberHelper;
import org.appxi.util.StringHelper;

import java.util.*;

public class RecentController extends WorkbenchSideViewController {
    private final Map<String, RecentBook> recentBooksMap = new LinkedHashMap<>(128);
    private TreeViewEx<Object> treeView;

    public RecentController(WorkbenchApplication application) {
        super("RECENT", application);
        this.setTitles("已读");
        this.viewIcon.set(MaterialIcon.HISTORY.iconView());
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
        //
        getEventBus().addEventHandler(GenericEvent.PROFILE_READY, event -> FxHelper.runLater(() -> {
            if (!(UserPrefs.recents instanceof PreferencesInProperties))
                UserPrefs.recents = new PreferencesInProperties(UserPrefs.confDir().resolve(".recents"));
            final Preferences recent = createRecentViews(true);
            final RawHolder<WorkbenchMainViewController> swapRecentViewSelected = new RawHolder<>();
            final List<WorkbenchMainViewController> swapRecentViews = new ArrayList<>();
            WorkbenchMainViewController addedController = null;
            for (String key : recent.getPropertyKeys()) {
                final Book book = AppContext.booklistProfile.getBook(key);
                // 如果此书不存在于当前书单，则需要移除（如果存在）
                if (null == book) {
                    Optional.ofNullable(getPrimaryViewport().findMainViewTab(key))
                            .ifPresent(tab -> getPrimaryViewport().removeMainView(tab));
                    continue;
                }
                //
                if (getPrimaryViewport().existsMainView(book.id))
                    continue;
                //
                addedController = new BookXmlViewer(book, getApplication());
                if (recent.getBoolean(key, false))
                    swapRecentViewSelected.value = addedController;
                swapRecentViews.add(addedController);
            }
            if (!swapRecentViews.isEmpty()) {
                for (WorkbenchMainViewController viewController : swapRecentViews) {
                    getPrimaryViewport().addWorkbenchViewAsMainView(viewController, true);
                }
                if (null == swapRecentViewSelected.value)
                    swapRecentViewSelected.value = addedController;
            }
            if (!swapRecentViews.isEmpty()) {
                swapRecentViews.forEach(ViewController::setupInitialize);
                if (null != swapRecentViewSelected.value)
                    FxHelper.runLater(() -> getPrimaryViewport().selectMainView(swapRecentViewSelected.value.viewId.get()));
            }
        }));
        getEventBus().addEventHandler(GenericEvent.DISPLAY_HAN_CHANGED,
                event -> Optional.ofNullable(this.treeView).ifPresent(TreeView::refresh));
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

    private Preferences createRecentViews(boolean load) {
        return new PreferencesInProperties(UserPrefs.confDir().resolve(".recentviews"), load);
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

    private void saveRecentViews() {
        final Preferences recent = createRecentViews(false);
        getPrimaryViewport().getMainViewsTabs().forEach(tab -> {
            if (tab.getUserData() instanceof BookXmlViewer bookView) {
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
    public void onViewportShowing(boolean firstTime) {
        if (firstTime) {
            this.treeView = new TreeViewExt<>((e, treeItem) -> {
                if (!(treeItem.getValue() instanceof RecentBook rBook))
                    return;
                final Book book = AppContext.booklistProfile.getBook(rBook.id);
                if (null != book)
                    getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, book));
            });
            this.treeView.setRoot(new TreeItem<>("ROOT"));
            this.treeView.setCellFactory(v -> new TreeCell<>() {
                Object updatedItem;

                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        updatedItem = null;
                        this.setText(null);
                        return;
                    }
                    if (item == updatedItem)
                        return;//
                    updatedItem = item;
                    String text = null;
                    if (item instanceof String str) {
                        text = str;
                    } else if (item instanceof RecentBook rBook) {
                        final Book book = AppContext.booklistProfile.getBook(rBook.id);
                        if (null != book) {
                            text = book.title;
                            if (null != book.path && book.volumes.size() > 0) {
                                text = StringHelper.concat(text, "（", book.volumes.size(), "卷）");
                            }
                            text = DisplayHelper.displayText(text);
                        } else {
                            text = rBook.id;
                        }
                    } else {
                        text = item.toString();
                    }
                    this.setText(text);
                }
            });
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

    @Override
    public void onViewportHiding() {
    }
}
