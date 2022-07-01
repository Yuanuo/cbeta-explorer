package org.appxi.cbeta.app.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.app.AppContext;
import org.appxi.cbeta.app.event.BookEvent;
import org.appxi.javafx.app.search.SearcherEvent;
import org.appxi.javafx.control.TreeViewEx;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchApp;
import org.appxi.javafx.workbench.WorkbenchPane;

import java.util.ArrayList;
import java.util.List;

class BooklistTreeView extends TreeViewEx<Book> {
    final WorkbenchApp app;
    final WorkbenchPane workbench;
    final BooklistExplorer explorer;

    BooklistTreeView(BooklistExplorer explorer) {
        super();
        this.app = explorer.app;
        this.workbench = explorer.workbench;
        this.explorer = explorer;

        this.getStyleClass().add("explorer");
        this.setEnterOrDoubleClickAction((e, t) -> app.eventBus.fireEvent(new BookEvent(BookEvent.OPEN, t.getValue())));
        this.setCellFactory(new BooklistTreeCell());
    }

    @Override
    protected void handleOnContextMenuRequested() {
        final TreeItem<Book> selectedItem = getSelectionModel().getSelectedItem();
        final List<MenuItem> menuItems = new ArrayList<>(16);
        //
        if (null != selectedItem) {
            final Book book = selectedItem.getValue();
            MenuItem menuItem;
            // view
            if (book.id != null && book.path != null) {
                menuItem = new MenuItem("查看");
                menuItem.setGraphic(MaterialIcon.VISIBILITY.graphic());
                menuItem.setOnAction(event1 -> app.eventBus.fireEvent(new BookEvent(BookEvent.OPEN, book)));
                menuItems.add(menuItem);
            }

            // search in this
            menuItem = new MenuItem("从这里搜索");
            menuItem.setGraphic(MaterialIcon.FIND_IN_PAGE.graphic());
            menuItem.setOnAction(event1 -> {
                Book data = book.clone();
                data.title = (null == data.id ? "【目录】" : "【典籍】") + AppContext.hanText(BookLabelStyle.format(data));
                data.path = "nav/" + BooklistProfile.ONE.profile().template().name() + "/" + TreeHelper.path(selectedItem);

                app.eventBus.fireEvent(SearcherEvent.ofSearch(null, data));
            });
            menuItems.add(menuItem);
        }
        setContextMenu(menuItems.isEmpty() ? null : new ContextMenu(menuItems.toArray(new MenuItem[0])));
    }
}
