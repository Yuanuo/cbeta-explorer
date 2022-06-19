package org.appxi.cbeta.app.search;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Pagination;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.Period;
import org.appxi.cbeta.app.AppContext;
import org.appxi.cbeta.app.dao.PiecesRepository;
import org.appxi.cbeta.app.event.GenericEvent;
import org.appxi.cbeta.app.event.ProgressEvent;
import org.appxi.cbeta.app.explorer.BooklistProfile;
import org.appxi.event.EventHandler;
import org.appxi.javafx.app.search.SearchedEvent;
import org.appxi.javafx.control.ListViewEx;
import org.appxi.javafx.control.ProgressLayer;
import org.appxi.javafx.control.TabPaneEx;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.search.solr.Piece;
import org.appxi.smartcn.convert.ChineseConvertors;
import org.appxi.smartcn.pinyin.PinyinHelper;
import org.appxi.util.StringHelper;
import org.springframework.data.solr.core.query.SolrPageRequest;
import org.springframework.data.solr.core.query.result.FacetAndHighlightPage;
import org.springframework.data.solr.core.query.result.HighlightEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SearcherController extends WorkbenchPartController.MainView {
    static final int PAGE_SIZE = 10;
    final EventHandler<ProgressEvent> handleEventOnIndexingToBlocking = this::handleEventOnIndexingToBlocking;

    public SearcherController(String viewId, WorkbenchPane workbench) {
        super(workbench);

        this.id.set(viewId);
        this.setTitles(null);
    }

    protected void setTitles(String appendText) {
        String title = "搜索";
        if (null != appendText)
            title = title + "：" + (appendText.isBlank() ? "*" : StringHelper.trimChars(appendText, 16));
        this.title.set(title);
        this.tooltip.set(title);
        this.appTitle.set(title);
    }

    @Override
    public void postConstruct() {
    }

    private InputView inputView;
    private Book searchScope;
    private Label searchScopeTipLabel;
    private TextField input;
    private ListView<FacetFilter> facetCatalogView, facetPeriodView, facetTripitakaView, facetAuthorView;
    private BorderPane resultView;
    private Label resultInfo;

    private String inputQuery, finalQuery;
    private final InvalidationListener facetCellStateListener = o -> handleSearching(finalQuery);

    @Override
    public void createViewport(StackPane viewport) {
        super.createViewport(viewport);
        //
        inputView = new InputView(this::search);
        //
        final SplitPane splitPane = new SplitPane();
        HBox.setHgrow(splitPane, Priority.ALWAYS);
        splitPane.getStyleClass().add("search-view");

        input = new TextField();
        HBox.setHgrow(input, Priority.ALWAYS);

        final Button submit = new Button("搜");
        submit.setTooltip(new Tooltip("模糊搜索"));
        submit.setOnAction(event -> this.search(""));
        input.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ENTER)
                submit.fire();
        });

        final TabPaneEx tabPane = new TabPaneEx();
        tabPane.getStyleClass().add("filters");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        Tab tab1 = new Tab("部类", facetCatalogView = new ListViewEx<>(this::handleEventOnFacetEnterOrDoubleClick));
        Tab tab2 = new Tab("时域", facetPeriodView = new ListViewEx<>(this::handleEventOnFacetEnterOrDoubleClick));
        Tab tab3 = new Tab("藏经", facetTripitakaView = new ListViewEx<>(this::handleEventOnFacetEnterOrDoubleClick));
        Tab tab4 = new Tab("作译者", facetAuthorView = new ListViewEx<>(this::handleEventOnFacetEnterOrDoubleClick));
        tabPane.getTabs().addAll(tab1, tab2, tab3, tab4);

        final Callback<ListView<FacetFilter>, ListCell<FacetFilter>> facetCellFactory =
                param -> new CheckBoxListCell<>(val -> {
                    val.stateProperty.removeListener(facetCellStateListener);
                    val.stateProperty.addListener(facetCellStateListener);
                    return val.stateProperty;
                });
        facetCatalogView.setCellFactory(facetCellFactory);
        facetPeriodView.setCellFactory(facetCellFactory);
        facetTripitakaView.setCellFactory(facetCellFactory);
        facetAuthorView.setCellFactory(facetCellFactory);

        resultView = new BorderPane();
        resultView.setBottom(resultInfo = new Label());
        BorderPane.setAlignment(resultInfo, Pos.CENTER);
        resultInfo.setStyle(resultInfo.getStyle().concat("-fx-padding:.5em;"));
        resultInfo.getStyleClass().add("result-info");

        searchScopeTipLabel = new Label("在 全局 中搜索");
        searchScopeTipLabel.setWrapText(true);

        final VBox vBox = new VBox();
        vBox.setStyle(vBox.getStyle().concat("-fx-padding:.5em;-fx-spacing:.5em;"));
        vBox.getChildren().addAll(new HBox(input, submit), searchScopeTipLabel, tabPane);
        splitPane.getItems().setAll(vBox, resultView);
        splitPane.setDividerPositions(.3);

        viewport.getChildren().addAll(splitPane, inputView);

        //
        app.eventBus.addEventHandler(ProgressEvent.INDEXING, handleEventOnIndexingToBlocking);
        //
        app.eventBus.addEventHandler(GenericEvent.HAN_LANG_CHANGED, event -> {
            facetCatalogView.refresh();
            facetPeriodView.refresh();
            facetTripitakaView.refresh();
            facetAuthorView.refresh();
            if (null != resultPageView)
                resultPageView.refresh();
        });
    }

    private ProgressLayer indexingProgressLayer;

    private void handleEventOnIndexingToBlocking(ProgressEvent event) {
        FxHelper.runLater(() -> {
            if (null == indexingProgressLayer) {
                indexingProgressLayer = new ProgressLayer();
                indexingProgressLayer.header.setWrapText(true);
                indexingProgressLayer.header.setStyle("-fx-font-size: 100%;");
                indexingProgressLayer.header.setText("""
                        正在更新索引
                        请等待索引任务完成后再搜索
                        索引期间可正常使用阅读功能
                        """);
                indexingProgressLayer.show(getViewport());
            }
            indexingProgressLayer.indicator.setProgress((double) event.step / event.steps);
            indexingProgressLayer.message.setText(event.message);
            if (event.isFinished()) {
                indexingProgressLayer.hide();
                indexingProgressLayer = null;
            }
        });
    }

    private void handleEventOnFacetEnterOrDoubleClick(InputEvent inputEvent, FacetFilter item) {
        final List<FacetFilter> selectedList = new ArrayList<>();
        selectedList.addAll(getSelectedFacetFilters(facetCatalogView));
        selectedList.addAll(getSelectedFacetFilters(facetPeriodView));
        selectedList.addAll(getSelectedFacetFilters(facetTripitakaView));
        selectedList.addAll(getSelectedFacetFilters(facetAuthorView));

        // remove listener
        selectedList.forEach(v -> v.stateProperty.removeListener(facetCellStateListener));
        // clean selected state
        selectedList.forEach(v -> v.stateProperty.set(false));
        // rebind listener
        selectedList.forEach(v -> v.stateProperty.addListener(facetCellStateListener));
        // select this one only
        item.stateProperty.set(true);
    }

    private List<FacetFilter> getSelectedFacetFilters(ListView<FacetFilter> listView) {
        return listView.getItems().stream().filter(v -> v.stateProperty.get()).collect(Collectors.toList());
    }

    private List<String> getSelectedFacetFilterValues(ListView<FacetFilter> listView) {
        return listView.getItems().stream().filter(v -> v.stateProperty.get()).map(v -> v.value).collect(Collectors.toList());
    }

    @Override
    public void activeViewport(boolean firstTime) {
        if (null != inputView) {
            FxHelper.runThread(30, () -> inputView.input.requestFocus());
        }
    }

    @Override
    public void inactiveViewport(boolean closing) {
        if (closing) {
            app.eventBus.removeEventHandler(ProgressEvent.INDEXING, handleEventOnIndexingToBlocking);
        }
    }

    boolean isNeverSearched() {
        // inputView还在显示时，此视图尚未执行过搜索
        return null != inputView;
    }

    SearcherController setSearchScope(Book searchScope) {
        this.searchScope = searchScope;
        FxHelper.runLater(() -> {
            this.searchScopeTipLabel.setText(null != searchScope ?
                    "在 ".concat(searchScope.title).concat(" 中搜索") : "在 全局 中搜索");
            if (null != inputView)
                inputView.scopeLabel.setText(this.searchScopeTipLabel.getText());
        });
        return this;
    }

    void search(String text) {
        if (null == text)
            return;
        if (null != inputView) {
            Platform.runLater(() -> {
                getViewport().getChildren().remove(inputView);
                inputView = null;
            });
            input.setText(text);
        }
        // 允许搜索空字符串，一般情况下用户无法入手时，可通过此处默认显示的搜索结果入门
        String inputText = input.getText().strip().replace(':', ' ');
        //
        inputQuery = inputText;
        // 允许输入简繁体汉字
        if (!inputText.isEmpty() && (inputText.charAt(0) == '!' || inputText.charAt(0) == '！')) {
            // 为避免自动转换失误导致检索失败，此处特殊处理，允许以感叹号开始的字符串不自动转换简繁体
            inputText = inputText.substring(1).strip();
        } else {
            // 由于CBETA数据是繁体汉字，此处转换以匹配目标文字
            inputText = ChineseConvertors.hans2HantTW(inputText.strip());
        }
        // 搜索太长的字符串实际并无意义
        if (inputText.length() > 20)
            inputText = inputText.substring(0, 20);
        //
        handleSearching(inputText);
    }

    //    private Runnable blockingHandler;
    private PiecesRepository repository;
    private Collection<String> categories;
    private FacetAndHighlightPage<Piece> facetAndHighlightPage;
    private ListView<Piece> resultPageView;

    private void handleSearching(String inputText) {
        // 此时如果还有索引没有准备好，则不做操作，也不提示，避免引发beans的初始化
        if (IndexedManager.isBookcaseIndexable() || IndexedManager.isBooklistIndexable())
            return;
        if (null != indexingProgressLayer) {
            // 正在索引时不执行搜索
            app.toast("正在准备数据，请稍后再试");
            return;
        }
        setTitles(inputQuery.length() > 20 ? inputQuery.substring(0, 20) : inputQuery);
        // 使用线程，避免UI阻塞假死
        ProgressLayer.showAndWait(getViewport(), progressLayer -> handleSearchingImpl(inputText));
    }

    private void handleSearchingImpl(String inputText) {
        if (null == repository)
            repository = AppContext.getBean(PiecesRepository.class);
        if (null == repository) return;

        boolean facet = true;
        categories = new ArrayList<>();
        // 当搜索条件（关键词）未改变时，已经搜索过一次，此时认为是在根据facet条件过滤，否则开启一个新的搜索
        if (inputText.equals(finalQuery) && null != facetAndHighlightPage && facetAndHighlightPage.getTotalElements() > 0) {
            facet = false;
            categories.addAll(getSelectedFacetFilterValues(facetCatalogView));
            categories.addAll(getSelectedFacetFilterValues(facetPeriodView));
            categories.addAll(getSelectedFacetFilterValues(facetTripitakaView));
            categories.addAll(getSelectedFacetFilterValues(facetAuthorView));
        }
        finalQuery = inputText;
        facetAndHighlightPage = repository.search(BooklistProfile.ONE.profile().name(),
                null == searchScope ? null : List.of(searchScope.id != null ? searchScope.id : searchScope.attr("scope")),
                finalQuery, categories, facet, new SolrPageRequest(0, PAGE_SIZE));
        if (null == facetAndHighlightPage) return;

        // update facet ui
        final List<FacetFilter> facetTripitakaList = new ArrayList<>(), facetCatalogList = new ArrayList<>(),
                facetPeriodList = new ArrayList<>(), facetAuthorList = new ArrayList<>();
        if (facet) {
            final Map<String, List<FacetFilter>> listMap = Map.of(
                    "/tripitaka/", facetTripitakaList,
                    "/catalog/", facetCatalogList,
                    "/period/", facetPeriodList,
                    "/author/", facetAuthorList);
            facetAndHighlightPage.getFacetResultPages().forEach(facetResultPage -> {
                facetResultPage.getContent().forEach(entry -> {
                    String value = entry.getValue();
                    if (value.startsWith("nav/")) return;
                    long count = entry.getValueCount();

                    for (String k : listMap.keySet()) {
                        if (!value.contains(k))
                            continue;

                        String label = value.split(k, 2)[1];
                        String order = PinyinHelper.convert(label, "-", false);
                        listMap.get(k).add(new FacetFilter(value, label, count, order));
                        break;
                    }
                });
            });
            // fix
            if (!facetPeriodList.isEmpty()) {
                facetPeriodList.forEach(f -> Optional.ofNullable(Period.valueBy(f.label))
                        .ifPresentOrElse(p -> f.update(p.toString(), p.start), () -> f.order = 99999));
                facetPeriodList.sort(Comparator.comparingInt(v -> (int) v.order));
            }
            facetCatalogList.sort(Comparator.comparing(v -> String.valueOf(v.order)));
            facetTripitakaList.sort(Comparator.comparing(v -> String.valueOf(v.order)));
            facetAuthorList.sort(Comparator.comparing(v -> String.valueOf(v.order)));
        }
        boolean finalFacet = facet;
        Runnable runnable = () -> {
            if (finalFacet) {
                facetCatalogView.getItems().setAll(facetCatalogList);
                facetPeriodView.getItems().setAll(facetPeriodList);
                facetTripitakaView.getItems().setAll(facetTripitakaList);
                facetAuthorView.getItems().setAll(facetAuthorList);
            }
            if (facetAndHighlightPage.getTotalElements() > 0) {
                Pagination pagination = new Pagination(facetAndHighlightPage.getTotalPages(), 0);
                pagination.setPageFactory(this::createResultPageView);
                resultView.setCenter(pagination);
            } else {
                resultView.setCenter(new Label("未找到匹配项，请调整关键词并重试！"));
                resultInfo.setText("");
            }
        };
        FxHelper.runLater(runnable);
    }

    private Node createResultPageView(int pageIdx) {
        if (null == repository) return new Label();
        final long st = System.currentTimeMillis();
        final FacetAndHighlightPage<Piece> highlightPage;
        if (pageIdx == 0) {
            // use exists data
            highlightPage = facetAndHighlightPage;
        } else {
            // query for next page
            highlightPage = repository.search(BooklistProfile.ONE.profile().name(),
                    null == searchScope ? null : List.of(searchScope.id != null ? searchScope.id : searchScope.attr("scope")),
                    finalQuery, categories, false, new SolrPageRequest(pageIdx, PAGE_SIZE));
        }
        if (null == highlightPage)
            return new Label();// avoid NPE

        resultPageView = new ListViewEx<>((event, item) -> app.eventBus.fireEvent(new SearchedEvent(item)));
        resultPageView.setCellFactory(v -> new ListCell<>() {
            final VBox cardBox;
            final Label nameLabel, locationLabel, authorsLabel;
            final TextFlow textFlow = new TextFlow();

            {
                nameLabel = new Label();
                nameLabel.getStyleClass().add("name");
                nameLabel.setStyle(nameLabel.getStyle().concat("-fx-font-size: 120%;"));

                locationLabel = new Label(null, MaterialIcon.LOCATION_ON.graphic());
                locationLabel.getStyleClass().add("location");
                locationLabel.setStyle(locationLabel.getStyle().concat("-fx-opacity:.75;"));

                authorsLabel = new Label(null, MaterialIcon.PEOPLE.graphic());
                authorsLabel.getStyleClass().add("authors");
                authorsLabel.setStyle(authorsLabel.getStyle().concat("-fx-opacity:.75;"));

                textFlow.getStyleClass().add("text-flow");

                cardBox = new VBox(nameLabel, locationLabel, authorsLabel, textFlow);
                cardBox.getStyleClass().addAll("v-box");
                cardBox.setStyle(cardBox.getStyle().concat("-fx-spacing:.85em;-fx-padding:.85em .5em;"));
                cardBox.maxWidthProperty().bind(Bindings.createDoubleBinding(
                        () -> getWidth() - getPadding().getLeft() - getPadding().getRight() - 1,
                        widthProperty(), paddingProperty()));

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                getStyleClass().add("result-item");
                setStyle(getStyle().concat("-fx-font-size: 110%;-fx-opacity:.9;"));
                //
                setTooltip(new Tooltip("鼠标右键点击可复制当前条目文字到剪贴板"));
                setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.SECONDARY) {
                        String copyText = "名称：" + nameLabel.getText() +
                                "\n位置：" + locationLabel.getText() +
                                "\n作译者：" + authorsLabel.getText() +
                                "\n文本：\n" + textFlow.getChildren().stream().filter(n -> n instanceof Text)
                                .map(n -> ((Text) n).getText())
                                .collect(Collectors.joining());

                        Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, copyText));
                        app.toast("已复制到剪贴板！");
                    }
                });
            }

            Piece updatedItem;

            @Override
            protected void updateItem(Piece item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    updatedItem = null;
                    setGraphic(null);
                    return;
                }
                if (item == updatedItem)
                    return; //
                updatedItem = item;
                nameLabel.setText(AppContext.hanText(item.title));
                locationLabel.setText(null);
                if (item.categories != null && !item.categories.isEmpty()) {
                    final String navPrefix = "nav/".concat(BooklistProfile.ONE.profile().template().name()).concat("/");
                    item.categories.stream().filter(s -> s.startsWith(navPrefix)).findFirst()
                            .ifPresent(s -> locationLabel.setText(AppContext.hanText(s.substring(navPrefix.length()))));
                }
                if (locationLabel.getText() == null || locationLabel.getText().isBlank())
                    locationLabel.setText(AppContext.hanText(item.field("location_s").concat("（").concat(item.field("book_s")).concat("）")));

                authorsLabel.setText(AppContext.hanText(item.field("author_txt_aio")));

                List<HighlightEntry.Highlight> highlights = highlightPage.getHighlights(item);
                List<Node> texts = new ArrayList<>();
                if (!highlights.isEmpty()) {
                    highlights.forEach(highlight -> {
                        highlight.getSnipplets().forEach(str -> {
                            String[] strings = "…".concat(AppContext.hanText(str))
                                    .concat("…").split("§§hl#pre§§");
                            for (String string : strings) {
                                String[] tmpArr = string.split("§§hl#end§§", 2);
                                if (tmpArr.length == 1) {
                                    final Text text1 = new Text(tmpArr[0]);
                                    text1.getStyleClass().add("plaintext");
                                    texts.add(text1);
                                } else {
                                    final Text hlText = new Text(tmpArr[0]);
                                    hlText.getStyleClass().add("highlight");
                                    hlText.setStyle(hlText.getStyle().concat("-fx-cursor:hand;"));
                                    hlText.setOnMouseReleased(event -> {
                                        if (event.getButton() == MouseButton.PRIMARY) {
                                            app.eventBus.fireEvent(new SearchedEvent(item, tmpArr[0], string));
                                        }
                                    });
                                    texts.add(hlText);
                                    final Text text1 = new Text(tmpArr[1]);
                                    text1.getStyleClass().add("plaintext");
                                    texts.add(text1);
                                }
                            }
                            texts.add(new Text("\n"));
                        });
                    });
                } else {
                    String text = item.text("text_txt_aio_sub");
                    final Text text1 = new Text(null == text ? "" : AppContext.hanText(StringHelper.trimChars(text, 200)));
                    text1.getStyleClass().add("plaintext");
                    texts.add(text1);
                }
                textFlow.getChildren().setAll(texts.toArray(new Node[0]));
                setGraphic(cardBox);
            }
        });

        if (highlightPage.getTotalElements() > 0)
            resultInfo.setText("共 %d 条结果   显示：%d - %d   页数：%d / %d   用时%dms".formatted(
                    highlightPage.getTotalElements(),
                    pageIdx * PAGE_SIZE + 1, Math.min((long) (pageIdx + 1) * PAGE_SIZE, highlightPage.getTotalElements()),
                    pageIdx + 1, highlightPage.getTotalPages(),
                    System.currentTimeMillis() - st
            ));
        else resultInfo.setText("");
        resultPageView.getItems().setAll(highlightPage.getContent());
        return resultPageView;
    }

    static class FacetFilter {
        final SimpleBooleanProperty stateProperty = new SimpleBooleanProperty();
        final String value;
        String label;
        final long count;
        Object order;

        FacetFilter(String value, String label, long count, Object order) {
            this.value = value;
            this.label = label;
            this.count = count;
            this.order = order;
        }

        FacetFilter update(String label, Object order) {
            this.label = label;
            this.order = order;
            return this;
        }

        @Override
        public String toString() {
            return "%s（%d）".formatted(AppContext.hanText(label), count);
        }
    }

    static class InputView extends VBox {
        final TextField input;
        final Label scopeLabel;

        InputView(Consumer<String> enterAction) {
            super();
            getStyleClass().add("input-view");
            setAlignment(Pos.TOP_CENTER);
            setStyle(getStyle().concat(";-fx-background-color:-fx-background;"));

            final HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER);
            hBox.setPadding(new Insets(150, 0, 0, 0));

            final VBox vBox = new VBox();
            vBox.setAlignment(Pos.CENTER);
            vBox.setSpacing(20);

            input = new TextField();
            input.setAlignment(Pos.CENTER);
            input.setPrefColumnCount(50);
            input.setPrefHeight(40);

            scopeLabel = new Label("在 全局 中搜索");
            scopeLabel.setTextAlignment(TextAlignment.CENTER);
            scopeLabel.setWrapText(true);

            final Button submit = new Button("搜索");
            submit.setOnAction(event -> enterAction.accept(input.getText()));
            input.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
                if (event.getCode() == KeyCode.ENTER)
                    submit.fire();
            });

            final Button searchAll = new Button("无从下手？试试这个");
            searchAll.setOnAction(event -> enterAction.accept(""));
            final HBox buttonsBox = new HBox(submit, searchAll);
            buttonsBox.setAlignment(Pos.CENTER);
            buttonsBox.setSpacing(20);

            final Label tipLabel = new Label("""
                    提示：

                    默认不区分简繁体输入！以感叹号开始则强制区分
                    默认为分词搜索，精确搜索请使用（半角）双引号包含关键词！
                    输入任意词、句开始搜索
                    未输入关键词将显示所有结果
                    在搜索结果左侧过滤器中双击任一项可排除其余而单选过滤
                    在搜索结果列表中双击打开，单击高亮关键词将“尝试”打开并定位到该处
                    基于关键词搜索，输入字符限定长度为20，太长亦无意义
                    快捷键（Ctrl + H）开启此视图！可同时开启多个搜索视图！
                    """);
            tipLabel.setTextAlignment(TextAlignment.CENTER);
            tipLabel.setWrapText(true);
            tipLabel.setStyle(tipLabel.getStyle().concat("-fx-padding:5em 1em;"));

            vBox.getChildren().setAll(input, scopeLabel, buttonsBox, tipLabel);
            hBox.getChildren().setAll(vBox);
            this.getChildren().setAll(hBox);
        }
    }
}
