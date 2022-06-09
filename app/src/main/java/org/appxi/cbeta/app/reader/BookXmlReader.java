package org.appxi.cbeta.app.reader;

import org.appxi.cbeta.Book;
import org.appxi.cbeta.Chapter;
import org.appxi.cbeta.app.AppContext;
import org.appxi.cbeta.app.event.BookEvent;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.util.StringHelper;

public class BookXmlReader extends WorkbenchPartController.MainView {
    public final Book book;

    private BookXmlViewer viewer;

    public BookXmlReader(Book book, WorkbenchPane workbench) {
        super(workbench);

        this.book = book;

        this.id.set(book.id);
        this.appTitle.unbind();
        this.setTitles(null);
    }

    void setTitles(Chapter chapter) {
        String viewTitle = AppContext.hanText(book.title);
        String viewTooltip = viewTitle;
        String mainTitle = viewTitle;
        if (StringHelper.isNotBlank(book.id)) {
            viewTooltip = book.id.concat(" ").concat(viewTooltip);
            mainTitle = book.id.concat(" ").concat(mainTitle);
        }

        if (book.volumes.size() > 0) {
            short vol = BookXmlViewer.getVolume(chapter);
            String volInfo = vol > 0
                    ? StringHelper.concat('（', vol, '/', book.volumes.size(), "卷）")
                    : StringHelper.concat('（', book.volumes.size(), "卷）");
            viewTooltip = viewTooltip.concat(volInfo);
            mainTitle = mainTitle.concat(volInfo);
        }

        if (StringHelper.isNotBlank(book.authorInfo)) {
            String authorInfo = AppContext.hanText(book.authorInfo);
            viewTooltip = viewTooltip.concat("\n").concat(authorInfo);
            mainTitle = mainTitle.concat(" by ").concat(authorInfo);
        }

        this.title.set(StringHelper.trimChars(viewTitle, 20));
        this.tooltip.set(viewTooltip);
        this.appTitle.set(mainTitle);
    }

    @Override
    public void initialize() {
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////

    public final BookXmlViewer viewer() {
        if (null == viewer) {
            viewer = new BookXmlViewer(this, book, getViewport());
        }
        return viewer;
    }

    @Override
    public void activeViewport(boolean firstTime) {
        if (firstTime) {
            viewer().navigate(null);
        } else {
            app.eventBus.fireEvent(new BookEvent(BookEvent.VIEW, book, viewer.chapter));
        }
    }

    @Override
    public void inactiveViewport(boolean closing) {
        if (null == viewer) {
            return;
        }
        if (closing) {
            app.eventBus.fireEvent(new BookEvent(BookEvent.CLOSE, this.book, viewer.chapter));
            viewer.uninstall();
        } else {
            viewer.saveUserData();
            app.eventBus.fireEvent(new BookEvent(BookEvent.HIDE, this.book, viewer.chapter));
        }
    }
}
