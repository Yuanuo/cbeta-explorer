package org.appxi.cbeta.explorer.search;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.javafx.control.DialogPaneEx;
import org.appxi.javafx.control.ListViewExt;
import org.appxi.javafx.control.MaskingPane;
import org.appxi.javafx.control.ToolBarEx;
import org.appxi.util.StringHelper;

import java.util.Collection;
import java.util.Objects;

public abstract class LookupView<T> {
    private final StackPane primaryViewport;

    public LookupView(StackPane primaryViewport) {
        this.primaryViewport = primaryViewport;
    }

    public final void handleEventToShow(Event evt) {
        boolean handled = false;
        if (evt instanceof KeyEvent event) {
            handled = isKeyEventHandled(event);
        } else if (evt instanceof MouseEvent event) {
            handled = event.getButton() == MouseButton.PRIMARY;
        } else if (evt instanceof ActionEvent event) {
            handled = true;
        }
        if (handled) {
            show();
            evt.consume();
        }
    }

    protected boolean isKeyEventHandled(KeyEvent event) {
        return false;
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

    public void hide() {
        showing = false;
        primaryViewport.getChildren().removeAll(masking, dialogPane);
    }

    private boolean showing;
    private MaskingPane masking;
    private DialogPaneEx dialogPane;
    private Label searchInfo;
    private TextField searchInput;
    private ListViewExt<T> searchResult;


    public void show() {
        if (null == dialogPane) {
            masking = new MaskingPane();
            masking.addEventHandler(KeyEvent.KEY_PRESSED, this::handleEventToHide);
            masking.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleEventToHide);

            dialogPane = new DialogPaneEx();
            dialogPane.getStyleClass().add("lookup-view");
            StackPane.setAlignment(dialogPane, Pos.TOP_CENTER);
            dialogPane.setPrefSize(getPrefWidth(), getPrefHeight());
            dialogPane.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            dialogPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            dialogPane.addEventHandler(KeyEvent.KEY_PRESSED, this::handleEventToHide);

            //
            final ToolBarEx headBox = new ToolBarEx();

            final Label searchTitle = new Label(getSearchTitle());
            searchTitle.setStyle("-fx-font-weight: bold;");
            headBox.addLeft(searchTitle);

            searchInfo = new Label("请输入...");
            headBox.addRight(searchInfo);

            final Label searchHelp = new Label(getSearchHelp());
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
                T updatedItem;

                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        updatedItem = null;
                        this.setText(null);
                        return;
                    }
                    if (item == updatedItem)
                        return;//
                    updatedItem = item;
                    this.setText(convertItemToString(item));
                }
            });

            dialogPane.setContent(new VBox(headBox, helpBox, searchInput, searchResult));
        }
        if (!showing) {
            showing = true;
            primaryViewport.getChildren().addAll(masking, dialogPane);
        }
        searchInput.requestFocus();
        lookup(this.searchedText);
    }

    protected int getPrefWidth() {
        return 1280;
    }

    protected int getPrefHeight() {
        return 720;
    }

    private void handleEventToMoveCaret(KeyEvent event) {
        if (event.getCode() == KeyCode.UP) {
            SelectionModel<T> model = searchResult.getSelectionModel();
            int selIdx = model.getSelectedIndex() - 1;
            if (selIdx < 0)
                selIdx = searchResult.getItems().size() - 1;
            model.select(selIdx);
            searchResult.scrollToIfNotVisible(selIdx);
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            SelectionModel<T> model = searchResult.getSelectionModel();
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

    protected abstract String getSearchTitle();

    protected abstract String getSearchHelp();

    protected abstract String convertItemToString(T item);

    protected void handleEnterOrDoubleClickActionOnSearchResultList(InputEvent event, T item) {
        hide();
    }

    protected abstract Collection<T> search(String searchedText, String[] searchWords, int resultLimit);

    private boolean searching;
    private String searchedText;

    public void lookup(String text) {
        searchInput.setText(text);
    }

    private void handleInputChangedToSearching(String input) {
        final String inputText = null == input ? "" : input.replaceAll("[,，]$", "").strip();
        if (Objects.equals(this.searchedText, inputText))
            return;
        // 允许输入简繁体汉字
        if (!inputText.isEmpty() && (inputText.charAt(0) == '!' || inputText.charAt(0) == '！')) {
            // 为避免自动转换失误导致检索失败，此处特殊处理，允许以感叹号开始的字符串不自动转换简繁体
            this.searchedText = inputText.substring(1).strip();
        } else {
            // 由于CBETA数据是繁体汉字，此处转换以匹配目标文字
            this.searchedText = ChineseConvertors.hans2HantTW(inputText.strip());
        }

        searchResult.getItems().clear();
        if (this.searchedText.isBlank() && dontSearchBlankText()) {
            searchInfo.setText("请输入...");
            return;
        }

        String[] searchWords = this.searchedText.split("[,，]");
        if (searchWords.length == 1)
            searchWords = null;
        searching = true;

        Collection<T> matches = search(this.searchedText, searchWords, getResultLimit());
        searchResult.getItems().setAll(matches);
        updateSearchInfo();
        searching = false;
    }

    protected void updateSearchInfo() {
        if (this.searchResult.getItems().isEmpty()) {
            searchInfo.setText("请输入...");
        } else {
            int matches = searchResult.getItems().size();
            searchInfo.setText(matches < 1 ? "未找到匹配项"
                    : StringHelper.concat("找到 ", Math.min(matches, getResultLimit()), matches > getResultLimit() ? "+" : "", " 项"));
        }
    }

    protected boolean dontSearchBlankText() {
        return false;
    }

    protected abstract int getResultLimit();
}
