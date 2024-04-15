package org.appxi.cbeta.app.search;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.appxi.book.Chapter;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.app.DataApp;
import org.appxi.cbeta.app.SpringConfig;
import org.appxi.cbeta.app.event.BookEvent;
import org.appxi.cbeta.app.event.GenericEvent;
import org.appxi.holder.BoolHolder;
import org.appxi.javafx.app.search.SearchedEvent;
import org.appxi.javafx.app.search.SearcherEvent;
import org.appxi.javafx.control.OpaqueLayer;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.settings.DefaultOption;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPart;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.search.solr.Piece;
import org.appxi.util.DigestHelper;
import org.appxi.util.OSVersions;
import org.appxi.util.ext.RawVal;

import java.util.Objects;

public class SearchController extends WorkbenchPartController implements WorkbenchPart.SideTool {
    private static final String PK_START_TYPE = "search.engine.start";
    private final BoolHolder profileReadyState = new BoolHolder();

    public final DataApp dataApp;
    private IndexingEvent indexingEvent;

    public SearchController(WorkbenchPane workbench, DataApp dataApp) {
        super(workbench);
        this.dataApp = dataApp;

        this.id.set("SEARCH");
        this.tooltip.set("全文检索 (Ctrl+" + (OSVersions.isMac ? "J" : "H") + ")");
        this.graphic.set(MaterialIcon.SEARCH.graphic());
    }

    @Override
    public boolean sideToolAlignTop() {
        return true;
    }

    @Override
    public void postConstruct() {
        // 响应快捷键 Ctrl+H/J 事件，以打开搜索视图, MacOS平台上Ctrl+H与系统快捷键冲突
        app.getPrimaryScene().getAccelerators().put(
                new KeyCodeCombination(OSVersions.isMac ? KeyCode.J : KeyCode.H, KeyCombination.SHORTCUT_DOWN),
                () -> openSearcherWithText(null, null));
        // 响应SEARCH Event事件，以打开搜索视图
        app.eventBus.addEventHandler(SearcherEvent.SEARCH, event -> openSearcherWithText(event.text, event.data()));
        dataApp.eventBus.addEventHandler(IndexingEvent.START, event -> indexingEvent = event);
        dataApp.eventBus.addEventHandler(IndexingEvent.STOP, event -> indexingEvent = null);
        app.eventBus.addEventHandler(GenericEvent.PROFILE_READY, event -> {
            profileReadyState.value = true;
            if (!dataApp.config.getBoolean(PK_START_TYPE, true)) return;
            if (dataApp.indexedManager.isBookListIndexable()) {
//                alertIndexable(null);
            } else {
                // 如果全文索引数据正常（不需重建索引），则此时尝试加载，否则仅在后续重建索引时初始化
                new Thread(SpringConfig::beans).start();
            }
        });
        // 响应搜索结果打开事件
        app.eventBus.addEventHandler(SearchedEvent.OPEN, event -> {
            Piece piece = event.data();
            if (null == piece)
                return;
            Book book = dataApp.dataContext.getBook(piece.field("book_s"));
            Chapter chapter = null;
            String file = piece.field("file_s");
            if (null != file) {
                // open as chapter
                chapter = book.ofChapter();
                chapter.path = file;
                chapter.anchor = piece.field("anchor_s");
                if (null != chapter.anchor) {
                    chapter.attr("position.selector", chapter.anchor);
                }
                if (null != event.highlightTerm) {
                    chapter.attr("position.term", event.highlightTerm.replaceAll("…|^\"|\"$", ""));
                }
                if (null != event.highlightSnippet) {
                    chapter.attr("position.text", event.highlightSnippet
                            .replace("§§hl#end§§", "").replace("…", ""));
                }
            }
            app.eventBus.fireEvent(new BookEvent(BookEvent.OPEN, book, chapter));
        });
        //
        dataApp.options.add(() -> {
            final BooleanProperty valueProperty = new SimpleBooleanProperty();
            valueProperty.set(dataApp.config.getBoolean(PK_START_TYPE, true));
            valueProperty.addListener((o, ov, nv) -> {
                if (null != ov && !Objects.equals(ov, nv)) {
                    dataApp.config.setProperty(PK_START_TYPE, nv);
                }
            });
            return new DefaultOption<Boolean>("自动初始化全文搜索引擎", "开：程序启动时；关：首次使用时", "性能", true)
                    .setValueProperty(valueProperty);
        });
    }

    @Override
    public void activeViewport(boolean firstTime) {
        openSearcherWithText(null, null);
    }

    private void openSearcherWithText(String text, Book scope) {
        // 有从外部打开的全文搜索，此时需要隐藏透明层
        OpaqueLayer.hideOpaqueLayer(app.getPrimaryGlass());

        // 优先查找可用的搜索视图，以避免打开太多未使用的搜索视图
        SearcherController searcher = workbench.mainViews.getTabs().stream()
                .map(tab -> (tab.getUserData() instanceof SearcherController view && view.isNeverSearched()) ? view : null)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> new SearcherController("SEARCHER-".concat(DigestHelper.uid()), workbench, dataApp));
        FxHelper.runLater(() -> {
            if (!workbench.existsMainView(searcher.id.get())) {
                workbench.addWorkbenchPartAsMainView(searcher, false);
            }
            workbench.selectMainView(searcher.id.get());
            if (null == scope) {
                searcher.setSearchScopes();
            } else {
                searcher.setSearchScopes(RawVal.kv(scope.title, scope.path + "/" + (null == scope.id ? "" : scope.id)));
            }
            if (null != indexingEvent) {
                searcher.handleEventOnIndexingRunning.handle(indexingEvent);
            } else if (dataApp.indexedManager.isBookListIndexable()) {
                alertIndexable(searcher);
            } else {
                searcher.search(text);
            }
        });
    }

    private void alertIndexable(SearcherController controller) {
        if (!profileReadyState.value) {
            return;
        }
        FxHelper.runThread(null == controller ? 1500 : 100, () -> {
            String headerText = "全文检索功能需要索引书单数据";
            String contentText = """
                    检测到【书单数据】有变化或基于新的功能特性，
                    全文检索功能需要建立索引数据，否则无法使用全文检索功能！
                    
                    你可以选择不更新索引，仅使用阅读和书名检索等功能！

                    是否现在建立索引？（索引期间可正常使用阅读等功能）
                    """;
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, contentText);
            alert.setTitle("数据更新！");
            alert.setHeaderText(headerText);
            alert.initOwner(app.getPrimaryStage());
            alert.showAndWait().filter(v -> v == ButtonType.OK)
                    .ifPresentOrElse(v -> {
                        indexingEvent = new IndexingEvent(IndexingEvent.STATUS, -1, 1, "处理中。。。");
                        if (null != controller) {
                            controller.handleEventOnIndexingRunning.handle(indexingEvent);
                        }
                        new Thread(new IndexingTask(dataApp)).start();
                    }, () -> {
                        if (null != controller) {
                            workbench.mainViews.removeTabs(workbench.mainViews.findById(controller.id.get()));
                        }
                    });
        });
    }
}
