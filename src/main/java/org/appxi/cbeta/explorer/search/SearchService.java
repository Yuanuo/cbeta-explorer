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
import org.appxi.cbeta.explorer.CbetaxHelper;
import org.appxi.cbeta.explorer.event.BookEvent;
import org.appxi.cbeta.explorer.event.ChapterEvent;
import org.appxi.cbeta.explorer.event.DataEvent;
import org.appxi.hanlp.convert.ChineseConvertors;
import org.appxi.holder.IntHolder;
import org.appxi.holder.RawHolder;
import org.appxi.javafx.control.AlignedBar;
import org.appxi.javafx.control.DialogPaneEx;
import org.appxi.javafx.control.WorkbenchPane;
import org.appxi.javafx.workbench.WorkbenchController;
import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.cbeta.CbetaHelper;
import org.appxi.tome.model.Chapter;
import org.appxi.util.StringHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

class SearchService {
    private static final int SEARCH_RESULT_LIMIT = 1000;

    private final SearchEngineMem searchEngine;
    private final WorkbenchController workbenchController;
    private final WorkbenchPane workbenchPane;
    private boolean showing;

    public SearchService(WorkbenchController workbenchController) {
        this.workbenchController = workbenchController;
        this.searchEngine = new SearchEngineMem();
        //
        workbenchPane = workbenchController.getViewport();
        workbenchController.getPrimaryScene().getAccelerators().put(
                new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN),
                this::show);
        workbenchPane.addEventHandler(KeyEvent.KEY_PRESSED, this::handleEventToShow);
        workbenchController.getEventBus().addEventHandler(DataEvent.SEARCH_OPEN, event -> this.show());
    }

    public void setupInitialize() {
        CompletableFuture.runAsync(() -> {
            ChineseConvertors.s2t("测试");
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long st = System.currentTimeMillis();
            CbetaxHelper.books.getDataMap().values().parallelStream().forEach(book -> {
                final Collection<SearchRecord> records = new ArrayList<>();
                records.add(new SearchRecord(book.id, null, StringHelper.concat(book.id, " - ", book.title, " / ", book.authorInfo)));
                CbetaHelper.walkTocChapters(book, (href, text) -> {
                    records.add(new SearchRecord(book.id, href, StringHelper.concat(book.title, " / ", text)));
                    // return false to break walking
                    return false;
                });
                searchEngine.addSearchRecords(records);
                records.clear();
            });
            workbenchController.getEventBus().fireEvent(new DataEvent(DataEvent.SEARCH_READY));
            System.out.println("init search records used time: " + (System.currentTimeMillis() - st));
            System.out.println("records size: " + SearchEngineMem.DATABASE.size());
        }).whenComplete((o, err) -> {
            System.out.println("111111    " + System.currentTimeMillis());
            System.gc();
        });
    }

    private long previousShiftPressedTime;

    public final void handleEventToShow(Event evt) {
        boolean handled = false;
        if (evt instanceof KeyEvent event) {
            handled = event.isControlDown() && event.getCode() == KeyCode.O;
            if (!handled && event.getCode() == KeyCode.SHIFT) {
                final long currentShiftPressedTime = System.currentTimeMillis();
                handled = currentShiftPressedTime - previousShiftPressedTime <= 400;
                previousShiftPressedTime = currentShiftPressedTime;
            }
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
            hide();
            evt.consume();
        }
    }

    private DialogPaneEx dialogPane;
    private Label searchInfo;
    private TextField searchInput;
    private ListView<SearchRecord> searchResult;

    public void show() {
        if (null == dialogPane) {
            dialogPane = new DialogPaneEx();
            dialogPane.getStyleClass().add("search-pane");
            StackPane.setAlignment(dialogPane, Pos.TOP_CENTER);
            dialogPane.setPrefSize(960, 600);
            dialogPane.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            dialogPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            dialogPane.addEventHandler(KeyEvent.KEY_PRESSED, this::handleEventToHide);

            //
            final AlignedBar headBox = new AlignedBar();

            final Label headTitle = new Label("快速查找书籍（快捷键：双击Shift 或 Ctrl+O）");
            headTitle.setStyle("-fx-font-weight: bold;");
            headBox.addLeft(headTitle);

            searchInfo = new Label("请输入...");
            headBox.addRight(searchInfo);

            final Label searchHelp = new Label(">> 不分简繁任意汉字匹配；逗号分隔任意字/词/短语匹配；书名/书号ID/作者/译者/朝代任意匹配；");
            final ToolBar helpBox = new ToolBar(searchHelp);

            //
            searchInput = new TextField();
            searchInput.setPromptText("在此输入");
            searchInput.textProperty().addListener((o, ov, text) -> this.handleSearchingOnSearchInputChanged(text));

            searchResult = new ListView<>();
            VBox.setVgrow(searchResult, Priority.ALWAYS);
            searchResult.setOnMouseReleased(this::handleSearchResultEventToOpen);
            searchResult.setOnKeyReleased(this::handleSearchResultEventToOpen);
            searchResult.addEventHandler(KeyEvent.KEY_PRESSED, this::handleEventToHide);
            searchResult.setCellFactory(v -> new ListCell<>() {
                @Override
                protected void updateItem(SearchRecord item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        this.setText(null);
                        return;
                    }
                    this.setText(item.content());
                }
            });

            dialogPane.setContent(new VBox(headBox, helpBox, searchInput, searchResult));
        }
        if (!showing) {
            showing = true;
            workbenchPane.showMasking(dialogPane);
            workbenchPane.masking.addEventHandler(KeyEvent.KEY_PRESSED, this::handleEventToHide);
            workbenchPane.masking.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleEventToHide);
        }
        searchInput.requestFocus();
    }

    public void hide() {
        searchingCanceled = true;
        showing = false;
        workbenchPane.hideMasking(dialogPane);
        workbenchPane.masking.removeEventHandler(KeyEvent.KEY_PRESSED, this::handleEventToHide);
        workbenchPane.masking.removeEventHandler(MouseEvent.MOUSE_PRESSED, this::handleEventToHide);
    }

    private void handleSearchResultEventToOpen(Event evt) {
        boolean handled = false;
        if (evt instanceof MouseEvent event) {
            handled = event.getButton() == MouseButton.PRIMARY && event.getClickCount() > 1;
        } else if (evt instanceof KeyEvent event) {
            handled = event.getCode() == KeyCode.ENTER;
        }
        if (!handled) return;

        final SearchRecord record = searchResult.getSelectionModel().getSelectedItem();
        if (null != record) {
            hide();
            evt.consume();
            CbetaBook book = SearchHelper.searchById(record.bookId());
            if (null != record.chapter()) {
                // open as chapter
                String[] tmpArr = record.chapter().split("#", 2);
                Chapter chapter = new Chapter();
                chapter.path = tmpArr[0];
                chapter.start = tmpArr.length == 2 ? tmpArr[1] : null;
                workbenchController.getEventBus().fireEvent(new ChapterEvent(ChapterEvent.OPEN, book, chapter));
            } else {
                // open as book
                workbenchController.getEventBus().fireEvent(new BookEvent(BookEvent.OPEN, book));
            }
        }
    }

    private boolean searchingCanceled;
    private String searchingText;

    private void handleSearchingOnSearchInputChanged(String input) {
        final String text = CbetaxHelper.stripUnexpected(input).replaceAll("[,，]$", "");
        if (Objects.equals(this.searchingText, text))
            return;
        this.searchingText = ChineseConvertors.s2t(text);
        searchResult.getItems().clear();
        if (searchingText.isBlank()) {
            searchInfo.setText("请输入...");
            return;
        }
        System.out.println(searchingText); // for debug

        final RawHolder<String[]> words = new RawHolder<>(searchingText.split("[,，]"));
        if (words.value != null && words.value.length == 1)
            words.value = null;

        final IntHolder matches = new IntHolder(0);

        searchingCanceled = false;
        for (SearchRecord record : SearchEngineMem.DATABASE) {
            if (searchingCanceled) break;
            matching(record, words, matches);
            if (matches.value > SEARCH_RESULT_LIMIT)
                searchingCanceled = true;
        }
//        for (CbetaBook book : CbetaxHelper.books.getDataMap().values()) {
//            if (searchingCanceled) break;
//            matching(book, words, matches);
//        }
//        if (booksModelInited) {
//        } else {
//            TreeHelper.walkLeafs(controller.treeView.getRoot(), item -> {
//                final Book book = item.getValue();
//                if (null == book) return false;
//                matching(book, words, matches);
//                return searchingCanceled;
//            });
//        }
        if (matches.value < 1)
            searchInfo.setText("未找到匹配项");
        else searchInfo.setText(StringHelper.concat("找到 ", matches.value, matches.value > SEARCH_RESULT_LIMIT ? "+" : "", " 项"));
    }

    private void matching(SearchRecord record, RawHolder<String[]> words, IntHolder matches) {
        final String content = record.content();
        boolean matched = content.contains(searchingText);
        if (!matched && null != words.value)
            matched = StringHelper.containsAny(content, words.value);
        if (matched) {
            matches.value++;
            searchResult.getItems().add(record);
        }
    }
}
