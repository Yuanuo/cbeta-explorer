package org.appxi.cbeta.explorer.search;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.appxi.javafx.control.AlignedBox;
import org.appxi.javafx.control.DialogPaneEx;
import org.appxi.javafx.control.ListViewExt;
import org.appxi.javafx.control.MaskingPane;
import org.appxi.util.StringHelper;

import java.util.Collection;
import java.util.Objects;

public abstract class LookupView<T> {
    private final StackPane primaryViewport;

    private boolean showing;
    private MaskingPane masking;
    private DialogPaneEx dialogPane;
    private Label searchInfo;
    private TextField searchInput;
    private ListViewExt<T> searchResult;

    private boolean searching;
    private String searchedText;

    public LookupView(StackPane primaryViewport) {
        this.primaryViewport = primaryViewport;
    }

    public final void search(String text) {
        if (null != searchInput)
            searchInput.setText(text);
    }

    public void hide() {
        if (!showing)
            return;
        showing = false;
        primaryViewport.getChildren().removeAll(masking, dialogPane);
    }

    public void show() {
        if (null == dialogPane) {
            final EventHandler<Event> handleEventToHide = evt -> {
                boolean handled = false;
                if (evt instanceof KeyEvent event) {
                    handled = showing && event.getCode() == KeyCode.ESCAPE;
                } else if (evt instanceof MouseEvent event) {
                    handled = showing && event.getButton() == MouseButton.PRIMARY;
                } else if (evt instanceof ActionEvent) {
                    handled = showing;
                }
                if (handled) {
                    if (searching)
                        searching = false;
                    else hide();
                    evt.consume();
                }
            };

            masking = new MaskingPane();
            masking.addEventHandler(KeyEvent.KEY_PRESSED, handleEventToHide);
            masking.addEventHandler(MouseEvent.MOUSE_PRESSED, handleEventToHide);

            dialogPane = new DialogPaneEx();
            dialogPane.getStyleClass().add("lookup-view");
            StackPane.setAlignment(dialogPane, Pos.TOP_CENTER);
            dialogPane.setPrefSize(getPrefWidth(), getPrefHeight());
            dialogPane.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            dialogPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            dialogPane.addEventHandler(KeyEvent.KEY_PRESSED, handleEventToHide);

            //
            final AlignedBox headBox = new AlignedBox();

            final Label labelHeader = new Label(getHeaderText());
            labelHeader.setStyle("-fx-font-weight: bold;");
            headBox.getStyleClass().add("tool-bar");
            headBox.addLeft(labelHeader);

            searchInfo = new Label("请输入...");
            headBox.addRight(searchInfo);

            final Label labelUsages = new Label(getUsagesText());
            labelUsages.setWrapText(true);
            final AlignedBox helpBox = new AlignedBox();
            helpBox.getStyleClass().add("tool-bar");
            helpBox.addLeft(labelUsages);

            //
            searchInput = new TextField();
            searchInput.setPromptText("在此输入");
            searchInput.textProperty().addListener((o, ov, text) -> {
                String inputText = null == text ? "" : text.strip();
                if (inputText.length() > 64)
                    inputText = inputText.substring(0, 64);
                if (Objects.equals(this.searchedText, inputText))
                    return;
                this.searchedText = inputText;
                searching = true;

                final int resultLimit = getResultLimit();
                searchResult.getItems().setAll(search(inputText, resultLimit));
                if (this.searchResult.getItems().isEmpty()) {
                    searchInfo.setText("请输入...");
                } else {
                    int matches = searchResult.getItems().size();
                    searchInfo.setText(matches < 1
                            ? "未找到匹配项"
                            : StringHelper.concat("找到 ", Math.min(matches, resultLimit), matches > resultLimit ? "+" : "", " 项"));
                }
                searching = false;
            });
            searchInput.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
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
            });

            searchResult = new ListViewExt<>(this::handleEnterOrDoubleClickActionOnSearchResultList);
            VBox.setVgrow(searchResult, Priority.ALWAYS);
            searchResult.setFocusTraversable(false);
            searchResult.setCellFactory(v -> new ListCell<>() {
                T updatedItem;

                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        this.setText(null);
                        return;
                    }
                    if (item == updatedItem)
                        return;//
                    updatedItem = item;
                    updateItemOnce(this, item);
                }
            });

            dialogPane.setContent(new VBox(headBox, helpBox, searchInput, searchResult));
        }
        if (!showing) {
            showing = true;
            primaryViewport.getChildren().addAll(masking, dialogPane);
        }
        searchInput.requestFocus();
        search(this.searchedText);
        searchInput.selectAll();
    }

    protected int getPrefWidth() {
        return 1280;
    }

    protected int getPrefHeight() {
        return 720;
    }

    protected int getResultLimit() {
        return 100;
    }

    protected void updateItemOnce(Labeled labeled, T item) {
        labeled.setText(item.toString());
    }

    protected abstract String getHeaderText();

    protected abstract String getUsagesText();

    protected abstract void handleEnterOrDoubleClickActionOnSearchResultList(InputEvent event, T item);

    protected abstract Collection<T> search(String inputText, int resultLimit);
}
