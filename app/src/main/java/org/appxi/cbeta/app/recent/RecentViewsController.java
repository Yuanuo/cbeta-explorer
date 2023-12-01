package org.appxi.cbeta.app.recent;

import org.appxi.cbeta.Book;
import org.appxi.cbeta.app.DataApp;
import org.appxi.cbeta.app.event.GenericEvent;
import org.appxi.cbeta.app.reader.BookXmlReader;
import org.appxi.holder.RawHolder;
import org.appxi.javafx.app.AppEvent;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPart;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RecentViewsController extends WorkbenchPartController {
    final DataApp dataApp;

    public RecentViewsController(WorkbenchPane workbench, DataApp dataApp) {
        super(workbench);
        this.dataApp = dataApp;
    }

    @Override
    public void postConstruct() {
        app.eventBus.addEventHandler(AppEvent.STOPPING, event -> saveRecentViews());
        //
        app.eventBus.addEventHandler(GenericEvent.PROFILE_READY, event -> FxHelper.runThread(30, () -> {
            final Preferences recent = createRecentViews(true);
            final RawHolder<WorkbenchPart.MainView> swapRecentViewSelected = new RawHolder<>();
            final List<WorkbenchPart.MainView> swapRecentViews = new ArrayList<>();
            WorkbenchPart.MainView addedController = null;
            for (String key : recent.getPropertyKeys()) {
                final Book book = dataApp.dataContext.getBook(key);
                // 如果此书不存在于当前书单，则需要移除（如果存在）
                if (null == book) {
                    Optional.ofNullable(workbench.findMainView(key)).ifPresent(workbench.mainViews::closeTabs);
                    continue;
                }
                //
                if (workbench.existsMainView(book.id))
                    continue;
                //
                addedController = new BookXmlReader(book, workbench, dataApp);
                if (recent.getBoolean(key, false)) {
                    swapRecentViewSelected.value = addedController;
                }
                swapRecentViews.add(addedController);
            }
            if (!swapRecentViews.isEmpty()) {
                for (WorkbenchPart.MainView viewController : swapRecentViews) {
                    workbench.addWorkbenchPartAsMainView(viewController, true);
                }
                if (null == swapRecentViewSelected.value) {
                    swapRecentViewSelected.value = addedController;
                }
            }
            if (!swapRecentViews.isEmpty()) {
                if (null != swapRecentViewSelected.value) {
                    workbench.selectMainView(swapRecentViewSelected.value.id().get());
                }
            }
        }));
    }

    private Preferences createRecentViews(boolean load) {
        return new PreferencesInProperties(app.workspace.resolve(".recentviews"), load);
    }

    private void saveRecentViews() {
        final Preferences recent = createRecentViews(false);
        workbench.mainViews.getTabs().forEach(tab -> {
            if (tab.getUserData() instanceof BookXmlReader bookXmlReader) {
                recent.setProperty(bookXmlReader.book.id, tab.isSelected());
            }
        });
        recent.save();
    }
}
