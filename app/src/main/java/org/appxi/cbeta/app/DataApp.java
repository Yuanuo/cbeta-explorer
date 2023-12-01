package org.appxi.cbeta.app;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.Profile;
import org.appxi.cbeta.app.dao.DaoService;
import org.appxi.cbeta.app.dao.IndexedManager;
import org.appxi.cbeta.app.event.GenericEvent;
import org.appxi.cbeta.app.explorer.BookLabelStyle;
import org.appxi.cbeta.app.explorer.BooklistExplorer;
import org.appxi.cbeta.app.home.AboutController;
import org.appxi.cbeta.app.home.PreferencesController;
import org.appxi.cbeta.app.reader.BookDataPlaceController;
import org.appxi.cbeta.app.reader.BookmarksController;
import org.appxi.cbeta.app.reader.FavoritesController;
import org.appxi.cbeta.app.recent.RecentItemsController;
import org.appxi.cbeta.app.recent.RecentViewsController;
import org.appxi.cbeta.app.search.LookupController;
import org.appxi.cbeta.app.search.SearchController;
import org.appxi.cbeta.app.widget.WidgetsController;
import org.appxi.dictionary.ui.DictionaryContext;
import org.appxi.dictionary.ui.DictionaryController;
import org.appxi.javafx.app.BaseApp;
import org.appxi.javafx.app.WorkbenchAppWindowed;
import org.appxi.javafx.app.web.WebApp;
import org.appxi.javafx.app.web.WebViewer;
import org.appxi.javafx.control.ProgressLayer;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.settings.DefaultOption;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPart;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInMemory;
import org.appxi.smartcn.convert.ChineseConvertors;
import org.appxi.util.ext.HanLang;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DataApp extends WorkbenchAppWindowed implements WebApp {
    public final App basedApp;
    public final DataContext dataContext;
    public final Profile profile;
    public final HanLang.Provider hanTextProvider;

    private BookLabelStyle bookLabelStyle;

    public Preferences recentBooks = new PreferencesInMemory();
    public BooklistExplorer explorer;
    public BookDataPlaceController bookDataPlaceController;

    public DaoService daoService;

    public final IndexedManager indexedManager;

    public DataApp(App app, DataContext dataContext) {
        super(dataContext.profile.workspace(), app);
        this.basedApp = app;
        this.dataContext = dataContext;
        this.profile = dataContext.profile;
        this.hanTextProvider = new HanLang.Provider(config, eventBus);
        this.indexedManager = new IndexedManager(this);
    }

    protected List<WorkbenchPart> createWorkbenchParts(WorkbenchPane workbench) {
        final List<WorkbenchPart> result = new ArrayList<>();

        result.add(explorer = new BooklistExplorer(workbench, this));
        result.add(bookDataPlaceController = new BookDataPlaceController(workbench, this));
        result.add(new RecentItemsController(workbench, this));
        result.add(new RecentViewsController(workbench, this));
        result.add(new FavoritesController(workbench, this));
        result.add(new BookmarksController(workbench, this));
        result.add(new LookupController(workbench, this));
        result.add(new SearchController(workbench, this));
        result.add(new DictionaryController(workbench));
        result.add(new WidgetsController(workbench));
        result.add(new PreferencesController(workbench, this));
        result.add(new AboutController(workbench));

        return result;
    }

    @Override
    public void init() {
        super.init();
        //
        bookLabelStyle = BookLabelStyle.valueBy(config.getString("book.label.style", "name_vols"));

        //
        DictionaryContext.setupApplication(this);
    }

    @Override
    protected void starting(Scene primaryScene) {
        super.starting(primaryScene);
        this.daoService = new DaoService(workspace.resolve(".db"));
        //
        attachSettings();
        //
        loadProfile();
    }

    private void attachSettings() {
        //
        settings.add(() -> {
            final ObjectProperty<BookLabelStyle> valueProperty = new SimpleObjectProperty<>(bookLabelStyle);
            valueProperty.addListener((o, ov, nv) -> {
                if (null == ov || Objects.equals(ov, nv)) return;
                config.setProperty("book.label.style", nv.name());
                bookLabelStyle = nv;
                eventBus.fireEvent(new GenericEvent(GenericEvent.BOOK_LABEL_STYLED, nv));
            });
            return new DefaultOption<BookLabelStyle>("书名显示风格", "仅典籍树、已读中有效", "显示", true)
                    .setValueProperty(valueProperty);
        });
        //
        settings.add(() -> FxHelper.optionForHanLang(hanTextProvider, "以 简体/繁体 显示经名标题、阅读视图等经藏数据"));
    }

    public String formatBookLabel(Book book) {
        return bookLabelStyle.format(book);
    }

    public String hanTextToShow(String text) {
        final HanLang hanLangBase = dataContext.booklist().getHanLang();
        final HanLang hanLangUser = hanTextProvider.get();
        if (hanLangUser == hanLangBase) {
            return text;
        }
        if (hanLangUser == HanLang.hans) {
            return ChineseConvertors.toHans(text);
        }
        return ChineseConvertors.convert(text, null, hanLangUser);
    }

    public String hanTextToBase(String text) {
        final HanLang hanLangBase = dataContext.booklist().getHanLang();
        return ChineseConvertors.convert(text, null, hanLangBase);
    }

    public void editProfile() {
        FxHelper.runLater(() -> new ProfileEditor(this).showAndWait());
    }

    String profileVersion;

    void loadProfile() {
        // same as previous, do nothing
        if (Objects.equals(this.profileVersion, profile.version())) return;
        this.dataContext.reloadBookList();
        this.profileVersion = profile.version();
        //
        ProgressLayer.showAndWait(getPrimaryGlass(), progressLayer -> {
            title2.set(profile.title());
            TreeItem<Book> tree = dataContext.booklist().tree();
            if (tree.getChildren().isEmpty()) {
                FxHelper.runThread(1000, this::editProfile);
            } else {
                eventBus.fireEvent(new GenericEvent(GenericEvent.PROFILE_READY, profile));
            }
        });
    }

    @Override
    public Supplier<List<String>> webIncludesSupplier() {
        return () -> {
            List<String> result = WebViewer.getWebIncludeURIs();
            final Path dir = BaseApp.appDir().resolve("template/web-incl");
            result.addAll(Stream.of("html-viewer.css", "html-viewer.js")
                    .map(s -> dir.resolve(s).toUri().toString())
                    .toList()
            );
            result.add("<link id=\"CSS\" rel=\"stylesheet\" type=\"text/css\" href=\"" + visualProvider().getWebStyleSheetURI() + "\">");
            return result;
        };
    }

    @Override
    public Function<String, String> htmlDocumentWrapper() {
        return this::hanTextToShow;
    }
}
