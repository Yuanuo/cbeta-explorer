package org.appxi.cbeta.explorer.search;

import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.dao.Piece;
import org.appxi.cbeta.explorer.dao.PieceRepository;
import org.appxi.cbeta.explorer.dao.PiecesRepository;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.ProgressEvent;
import org.appxi.cbeta.explorer.model.BookList;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.javafx.control.BlockingView;
import org.appxi.javafx.control.BlockingViewEx;
import org.appxi.javafx.control.ListViewExt;
import org.appxi.javafx.control.TabPaneExt;
import org.appxi.javafx.helper.ToastHelper;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchMainViewController;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.model.Chapter;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.HanLang;
import org.springframework.data.solr.core.query.SolrPageRequest;
import org.springframework.data.solr.core.query.result.CountEntry;
import org.springframework.data.solr.core.query.result.FacetAndHighlightPage;
import org.springframework.data.solr.core.query.result.FacetFieldEntry;
import org.springframework.data.solr.core.query.result.HighlightEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SearcherController extends WorkbenchMainViewController {
    static final int PAGE_SIZE = 10;

    public SearcherController(String viewId, WorkbenchApplication application) {
        super(viewId, "搜索", application);
    }

    @Override
    public Node createToolIconGraphic(Boolean placeInSideViews) {
        return null;
    }

    @Override
    public void setupInitialize() {
    }

    private BlockingViewEx indexingView;
    private InputView inputView;

    private TextField input;
    private Button submit;
    private ListView<FacetValueLabel> facetCatalogView, facetPeriodView, facetTripitakaView;
    private BorderPane resultView;
    private Label resultInfo;

    @Override
    protected void onViewportInitOnce(StackPane viewport) {
        inputView = new InputView(this::search);
        //
        final SplitPane splitPane = new SplitPane();
        HBox.setHgrow(splitPane, Priority.ALWAYS);
        splitPane.getStyleClass().add("search-view");

        input = new TextField();
        HBox.setHgrow(input, Priority.ALWAYS);

        submit = new Button("搜");
        submit.setOnAction(this::handleActionOnSubmitToSearch);
        input.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ENTER)
                submit.fire();
        });

        final TabPaneExt tabPane = new TabPaneExt();
        tabPane.getStyleClass().add("filters");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        Tab tab1 = new Tab("部类", facetCatalogView = new ListView<>());
        Tab tab2 = new Tab("时代", facetPeriodView = new ListView<>());
        Tab tab3 = new Tab("藏经", facetTripitakaView = new ListView<>());
        tabPane.getTabs().addAll(tab1, tab2, tab3);

        final InvalidationListener facetCellStateListener = o -> handleSearching(searchedText);
        final Callback<ListView<FacetValueLabel>, ListCell<FacetValueLabel>> facetCellFactory =
                param -> new CheckBoxListCell<>(val -> {
                    val.stateProperty.removeListener(facetCellStateListener);
                    val.stateProperty.addListener(facetCellStateListener);
                    return val.stateProperty;
                });
        facetCatalogView.setCellFactory(facetCellFactory);
        facetPeriodView.setCellFactory(facetCellFactory);
        facetTripitakaView.setCellFactory(facetCellFactory);

        resultView = new BorderPane();
        resultView.setBottom(resultInfo = new Label());
        BorderPane.setAlignment(resultInfo, Pos.CENTER);
        resultInfo.setStyle(resultInfo.getStyle().concat("-fx-padding:.5em;"));
        resultInfo.getStyleClass().add("result-info");

        final VBox vBox = new VBox(new HBox(input, submit), tabPane);
        vBox.setStyle(vBox.getStyle().concat("-fx-padding:.5em;-fx-spacing:.5em;"));
        splitPane.getItems().setAll(vBox, resultView);
        splitPane.setDividerPositions(.3);

        viewport.getChildren().addAll(splitPane, inputView);

        //
        getEventBus().addEventHandler(ProgressEvent.INDEXING, this::handleEventOnIndexingToBlocking);
    }

    private void handleEventOnIndexingToBlocking(ProgressEvent event) {
        Platform.runLater(() -> {
            if (null == indexingView) {
                indexingView = new BlockingViewEx("正在建立索引\n请等待索引任务完成后再搜索\n索引期间可正常使用阅读功能");
                getViewport().getChildren().add(indexingView);
            }
            indexingView.progressIndicator.setProgress((double) event.step / event.steps);
            indexingView.progressMessage.setText(event.message);
            if (event.step >= event.steps) {
                getViewport().getChildren().remove(indexingView);
                indexingView = null;
            }
        });
    }

    @Override
    public void onViewportShow(boolean firstTime) {
        if (null != inputView) {
            inputView.input.requestFocus();
        }
    }

    @Override
    public void onViewportHide(boolean hideOrElseClose) {
        getEventBus().removeEventHandler(ProgressEvent.INDEXING, this::handleEventOnIndexingToBlocking);
    }

    boolean isNeverSearched() {
        // inputView还在显示时，此视图尚未执行过搜索
        return null != inputView;
    }

    void search(String text) {
        if (null == text)
            return;
        input.setText(text);
        submit.fire();
        if (null != inputView) {
            getViewport().getChildren().remove(inputView);
            inputView = null;
        }
    }

    private void handleActionOnSubmitToSearch(ActionEvent event) {
        // 允许搜索空字符串，一般情况下用户无法入手时，可通过此处默认显示的搜索结果入门
        String inputText = input.getText().strip().replace(':', ' ');
        // 搜索太长的字符串实际并无意义
        if (inputText.length() > 20)
            inputText = inputText.substring(0, 20);
        // 允许输入简体汉字，由于CBETA数据是繁体汉字，此处转换以匹配目标文字
        inputText = ChineseConvertors.hans2HantTW(inputText);
        handleSearching(inputText);
    }

    private BlockingView blockingView;
    private PieceRepository repository;
    private String searchedText, inputQuery;
    private Collection<String> categories;
    private FacetAndHighlightPage<Piece> facetAndHighlightPage;

    private void handleSearching(String inputText) {
        if (null != indexingView) {
            // 正在索引时不执行搜索
            ToastHelper.toast(getApplication(), "正在准备数据，请稍后再试");
            return;
        }
        if (null == blockingView)
            blockingView = new BlockingView();
        setSecondaryTitle("搜索：".concat(inputText.isBlank() ? "*" : inputText));
        getViewport().getChildren().add(blockingView);
        // 使用线程，避免UI阻塞假死
        new Thread(() -> handleSearchingImpl(inputText)).start();
    }

    private void handleSearchingImpl(String inputText) {
        if (null == repository)
            repository = AppContext.beans().getBean(PiecesRepository.class);

        boolean facet = true;
        categories = new ArrayList<>();
        // 当搜索条件（关键词）未改变时，已经搜索过一次，此时认为是在根据facet条件过滤，否则开启一个新的搜索
        if (inputText.equals(searchedText) && null != facetAndHighlightPage && facetAndHighlightPage.getTotalElements() > 0) {
            facet = false;
            categories.addAll(facetCatalogView.getItems().stream().filter(v -> v.stateProperty.get()).map(v -> v.value).collect(Collectors.toList()));
            categories.addAll(facetPeriodView.getItems().stream().filter(v -> v.stateProperty.get()).map(v -> v.value).collect(Collectors.toList()));
            categories.addAll(facetTripitakaView.getItems().stream().filter(v -> v.stateProperty.get()).map(v -> v.value).collect(Collectors.toList()));
        }
        searchedText = inputText;
        inputQuery = inputText;
        facetAndHighlightPage = repository.query(IndexingHelper.PROJECT, inputQuery, categories, facet, new SolrPageRequest(0, PAGE_SIZE));

        // update facet ui
        List<FacetValueLabel> tripitakaList = new ArrayList<>(), catalogList = new ArrayList<>(), periodList = new ArrayList<>();
        if (facet) {
            facetAndHighlightPage.getFacetResultPages().forEach(facetResultPage -> {
                final List<FacetFieldEntry> facetResultList = new ArrayList<>(facetResultPage.getContent());
                facetResultList.sort(Comparator.comparing(CountEntry::getValue));
                facetResultList.forEach(entry -> {
                    String value = entry.getValue();
                    long count = entry.getValueCount();

                    List<FacetValueLabel> facetList = null;
                    if (value.contains("/tripitaka/"))
                        facetList = tripitakaList;
                    else if (value.contains("/catalog/"))
                        facetList = catalogList;
                    else if (value.contains("/period/"))
                        facetList = periodList;
                    if (null != facetList) {
                        String label = value;
                        label = label.substring(label.indexOf('/') + 1);
                        label = label.substring(label.indexOf('/') + 1);
                        label = label.concat("（").concat(String.valueOf(count)).concat("）");
                        facetList.add(new FacetValueLabel(value, label));
                    }
                });
            });
        }
        boolean finalFacet = facet;
        Runnable runnable = () -> {
            if (finalFacet) {
                facetCatalogView.getItems().setAll(catalogList);
                facetPeriodView.getItems().setAll(periodList);
                facetTripitakaView.getItems().setAll(tripitakaList);
            }
            if (facetAndHighlightPage.getTotalElements() > 0) {
                Pagination pagination = new Pagination(facetAndHighlightPage.getTotalPages(), 0);
                pagination.setPageFactory(this::createResultPageView);
                resultView.setCenter(pagination);
            } else {
                resultView.setCenter(new Label("未找到匹配项，请调整关键词并重试！"));
                resultInfo.setText("");
            }
            getViewport().getChildren().remove(blockingView);
        };
        if (Platform.isFxApplicationThread())
            runnable.run();
        else Platform.runLater(runnable);
    }

    private Node createResultPageView(int pageIdx) {
        final long st = System.currentTimeMillis();
        final FacetAndHighlightPage<Piece> highlightPage;
        if (pageIdx == 0) {
            // use exists data
            highlightPage = facetAndHighlightPage;
        } else {
            // query for next page
            highlightPage = repository.query(IndexingHelper.PROJECT, inputQuery, categories, false, new SolrPageRequest(pageIdx, PAGE_SIZE));
        }
        if (null == highlightPage)
            return new Label();

        final HanLang displayHan = AppContext.getDisplayHanLang();
        final ListView<Piece> resultPageView = new ListViewExt<>((event, item) -> handleActionToOpenAndPosition(item, null, null));
        resultPageView.setCellFactory(v -> new ListCell<>() {
            final VBox cardBox;
            final Label nameLabel, locationLabel, authorsLabel;
            final TextFlow textFlow = new TextFlow();

            {
                nameLabel = new Label();
                nameLabel.getStyleClass().add("name");
                nameLabel.setStyle(nameLabel.getStyle().concat("-fx-font-size: 120%;"));

                locationLabel = new Label(null, new MaterialIconView(MaterialIcon.LOCATION_ON, "1.35em"));
                locationLabel.getStyleClass().add("location");
                locationLabel.setStyle(locationLabel.getStyle().concat("-fx-opacity:.75;"));

                authorsLabel = new Label(null, new MaterialIconView(MaterialIcon.PEOPLE, "1.35em"));
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
            }

            Piece updatedItem;

            @Override
            protected void updateItem(Piece item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                if (item == updatedItem)
                    return; //
                updatedItem = item;
                nameLabel.setText(ChineseConvertors.convert(item.title, HanLang.hantTW, displayHan));
                locationLabel.setText(item.fields.get("location_s").concat("（").concat(item.fields.get("book_s")).concat("）"));
                authorsLabel.setText(item.fields.get("authors_s"));

                List<HighlightEntry.Highlight> highlights = highlightPage.getHighlights(item);
                List<Node> texts = new ArrayList<>();
                if (!highlights.isEmpty()) {
                    highlights.forEach(highlight -> {
                        highlight.getSnipplets().forEach(str -> {
                            String[] strings = "…".concat(ChineseConvertors.convert(str, HanLang.hantTW, displayHan))
                                    .concat("…").split("§§hl#pre§§");
                            for (String string : strings) {
                                String[] tmpArr = string.split("§§hl#end§§", 2);
                                if (tmpArr.length == 1) {
                                    texts.add(new Text(tmpArr[0]));
                                } else {
                                    final Text hlText = new Text(tmpArr[0]);
                                    hlText.getStyleClass().add("highlight");
                                    hlText.setStyle(hlText.getStyle().concat("-fx-font-weight:bold; -fx-cursor:hand;"));
                                    hlText.setOnMouseReleased(event -> {
                                        if (event.getButton() == MouseButton.PRIMARY)
                                            handleActionToOpenAndPosition(item, tmpArr[0], string);
                                    });
                                    texts.add(hlText);
                                    texts.add(new Text(tmpArr[1]));
                                }
                            }
                            texts.add(new Text("\n"));
                        });
                    });
                } else {
                    String text = item.texts.get("text_txt_cjk_substr");
                    if (null == text)
                        text = "暂无数据";
                    texts.add(new Text(StringHelper.trimChars(text, 200)));
                }
                texts.forEach(t -> t.getStyleClass().add("texts"));
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

    private void handleActionToOpenAndPosition(Piece item, String term, String snippet) {
        if (null == item)
            return;
        CbetaBook book = BookList.getById(item.fields.get("book_s"));
        Chapter chapter = null;
        if (null != item.path) {
            // open as chapter
            chapter = new Chapter();
            chapter.path = item.path;
            chapter.start = item.fields.get("anchor_s");
            if (null != chapter.start)
                chapter.attr("position.selector", chapter.start);
            if (null != snippet) {
                chapter.attr("position.term", term.replace("…", ""));
                chapter.attr("position.text", snippet.replace("§§hl#end§§", "").replace("…", ""));
            }
        }
        getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, book, chapter));
    }

    static class FacetValueLabel {
        final SimpleBooleanProperty stateProperty = new SimpleBooleanProperty();
        final String value, label;

        FacetValueLabel(String value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    static class InputView extends VBox {
        final TextField input;

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
                                        
                    不区分简繁体输入！
                                        
                    精确搜索请使用（半角）双引号包含关键词！
                                        
                    输入任意字、词、句开始搜索
                                        
                    未输入关键词将显示所有结果
                                        
                    在搜索结果列表中双击打开
                                        
                    基于关键词搜索，输入字符限定长度为20
                                        
                    快捷键（Ctrl + H）开启此视图！可同时开启多个搜索视图！
                    """);
            tipLabel.setTextAlignment(TextAlignment.CENTER);
            tipLabel.setWrapText(true);
            tipLabel.setStyle(tipLabel.getStyle().concat("-fx-padding:5em 1em;"));

            vBox.getChildren().setAll(input, buttonsBox, tipLabel);
            hBox.getChildren().setAll(vBox);
            this.getChildren().setAll(hBox);
        }
    }
}
