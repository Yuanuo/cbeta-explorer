package org.appxi.cbeta.explorer.dict;

import appxi.dict.DictionaryApi;
import appxi.dict.SearchMode;
import appxi.dict.doc.DictEntry;
import javafx.concurrent.Worker;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import netscape.javascript.JSObject;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.book.HtmlViewer;
import org.appxi.cbeta.explorer.event.SearcherEvent;
import org.appxi.holder.RawHolder;
import org.appxi.javafx.control.WebPane;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.javafx.visual.VisualEvent;
import org.appxi.javafx.workbench.WorkbenchApp;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.StringHelper;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

class DictionaryViewer extends StackPane {
    protected WebPane webPane;
    protected WebPane.WebMarksFinder webFinder;

    final WorkbenchApp app;
    final DictEntry dictEntry;

    public DictionaryViewer(WorkbenchApp app, DictEntry dictEntry) {
        this.app = app;
        this.dictEntry = dictEntry;
        //
        this.webPane = new WebPane();
        this.webPane.addEventHandler(KeyEvent.KEY_PRESSED, this::handleWebViewShortcuts);
        this.getChildren().add(this.webPane);
        //
        addTool_Print();
        addTool_SearchAllDictionaries();
        addTool_FindInPage();
    }

    protected void addTool_Print() {
        Button button = MaterialIcon.PRINT.flatButton();
        button.setText("打印");
        button.setTooltip(new Tooltip("添加到系统默认打印机"));
        button.setOnAction(event -> {
            Printer printer = Printer.getDefaultPrinter();
            if (null == printer) {
                app.toastError("打印机不可用");
                return;
            }

            PrinterJob job = PrinterJob.createPrinterJob(printer);
            PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT,
                    .4, .4, .4, .4);
            job.getJobSettings().setPageLayout(pageLayout);
            webPane.webEngine().print(job);
            job.endJob();
            app.toast("已添加到系统打印队列，请检查打印结果！");
        });
        //
        webPane.getTopAsBar().addLeft(button);
    }

    protected void addTool_SearchAllDictionaries() {
        Button button = MaterialIcon.TRAVEL_EXPLORE.flatButton();
        button.setText("查全部词典");
        button.setTooltip(new Tooltip("从所有词典中精确查词“" + dictEntry.title + "”"));
        button.setOnAction(event -> {
            loadHtmlContent(true);
            // 已从全部词典查询，此时禁用掉此按钮
            button.setDisable(true);
        });
        //
        webPane.getTopAsBar().addLeft(button);
    }

    protected void addTool_FindInPage() {
        webFinder = new WebPane.WebMarksFinder(this.webPane, this);
        webFinder.setAsciiConvertor(AppContext::ascii);
        webPane.getTopAsBar().addRight(webFinder);
    }

    protected void onSetWebStyle(VisualEvent event) {
        if (null == this.webPane) return;
        this.onSetAppStyle(null);
    }

    protected void onSetAppStyle(VisualEvent event) {
        if (null == this.webPane) return;

        final RawHolder<byte[]> allBytes = new RawHolder<>();
        allBytes.value = """
                :root {
                    --font-family: tibetan, "%s", AUTO !important;
                    --zoom: %.2f !important;
                    --text-color: %s;
                }
                body {
                    background-color: %s;
                }
                """.formatted(app.visualProvider.webFontName(),
                app.visualProvider.webFontSize(),
                app.visualProvider.webTextColor(),
                app.visualProvider.webPageColor()
        ).getBytes(StandardCharsets.UTF_8);
        Consumer<InputStream> consumer = stream -> {
            try (BufferedInputStream in = new BufferedInputStream(stream)) {
                int pos = allBytes.value.length;
                byte[] tmpBytes = new byte[pos + in.available()];
                System.arraycopy(allBytes.value, 0, tmpBytes, 0, pos);
                allBytes.value = tmpBytes;
                in.read(allBytes.value, pos, in.available());
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        };
        Optional.ofNullable(VisualEvent.class.getResourceAsStream("web.css")).ifPresent(consumer);
        Optional.ofNullable(AppContext.class.getResourceAsStream("web.css")).ifPresent(consumer);

        String cssData = "data:text/css;charset=utf-8;base64," + Base64.getMimeEncoder().encodeToString(allBytes.value);
        FxHelper.runLater(() -> {
            webPane.webEngine().setUserStyleSheetLocation(cssData);
            webPane.executeScript("document.body.setAttribute('class','".concat(app.visualProvider.toString()).concat("');"));
        });
    }

    public void onViewportClosing() {
        if (null != webPane) webPane.release();
    }

    public void onViewportShowing() {
        webPane.webEngine().setUserDataDirectory(UserPrefs.cacheDir().toFile());
        // apply theme
        this.onSetAppStyle(null);
        //
        webPane.webEngine().getLoadWorker().stateProperty().addListener((o, ov, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                // set an interface object named 'javaApp' in the web engine's page
                final JSObject window = webPane.executeScript("window");
                window.setMember("javaApp", javaApp);
                // apply theme
                webPane.executeScript("document.body.setAttribute('class','".concat(app.visualProvider.toString()).concat("');"));
                //
                webPane.setContextMenuBuilder(this::handleWebViewContextMenu);
            }
        });
        //
        loadHtmlContent(false);
    }

    private void loadHtmlContent(boolean loadFromAll) {
        final StringBuilder buff = new StringBuilder();
        //
        buff.append("<!DOCTYPE html><html lang=\"zh\"><head><meta charset=\"UTF-8\">");
        //
        final List<String> scripts = new ArrayList<>(), styles = new ArrayList<>();
        for (String include : HtmlViewer.WebIncl.getIncludePaths()) {
            if (include.endsWith(".js")) {
                buff.append("\r\n<script type=\"text/javascript\" src=\"").append(include).append("\"></script>");
            } else if (include.endsWith(".css")) {
                buff.append("\r\n<link rel=\"stylesheet\" href=\"").append(include).append("\"/>");
            } else if (include.startsWith("<script") || include.startsWith("<style")
                    || include.startsWith("<link") || include.startsWith("<meta")) {
                buff.append("\r\n").append(include);
            } else if (include.startsWith("var ") || include.startsWith("function")) {
                scripts.add(include);
            } else {
                styles.add(include);
            }
        }
        if (!scripts.isEmpty()) {
            buff.append("\r\n<script type=\"text/javascript\">").append(StringHelper.joinLines(scripts)).append("</script>");
        }
        if (!styles.isEmpty()) {
            buff.append("\r\n<style type=\"text/css\">").append(StringHelper.joinLines(styles)).append("</style>");
        }
        //
        buff.append("</head><body><article style=\"padding: 0 1rem;\">");
        //
        if (loadFromAll) {
            List<DictEntry> list = new ArrayList<>();
            DictionaryApi.api().searchTitle(SearchMode.TitleEquals, dictEntry.title, null, null)
                    .forEachRemaining(entry -> list.add(entry.dictEntry.dictionary == dictEntry.dictionary ? 0 : list.size(), entry.dictEntry));
            for (DictEntry entry : list) {
                buff.append(DictionaryApi.toHtmlDocument(entry));
            }
        } else {
            buff.append(DictionaryApi.toHtmlDocument(dictEntry));
        }

        buff.append("</article></body></html>");

        webPane.webEngine().loadContent(buff.toString());
        // reset
        webFinder.clear.fire();
    }

    protected void handleWebViewShortcuts(KeyEvent event) {
        if (!event.isConsumed() && event.isShortcutDown()) {
            // Ctrl + F
            if (event.getCode() == KeyCode.F) {
                // 如果有选中文字，则按查找选中文字处理
                String selText = webPane.executeScript("getValidSelectionText()");
                selText = null == selText ? null : selText.strip().replace('\n', ' ');
                webFinder.find(StringHelper.isBlank(selText) ? null : selText);
                event.consume();
                return;
            }
        }
    }

    protected ContextMenu handleWebViewContextMenu() {
        final String origText = webPane.executeScript("getValidSelectionText()");
        String trimText = null == origText ? null : origText.strip().replace('\n', ' ');
        final String availText = StringHelper.isBlank(trimText) ? null : trimText;
        //
        MenuItem copy = new MenuItem("复制文字");
        copy.setDisable(null == availText);
        copy.setOnAction(event -> Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, origText)));
        //
        MenuItem copyRef = new MenuItem("复制引用");
        copyRef.setDisable(true);
        //
        //
        String textTip = null == availText ? "" : "：".concat(StringHelper.trimChars(availText, 8));
        String textForSearch = availText;

        MenuItem search = new MenuItem("全文检索".concat(textTip));
        search.setOnAction(event -> app.eventBus.fireEvent(SearcherEvent.ofSearch(textForSearch)));

        MenuItem searchInAbs = new MenuItem("全文检索（精确检索）".concat(textTip));
        searchInAbs.setOnAction(event -> app.eventBus.fireEvent(
                SearcherEvent.ofSearch(null == textForSearch ? "" : "\"".concat(textForSearch).concat("\""))));

        MenuItem lookup = new MenuItem("快捷检索（经名、作译者等）".concat(textTip));
        lookup.setOnAction(event -> app.eventBus.fireEvent(SearcherEvent.ofLookup(textForSearch)));

        MenuItem finder = new MenuItem("页内查找".concat(textTip));
        finder.setOnAction(event -> webFinder.find(availText));
        //
        MenuItem dictionary = new MenuItem();
        if (null != availText) {
            final String str = StringHelper.trimChars(availText, 10);
            dictionary.setText("查词典：" + str);
        } else {
            dictionary.setText("查词典");
        }
        dictionary.setOnAction(event -> {
            final String str = null == availText ? null : StringHelper.trimChars(availText, 20, "");
            app.eventBus.fireEvent(DictionaryEvent.ofSearch(str));
        });
        //
        MenuItem pinyin = new MenuItem();
        if (null != availText) {
            final String str = StringHelper.trimChars(availText, 10, "");
            final String strPy = AppContext.ascii(str, true);
            pinyin.setText("查拼音：" + strPy);
            pinyin.setOnAction(event -> {
                Clipboard.getSystemClipboard().setContent(Map.of(DataFormat.PLAIN_TEXT, str + "\n" + strPy));
                app.toast("已复制拼音到剪贴板！");
            });
        } else {
            pinyin.setText("查拼音：<选择1~10字>，并点击可复制");
        }

        //
        return new ContextMenu(copy, copyRef,
                new SeparatorMenuItem(),
                search, searchInAbs, lookup, finder,
                new SeparatorMenuItem(),
                dictionary, pinyin
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * for communication from the Javascript engine.
     */
    protected final JavaApp javaApp = new JavaApp();

    public class JavaApp {
        public void log(String msg) {
            System.out.println("javaApp.LOG : " + msg);
        }

        public void updateFinderState(int index, int count) {
            webFinder.state(index, count);
        }

        public void seeAlso(String dictId, String keyword) {
            app.eventBus.fireEvent(DictionaryEvent.ofSearchExact(dictId, keyword));
        }
    }
}
