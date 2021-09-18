package org.appxi.cbeta.explorer.widget;

import org.appxi.javafx.glyphfont.MaterialIcon;
import org.appxi.javafx.glyphfont.MaterialIconView;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.appxi.javafx.workbench.WorkbenchApplication;
import org.appxi.javafx.workbench.views.WorkbenchSideViewController;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.Attributes;

import java.util.ArrayList;
import java.util.List;

public class WidgetsController extends WorkbenchSideViewController {
    private static final Object AK_FIRST_TIME = new Object();

    final List<Widget> widgets = new ArrayList<>();

    public WidgetsController(WorkbenchApplication application) {
        super("WIDGETS", application);
        this.setTitles("工具", "辅助工具集");
        this.viewIcon.set(new MaterialIconView(MaterialIcon.NOW_WIDGETS));
    }

    @Override
    public void setupInitialize() {
    }

    @Override
    protected void onViewportInitOnce() {
    }

    @Override
    public void onViewportShowing(boolean firstTime) {
        // only init at first time
        if (!firstTime) return;

        this.createWidgetsOnce();

        //
        List<TitledPane> widgetList = new ArrayList<>(this.widgets.size());
        for (int i = 0; i < this.widgets.size(); i++) {
            Widget widget = this.widgets.get(i);
            TitledPane pane = new TitledPane();
            pane.setText(StringHelper.concat(i + 1, " . ", widget.getName()));
            pane.setUserData(widget);
            widgetList.add(pane);
        }

        final Accordion accordion = new Accordion(widgetList.toArray(new TitledPane[0]));
        VBox.setVgrow(accordion, Priority.ALWAYS);
        accordion.expandedPaneProperty().addListener((o, ov, pane) -> {
            if (null == pane) return;
            Widget widget = (Widget) pane.getUserData();
            if (null == pane.getContent()) {
                pane.setContent(widget.getViewport());
            }
            widget.onViewportShowing(ensureFirstTime(widget));
        });
        this.viewportVBox.getChildren().add(accordion);
    }

    @Override
    public void onViewportHiding() {
    }

    private static boolean ensureFirstTime(Attributes attrs) {
        if (attrs.hasAttr(AK_FIRST_TIME))
            return false;
        attrs.attr(AK_FIRST_TIME, true);
        return true;
    }

    private void createWidgetsOnce() {
        this.widgets.add(new ImplEpubRenamer(this));
        this.widgets.add(new ImplHanLangConvertor(this));
    }
}
