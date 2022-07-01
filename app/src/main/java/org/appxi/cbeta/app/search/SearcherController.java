package org.appxi.cbeta.app.search;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
import org.appxi.holder.BoolHolder;
import org.appxi.javafx.app.search.SearchedEvent;
import org.appxi.javafx.control.ListViewEx;
import org.appxi.javafx.control.ProgressLayer;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.workbench.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchPartController;
import org.appxi.search.solr.Piece;
import org.appxi.smartcn.convert.ChineseConvertors;
import org.appxi.smartcn.pinyin.PinyinHelper;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.RawVal;
import org.springframework.data.solr.core.query.SolrPageRequest;
import org.springframework.data.solr.core.query.result.FacetAndHighlightPage;
import org.springframework.data.solr.core.query.result.HighlightEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.appxi.cbeta.app.AppContext.DND_ITEM;

class SearcherController extends WorkbenchPartController.MainView {
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

    private FirstView firstView;
    private TextField input;
    private TabPane filterTabs;
    private BorderPane resultPane;
    private Label resultInfo;

    private String inputQuery, finalQuery;

    @Override
    protected void createViewport(StackPane viewport) {
        super.createViewport(viewport);
        //
        firstView = new FirstView(this::search);
        //
        final SplitPane splitPane = new SplitPane();
        HBox.setHgrow(splitPane, Priority.ALWAYS);
        splitPane.getStyleClass().add("search-view");

        input = new TextField();
        HBox.setHgrow(input, Priority.ALWAYS);

        final Button submit = new Button("搜");
        submit.setOnAction(event -> this.search(""));
        input.setOnAction(event -> submit.fire());

        filterTabs = new TabPane(
                new FacetsTab("/catalog/", "部类"),
                new FacetsTab("/period/", "时域"),
                new FacetsTab("/author/", "作译者"),
                new FacetsTab("/tripitaka/", "藏经"),
                new ScopesTab("范围"),
                new UsagesTab());
        filterTabs.getStyleClass().addAll("filters", "compact");
        filterTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(filterTabs, Priority.ALWAYS);

        resultPane = new BorderPane();
        resultPane.setBottom(resultInfo = new Label());
        BorderPane.setAlignment(resultInfo, Pos.CENTER);
        resultInfo.setStyle(resultInfo.getStyle().concat("-fx-padding:.5em;"));
        resultInfo.getStyleClass().add("result-info");

        final HBox inputBox = new HBox(input, submit);
        inputBox.setStyle("-fx-padding: 1em 0;");

        final VBox vBox = new VBox();
        vBox.setStyle("-fx-padding:.5em;-fx-spacing:.5em;");
        vBox.getChildren().addAll(inputBox, filterTabs);
        SplitPane.setResizableWithParent(vBox, false);
        splitPane.getItems().setAll(vBox, resultPane);
        splitPane.setDividerPositions(.3);

        viewport.getChildren().addAll(splitPane, firstView);

        //
        app.eventBus.addEventHandler(ProgressEvent.INDEXING, handleEventOnIndexingToBlocking);
        //
        app.eventBus.addEventHandler(GenericEvent.HAN_LANG_CHANGED, event -> {
            filterTabs.getTabs().forEach(t -> {
                if (t instanceof FacetsTab ft) {
                    ft.listView.refresh();
                } else if (t instanceof ScopesTab st) {
                    st.listView.refresh();
                }
            });

            resultPane.requestLayout();
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

    @Override
    public void activeViewport(boolean firstTime) {
        if (null != firstView) {
            firstView.requestFocus();
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
        return null != firstView;
    }

    void setSearchScopes(RawVal<String>... scopes) {
        filterTabs.getTabs().stream()
                .filter(t -> t instanceof ScopesTab)
                .findFirst()
                .ifPresent(t -> ((ScopesTab) t).listView.getItems().setAll(scopes));
    }

    void search(String text) {
        if (null == text)
            return;
        if (null != firstView) {
            FxHelper.runLater(() -> {
                getViewport().getChildren().remove(firstView);
                firstView = null;
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
        searching(inputText);
    }

    private Runnable progressLayerRemover;
    private FacetAndHighlightPage<Piece> facetAndHighlightPage;

    private void searching(String inputText) {
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
        progressLayerRemover = ProgressLayer.show(getViewport(), progressLayer -> searchingImpl(inputText));
    }

    private void searchingImpl(String inputText) {
        final PiecesRepository repository = AppContext.getBean(PiecesRepository.class);
        if (null == repository) return;
        // 准备搜索条件
        final BoolHolder facet = new BoolHolder(true);

        final List<String> filterCategories = new ArrayList<>();
        // 当搜索条件（关键词）未改变时，已经搜索过一次，此时认为是在根据facet条件过滤，否则开启一个新的搜索
        if (Objects.equals(inputText, finalQuery) && null != facetAndHighlightPage && facetAndHighlightPage.getTotalElements() > 0) {
            facet.value = false;
            filterTabs.getTabs().stream()
                    .filter(t -> t instanceof FacetsTab)
                    .map(t -> (FacetsTab) t)
                    .forEach(t -> filterCategories.addAll(t.getSelectedValues()));
        }

        final List<String> filterScopes = new ArrayList<>();
        filterTabs.getTabs().stream()
                .filter(t -> t instanceof ScopesTab)
                .map(t -> (ScopesTab) t)
                .forEach(t -> filterScopes.addAll(t.getSelectedValues()));

        finalQuery = inputText;
        // 搜索
        facetAndHighlightPage = repository.search(BooklistProfile.ONE.profile().name(), filterScopes,
                finalQuery, filterCategories, facet.value, new SolrPageRequest(0, PAGE_SIZE));
        // debug
        if (null == facetAndHighlightPage) {
            FxHelper.runLater(() -> Optional.ofNullable(progressLayerRemover).ifPresent(Runnable::run));
            return;
        }

        // 处理搜索结果
        // 处理Facet过滤列表
        final Map<String, List<FacetItem>> facetListMap = new LinkedHashMap<>(4);
        if (facet.value) {
            filterTabs.getTabs().stream()
                    .filter(t -> t instanceof FacetsTab)
                    .map(t -> (FacetsTab) t)
                    .forEach(t -> facetListMap.put(t.getId(), new ArrayList<>()));

            facetAndHighlightPage.getFacetResultPages().forEach(p -> p.getContent().forEach(entry -> {
                String value = entry.getValue();
                // 用作表示目录路径的特殊数据，不当成Facet处理
                if (value.startsWith("nav/")) return;
                long count = entry.getValueCount();

                for (String k : facetListMap.keySet()) {
                    if (!value.contains(k))
                        continue;

                    String label = value.split(k, 2)[1];
                    String order = PinyinHelper.convert(label, "-", false);
                    facetListMap.get(k).add(new FacetItem(value, label, count, order));
                    break;
                }
            }));
            facetListMap.forEach((id, list) -> {
                if ("/period/".equals(id)) {
                    // 按数字年份排序
                    list.forEach(f -> Optional.ofNullable(Period.valueBy(f.label))
                            .ifPresentOrElse(p -> f.update(p.toString(), p.start), () -> f.order = 99999));
                    list.sort(Comparator.comparingInt(v -> (int) v.order));
                } else {
                    // 按字符拼音排序
                    list.sort(Comparator.comparing(v -> String.valueOf(v.order)));
                }
            });
        }
        FxHelper.runLater(() -> {
            if (facet.value) {
                // 更新Facet过滤列表
                filterTabs.getTabs().stream()
                        .filter(t -> t instanceof FacetsTab)
                        .map(t -> (FacetsTab) t)
                        .forEach(t -> t.listView.getItems().setAll(facetListMap.get(t.getId())));
            }

            // 更新结果列表
            if (facetAndHighlightPage.getTotalElements() > 0) {
                Pagination pagination = new Pagination(facetAndHighlightPage.getTotalPages(), 0);
                pagination.setPageFactory(pageIdx -> createPage(pageIdx, repository, filterCategories, filterScopes));
                resultPane.setCenter(pagination);
            } else {
                resultPane.setCenter(new Label("未找到匹配项，请调整关键词或搜索条件并重试！"));
                resultInfo.setText("");
            }
            if (null != progressLayerRemover) progressLayerRemover.run();
        });
    }

    private Node createPage(int pageIdx,
                            PiecesRepository repository,
                            List<String> filterCategories,
                            List<String> filterScopes) {
        if (null == repository) return new Label();
        final FacetAndHighlightPage<Piece> highlightPage;
        if (pageIdx == 0) {
            // use exists data
            highlightPage = facetAndHighlightPage;
        } else {
            // query for next page
            highlightPage = repository.search(BooklistProfile.ONE.profile().name(), filterScopes,
                    finalQuery, filterCategories, false, new SolrPageRequest(pageIdx, PAGE_SIZE));
        }
        if (null == highlightPage)
            return new Label();// avoid NPE

        final VBox listView = new VBox();
        highlightPage.getContent().forEach(v -> listView.getChildren().add(new PieceView(v, highlightPage.getHighlights(v))));

        if (highlightPage.getTotalElements() > 0) {
            resultInfo.setText("共 %d 条结果   显示条数：%d - %d   页数：%d / %d".formatted(
                    highlightPage.getTotalElements(),
                    pageIdx * PAGE_SIZE + 1, Math.min((long) (pageIdx + 1) * PAGE_SIZE, highlightPage.getTotalElements()),
                    pageIdx + 1,
                    highlightPage.getTotalPages()
            ));
        } else {
            resultInfo.setText("");
        }

        final ScrollPane scrollPane = new ScrollPane(listView);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-padding: 0;-fx-background-insets: 0;");
        return scrollPane;
    }

    static class FacetItem {
        final SimpleBooleanProperty stateProperty = new SimpleBooleanProperty();
        final String value;
        String label;
        final long count;
        Object order;

        FacetItem(String value, String label, long count, Object order) {
            this.value = value;
            this.label = label;
            this.count = count;
            this.order = order;
        }

        void update(String label, Object order) {
            this.label = label;
            this.order = order;
        }

        @Override
        public String toString() {
            return "%s（%d）".formatted(AppContext.hanText(label), count);
        }
    }

    class FacetsTab extends Tab implements Callback<ListView<FacetItem>, ListCell<FacetItem>> {
        final ListView<FacetItem> listView;
        final InvalidationListener facetCellStateListener;

        FacetsTab(String id, final String label) {
            super(label);
            setId(id);

            facetCellStateListener = o -> {
                // 更改Tab标签状态
                setText(getSelectedItems().isEmpty() ? label : label + "*");
                searching(finalQuery);
            };

            listView = new ListViewEx<>((event, item) -> {
                // 清除所有已选中状态
                filterTabs.getTabs().stream()
                        .filter(t -> t instanceof FacetsTab)
                        .map(t -> (FacetsTab) t)
                        .forEach(t -> {
                            List<FacetItem> selectedList = t.getSelectedItems();
                            // remove listener
                            selectedList.forEach(v -> v.stateProperty.removeListener(t.facetCellStateListener));
                            // clean selected state
                            selectedList.forEach(v -> v.stateProperty.set(false));
                            // rebind listener
                            selectedList.forEach(v -> v.stateProperty.addListener(t.facetCellStateListener));
                        });
                // 单独选中被双击的此条目
                item.stateProperty.set(true);
            });
            listView.setCellFactory(this);

            VBox.setVgrow(listView, Priority.ALWAYS);
            setContent(new VBox(listView));
        }

        List<FacetItem> getSelectedItems() {
            return listView.getItems().stream().filter(v -> v.stateProperty.get()).toList();
        }

        List<String> getSelectedValues() {
            return listView.getItems().stream().filter(v -> v.stateProperty.get()).map(v -> v.value).toList();
        }

        @Override
        public ListCell<FacetItem> call(ListView<FacetItem> param) {
            return new CheckBoxListCell<>(val -> {
                val.stateProperty.removeListener(facetCellStateListener);
                val.stateProperty.addListener(facetCellStateListener);
                return val.stateProperty;
            });
        }
    }

    class ScopesTab extends Tab {
        final ListView<RawVal<String>> listView;

        ScopesTab(final String label) {
            super(label);

            final Label usageTip = new Label("""
                    限定搜索范围，默认（空列表）为全局！
                    添加：从左侧【典籍树】中拖拽目录或文件到下方列表；
                    移除：在下方列表中鼠标右键可移除单项或全部；
                    """);
            usageTip.setWrapText(true);
            usageTip.setStyle("-fx-padding:.5em;");
            usageTip.setLineSpacing(5);
            usageTip.getStyleClass().add("bob-line");

            listView = new ListView<>();
            // 在列表内容变化时自动重新搜索
            listView.getItems().addListener((ListChangeListener<? super RawVal<String>>) c -> {
                // 更改Tab标签状态
                super.setText(listView.getItems().isEmpty() ? label : label + "*");
                // 尚未执行过搜索时不随条件改变而刷新搜索
                if (!isNeverSearched()) {
                    final String oldStr = finalQuery;
                    finalQuery = null; // reset
                    searching(oldStr);
                }
            });
            // 通过右键菜单移除条目
            final MenuItem removeSel = new MenuItem("移除所选");
            removeSel.setOnAction(e -> listView.getItems().removeAll(listView.getSelectionModel().getSelectedItems()));
            final MenuItem removeAll = new MenuItem("清空列表");
            removeAll.setOnAction(e -> listView.getItems().clear());
            listView.setContextMenu(new ContextMenu(removeSel, removeAll));

            VBox.setVgrow(listView, Priority.ALWAYS);
            final VBox vBox = new VBox(usageTip, listView);
            setContent(vBox);
            // 支持拖放，从资源库中拖放目录或文件以作为限定的搜索范围
            vBox.setOnDragOver(e -> e.acceptTransferModes(e.getDragboard().hasContent(DND_ITEM) ? TransferMode.ANY : TransferMode.NONE));
            vBox.setOnDragDropped(e -> {
                if (e.getDragboard().hasContent(DND_ITEM)) {
                    final Book book = (Book) e.getDragboard().getContent(DND_ITEM);
                    final RawVal<String> data = RawVal.kv(e.getDragboard().getString(), book.path + "/" + (null == book.id ? "" : book.id));
                    // remove old one
                    // 此路径或任一父级路径已在列表中时，先移除旧条目，总是以新拖入的路径为主
                    listView.getItems().removeIf(rv -> rv.value().startsWith(data.value()) || data.value().startsWith(rv.value()));
                    // add new one
                    listView.getItems().add(data);
                    e.setDropCompleted(true);
                }
            });
        }

        List<String> getSelectedValues() {
            return listView.getItems().stream().map(v ->
                    v.value().endsWith("/")
                            ? v.value().substring(0, v.value().length() - 1)
                            : v.value().substring(v.value().lastIndexOf('/') + 1)
            ).toList();
        }
    }

    static class UsagesTab extends Tab {
        UsagesTab() {
            super("？");

            final Label usageTip = new Label("""
                    1、默认不区分简繁体输入！以感叹号开始则强制区分
                    2、默认为分词搜索，精确搜索请使用（半角）双引号包含关键词；
                    3、未输入关键词将显示所有结果；
                    4、在过滤器中双击任一项可排除其余而单选过滤；
                    5、在搜索结果列表中双击打开，单击高亮关键词将“尝试”打开并定位到该处；
                    6、基于关键词搜索，输入字符限定长度为20；
                    7、快捷键（Ctrl + H）开启此视图！可同时开启多个搜索视图；
                    8、在搜索结果列表中右键单击，将复制所单击条目的文字内容到剪贴板；
                    """);
            usageTip.setWrapText(true);
            usageTip.setStyle("-fx-padding:.5em;");
            usageTip.setLineSpacing(5);

            setContent(usageTip);
        }
    }

    class PieceView extends VBox {
        PieceView(Piece item, List<HighlightEntry.Highlight> highlights) {
            final Hyperlink nameLabel = new Hyperlink(AppContext.hanText(item.title));
            nameLabel.getStyleClass().add("name");
            nameLabel.setStyle(nameLabel.getStyle().concat("-fx-font-size: 120%;"));
            nameLabel.setWrapText(true);
            nameLabel.setOnAction(e -> app.eventBus.fireEvent(new SearchedEvent(item, inputQuery, null)));

            final Label locationLabel = new Label(null, MaterialIcon.LOCATION_ON.graphic());
            locationLabel.getStyleClass().add("location");
            locationLabel.setStyle(locationLabel.getStyle().concat("-fx-opacity:.75;"));
            locationLabel.setWrapText(true);
            if (item.categories != null && !item.categories.isEmpty()) {
                final String navPrefix = "nav/".concat(BooklistProfile.ONE.profile().template().name()).concat("/");
                item.categories.stream().filter(s -> s.startsWith(navPrefix)).findFirst()
                        .ifPresent(s -> locationLabel.setText(AppContext.hanText(s.substring(navPrefix.length()))));
            }
            if (locationLabel.getText() == null || locationLabel.getText().isBlank())
                locationLabel.setText(AppContext.hanText(item.field("location_s").concat("（").concat(item.field("book_s")).concat("）")));


            final Label authorsLabel = new Label(AppContext.hanText(item.field("author_txt_aio")), MaterialIcon.PEOPLE.graphic());
            authorsLabel.getStyleClass().add("authors");
            authorsLabel.setStyle(authorsLabel.getStyle().concat("-fx-opacity:.75;"));
            authorsLabel.setWrapText(true);

            final TextFlow textFlow = new TextFlow();
            textFlow.getStyleClass().add("text-flow");
            //
            this.getStyleClass().addAll("list-cell", "bob-line", "result-item");
            this.setStyle("-fx-spacing:.85em;-fx-padding:.85em .5em;-fx-font-size: 110%;-fx-opacity:.9;");
            this.getChildren().setAll(nameLabel, locationLabel, authorsLabel, textFlow);
            this.setOnMouseReleased(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() > 1) {
                    nameLabel.fire();
                } else if (e.getButton() == MouseButton.SECONDARY) {
                    FxHelper.copyText("名称：" + nameLabel.getText() +
                                      "\n位置：" + locationLabel.getText() +
                                      "\n作译者：" + authorsLabel.getText() +
                                      "\n文本：\n" + textFlow.getChildren().stream().filter(n -> n instanceof Text)
                                              .map(n -> ((Text) n).getText())
                                              .collect(Collectors.joining()));
                    app.toast("已复制文字到剪贴板！");
                }
            });
            //

            List<Node> texts = new ArrayList<>();
            if (!highlights.isEmpty()) {
                highlights.forEach(highlight -> {
                    highlight.getSnipplets().forEach(str -> {
                        String[] strings = ("…" + AppContext.hanText(str) + "…").split("§§hl#pre§§");
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
        }
    }

    class FirstView extends BorderPane {
        final TextField _input;

        FirstView(Consumer<String> enterAction) {
            super();
            this.getStyleClass().add("input-view");
            this.setStyle(";-fx-background-color:-fx-background;");

            _input = new TextField();
            _input.setPromptText("在此输入关键词");
            _input.setTooltip(new Tooltip("在此输入关键词并回车开启搜索"));
            _input.setAlignment(Pos.CENTER);
            _input.setPrefColumnCount(50);

            final Button search = new Button("搜索");
            search.setTooltip(new Tooltip("根据关键词搜索"));
            search.setOnAction(event -> enterAction.accept(_input.getText()));
            _input.setOnAction(event -> search.fire());

            final Button searchAll = new Button("浏览");
            searchAll.setTooltip(new Tooltip("显示全部数据"));
            searchAll.setOnAction(event -> enterAction.accept(""));

            final Button searchScope = new Button("在…中搜索");
            searchScope.setTooltip(new Tooltip("自定义搜索范围"));
            searchScope.setOnAction(event -> {
                input.setText(_input.getText());
                if (getParent() instanceof Pane pane) {
                    pane.getChildren().remove(this);
                }
                filterTabs.getTabs().stream()
                        .filter(t -> t instanceof ScopesTab)
                        .findFirst()
                        .ifPresent(t -> filterTabs.getSelectionModel().select(t));
            });

            final HBox actionsBox = new HBox(search, searchAll, searchScope);
            actionsBox.setAlignment(Pos.CENTER);
            actionsBox.setSpacing(20);

            final Label usageTip = new Label("""
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
            usageTip.setTextAlignment(TextAlignment.CENTER);
            usageTip.setWrapText(true);
            usageTip.setStyle("-fx-padding:3em 1em;");
            usageTip.setLineSpacing(5);

            final VBox vBox = new VBox(_input, actionsBox, usageTip);
            vBox.setAlignment(Pos.CENTER);
            vBox.setStyle("-fx-padding:10em 2em 0; -fx-spacing:2em;");
            final ScrollPane scrollPane = new ScrollPane(vBox);
            scrollPane.setFitToWidth(true);
            this.setCenter(scrollPane);
        }

        @Override
        public void requestFocus() {
            FxHelper.runThread(50, _input::requestFocus);
        }
    }
}
