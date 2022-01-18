package org.appxi.cbeta.explorer.recent;

import appxi.cbeta.Book;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.book.BookXmlViewer;
import org.appxi.cbeta.explorer.event.GenericEvent;
import org.appxi.holder.RawHolder;
import org.appxi.javafx.app.AppEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchViewController;
import org.appxi.javafx.workbench.views.WorkbenchMainViewController;
import org.appxi.javafx.workbench.views.WorkbenchNoneViewController;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.prefs.UserPrefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecentViewsController extends WorkbenchNoneViewController {
    public RecentViewsController(WorkbenchPane workbench) {
        super("recentViews", workbench);
    }

    @Override
    public void initialize() {
        app.eventBus.addEventHandler(AppEvent.STOPPING, event -> saveRecentViews());
        //
        app.eventBus.addEventHandler(GenericEvent.PROFILE_READY, event -> FxHelper.runLater(() -> {
            final Preferences recent = createRecentViews(true);
            final RawHolder<WorkbenchMainViewController> swapRecentViewSelected = new RawHolder<>();
            final List<WorkbenchMainViewController> swapRecentViews = new ArrayList<>();
            WorkbenchMainViewController addedController = null;
            for (String key : recent.getPropertyKeys()) {
                final Book book = AppContext.booklistProfile.getBook(key);
                // 如果此书不存在于当前书单，则需要移除（如果存在）
                if (null == book) {
                    Optional.ofNullable(workbench.findMainViewTab(key)).ifPresent(workbench.mainViews::closeTabs);
                    continue;
                }
                //
                if (workbench.existsMainView(book.id))
                    continue;
                //
                addedController = new BookXmlViewer(book, workbench);
                if (recent.getBoolean(key, false))
                    swapRecentViewSelected.value = addedController;
                swapRecentViews.add(addedController);
            }
            if (!swapRecentViews.isEmpty()) {
                for (WorkbenchMainViewController viewController : swapRecentViews) {
                    workbench.addWorkbenchViewAsMainView(viewController, true);
                }
                if (null == swapRecentViewSelected.value)
                    swapRecentViewSelected.value = addedController;
            }
            if (!swapRecentViews.isEmpty()) {
                swapRecentViews.forEach(WorkbenchViewController::initialize);
                if (null != swapRecentViewSelected.value)
                    workbench.selectMainView(swapRecentViewSelected.value.viewId.get());
            }
        }));
    }

    private Preferences createRecentViews(boolean load) {
        return new PreferencesInProperties(UserPrefs.confDir().resolve(".recentviews"), load);
    }

    private void saveRecentViews() {
        final Preferences recent = createRecentViews(false);
        workbench.mainViews.getTabs().forEach(tab -> {
            if (tab.getUserData() instanceof BookXmlViewer bookView) {
                recent.setProperty(bookView.book.id, tab.isSelected());
            }
        });
        recent.save();
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
    }
}
