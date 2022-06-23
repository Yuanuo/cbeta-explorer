package org.appxi.cbeta.app.reader;

import org.appxi.cbeta.app.App;
import org.appxi.javafx.app.DesktopApp;
import org.appxi.javafx.app.web.WebViewerPart;
import org.appxi.javafx.workbench.WorkbenchPane;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public abstract class HtmlBasedViewer extends WebViewerPart.MainView {
    public static List<String> getWebIncludeURIsEx() {
        List<String> result = getWebIncludeURIs();
        final Path dir = DesktopApp.appDir().resolve("template/web-incl");
        result.addAll(Stream.of("html-viewer.css", "html-viewer.js")
                .map(s -> dir.resolve(s).toUri().toString())
                .toList()
        );
        result.add("<link id=\"CSS\" rel=\"stylesheet\" type=\"text/css\" href=\"" + App.app().visualProvider.getWebStyleSheetURI() + "\">");
        return result;
    }

    public HtmlBasedViewer(WorkbenchPane workbench) {
        super(workbench);
    }
}
