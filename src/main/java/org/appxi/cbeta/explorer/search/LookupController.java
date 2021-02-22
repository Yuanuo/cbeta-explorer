package org.appxi.cbeta.explorer.search;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.appxi.cbeta.explorer.CbetaxHelper;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.SearchEvent;
import org.appxi.cbeta.explorer.event.StatusEvent;
import org.appxi.cbeta.explorer.model.BookList;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.javafx.control.DialogPaneEx;
import org.appxi.javafx.control.ListViewExt;
import org.appxi.javafx.control.MaskingPane;
import org.appxi.javafx.control.ToolBarEx;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideToolController;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.model.Chapter;
import org.appxi.util.StringHelper;

import java.util.Collection;
import java.util.Objects;

public class LookupController extends WorkbenchSideToolController {
    private static final int RESULT_LIMIT = 100;

    private LookupProvider lookupProvider;

    public LookupController(WorkbenchApplication application) {
        super("LOOKUP", "快速查找", application);
    }

    @Override
    public Node createToolIconGraphic(Boolean placeInSideViews) {
        return null;
    }

    @Override
    public void setupInitialize() {
        getPrimaryScene().getAccelerators().put(new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN),
                () -> this.onViewportShow(false));
        getPrimaryScene().addEventHandler(KeyEvent.KEY_PRESSED, this::handleEventToShow);
        getEventBus().addEventHandler(SearchEvent.LOOKUP, event -> {
            this.onViewportShow(false);
            if (null != event.text)
                searchInput.setText(event.text.strip());
        });
        this.lookupProvider = new LookupInMemory();
        getEventBus().addEventHandler(StatusEvent.BOOKS_READY, event -> ((LookupInMemory) lookupProvider).setupInitialize());
    }

    private long lastShiftKeyPressedTime;

    public final void handleEventToShow(Event evt) {
        boolean handled = false;
        if (evt instanceof KeyEvent event) {
            handled = event.isShortcutDown() && event.getCode() == KeyCode.O;
            if (!handled && event.getCode() == KeyCode.SHIFT) {
                final long currShiftKeyPressedTime = System.currentTimeMillis();
                handled = currShiftKeyPressedTime - lastShiftKeyPressedTime <= 400;
                lastShiftKeyPressedTime = currShiftKeyPressedTime;
            }
        } else if (evt instanceof MouseEvent event) {
            handled = event.getButton() == MouseButton.PRIMARY;
        } else if (evt instanceof ActionEvent event) {
            handled = true;
        }
        if (handled) {
            onViewportShow(false);
            evt.consume();
        }
    }

    public final void handleEventToHide(Event evt) {
        boolean handled = false;
        if (evt instanceof KeyEvent event) {
            handled = showing && event.getCode() == KeyCode.ESCAPE;
        } else if (evt instanceof MouseEvent event) {
            handled = showing && event.getButton() == MouseButton.PRIMARY;
        } else if (evt instanceof ActionEvent event) {
            handled = showing;
        }
        if (handled) {
            if (searching)
                searching = false;
            else hide();
            evt.consume();
        }
    }

    private void hide() {
        showing = false;
        getPrimaryViewport().getChildren().removeAll(masking, dialogPane);
    }

    private boolean showing;
    private MaskingPane masking;
    private DialogPaneEx dialogPane;
    private Label searchInfo;
    private TextField searchInput;
    private ListViewExt<LookupItem> searchResult;

    @Override
    public void onViewportShow(boolean firstTime) {
        if (null == dialogPane) {
            masking = new MaskingPane();
            masking.addEventHandler(KeyEvent.KEY_PRESSED, this::handleEventToHide);
            masking.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleEventToHide);

            dialogPane = new DialogPaneEx();
            dialogPane.getStyleClass().add("lookup-view");
            StackPane.setAlignment(dialogPane, Pos.TOP_CENTER);
            dialogPane.setPrefSize(1280, 720);
            dialogPane.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            dialogPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            dialogPane.addEventHandler(KeyEvent.KEY_PRESSED, this::handleEventToHide);

            //
            final ToolBarEx headBox = new ToolBarEx();

            final Label headTitle = new Label("快速查找书籍（快捷键：双击Shift 或 Ctrl+G 开启。 ESC 或 点击透明区 退出此界面）");
            headTitle.setStyle("-fx-font-weight: bold;");
            headBox.addLeft(headTitle);

            searchInfo = new Label("请输入...");
            headBox.addRight(searchInfo);

            final Label searchHelp = new Label(">> 不分简繁任意汉字匹配；逗号分隔任意字/词/短语匹配；书名/书号ID/作者/译者/朝代任意匹配；");
            final ToolBar helpBox = new ToolBar(searchHelp);

            //
            searchInput = new TextField();
            searchInput.setPromptText("在此输入");
            searchInput.textProperty().addListener((o, ov, text) -> this.handleInputChangedToSearching(text));
            searchInput.addEventHandler(KeyEvent.KEY_PRESSED, this::handleEventToMoveCaret);

            searchResult = new ListViewExt<>(this::handleEnterOrDoubleClickActionOnSearchResultList);
            VBox.setVgrow(searchResult, Priority.ALWAYS);
            searchResult.setFocusTraversable(false);
            searchResult.setCellFactory(v -> new ListCell<>() {
                LookupItem updatedItem;

                @Override
                protected void updateItem(LookupItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        this.setText(null);
                        return;
                    }
                    if (item == updatedItem)
                        return;//
                    updatedItem = item;
                    this.setText(item.toString());
                }
            });

            dialogPane.setContent(new VBox(headBox, helpBox, searchInput, searchResult));
        }
        if (!showing) {
            showing = true;
            getPrimaryViewport().getChildren().addAll(masking, dialogPane);
        }
        searchInput.requestFocus();
    }

    private void handleEventToMoveCaret(KeyEvent event) {
        if (event.getCode() == KeyCode.UP) {
            SelectionModel<LookupItem> model = searchResult.getSelectionModel();
            int selIdx = model.getSelectedIndex() - 1;
            if (selIdx < 0)
                selIdx = searchResult.getItems().size() - 1;
            model.select(selIdx);
            searchResult.scrollToIfNotVisible(selIdx);
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            SelectionModel<LookupItem> model = searchResult.getSelectionModel();
            int selIdx = model.getSelectedIndex() + 1;
            if (selIdx >= searchResult.getItems().size())
                selIdx = 0;
            model.select(selIdx);
            searchResult.scrollToIfNotVisible(selIdx);
            event.consume();
        } else if (event.getCode() == KeyCode.ENTER) {
            event.consume();
            handleEnterOrDoubleClickActionOnSearchResultList(event, searchResult.getSelectionModel().getSelectedItem());
        }
    }

    private void handleEnterOrDoubleClickActionOnSearchResultList(InputEvent event, LookupItem item) {
        hide();
        CbetaBook book = BookList.getById(item.bookId());
        Chapter chapter = null;
        if (null != item.chapter()) {
            // open as chapter
            String[] tmpArr = item.chapter().split("#", 2);
            chapter = new Chapter();
            chapter.path = tmpArr[0];
            if (tmpArr.length == 2) {
                chapter.start = "#".concat(tmpArr[1]);
                chapter.attr("position.selector", chapter.start);
            }
        }
        getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, book, chapter));
    }

    private boolean searching, limitReached;
    private String searchingText;

    private void handleInputChangedToSearching(String input) {
        final String inputText = CbetaxHelper.stripUnexpected(input).replaceAll("[,，]$", "");
        if (Objects.equals(this.searchingText, inputText))
            return;
        this.searchingText = inputText;

        searchResult.getItems().clear();
        if (this.searchingText.isBlank()) {
            searchInfo.setText("请输入...");
            return;
        }

        String searchText = ChineseConvertors.hans2HantTW(inputText);
        String[] searchWords = searchText.split("[,，]");
        if (searchWords.length == 1)
            searchWords = null;
        searching = true;
//        lookupProvider.search(searchText, searchWords, (idx, record) -> {
//            limitReached = idx > RESULT_LIMIT;
//            searchResult.getItems().add(record);
//            updateSearchInfo();
//            return !searching || limitReached;
//        });
        Collection<LookupItem> matches = lookupProvider.search(searchText, searchWords, RESULT_LIMIT);
        searchResult.getItems().setAll(matches);
        updateSearchInfo();
        searching = false;
    }

    void updateSearchInfo() {
        if (this.searchingText.isBlank()) {
            searchResult.getItems().clear();
            searchInfo.setText("请输入...");
            return;
        }
        int matches = searchResult.getItems().size();
        searchInfo.setText(matches < 1 ? "未找到匹配项"
                : StringHelper.concat("找到 ", Math.min(matches, RESULT_LIMIT), matches > RESULT_LIMIT ? "+" : "", " 项"));
    }
}
