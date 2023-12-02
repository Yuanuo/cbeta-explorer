package org.appxi.cbeta.app;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.BookList;
import org.appxi.cbeta.BookMap;
import org.appxi.cbeta.Profile;
import org.appxi.javafx.control.CardChooser;
import org.appxi.javafx.control.TreeViewEx;
import org.appxi.javafx.helper.FxHelper;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.util.DigestHelper;
import org.appxi.util.FileHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.appxi.cbeta.app.DataContext.getProfileStream;

class ProfileEditor extends DialogPane {
    final DataApp dataApp;
    final Profile.ExternalProfile profile;
    final VBox form;

    TextField title, description;
    Button selTemplate;
    Profile template;
    TreeView<Book> treeView;

    public ProfileEditor(DataApp dataApp) {
        super();
        this.dataApp = dataApp;
        this.profile = (Profile.ExternalProfile) dataApp.profile;

        this.form = new VBox(10);
        this.form.getStyleClass().add("form");
        //
        this.edit_title();
        this.edit_description();
        this.edit_booklist();
        //
        this.setContent(form);

        this.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    }

    private void edit_title() {
        final Label label = new Label("显示名称");
        label.getStyleClass().add("field-label");

        title = new TextField(profile.toString());
        HBox.setHgrow(title, Priority.ALWAYS);
        //
        final HBox hBox = new HBox(label, title);
        hBox.setAlignment(Pos.CENTER_LEFT);
        this.form.getChildren().addAll(hBox);
    }

    private void edit_description() {
        final Label label = new Label("备注");
        label.getStyleClass().add("field-label");

        description = new TextField(profile.description());
        HBox.setHgrow(description, Priority.ALWAYS);
        //
        final HBox hBox = new HBox(label, description);
        hBox.setAlignment(Pos.CENTER_LEFT);
        this.form.getChildren().addAll(hBox);
    }

    private void edit_booklist() {
        final Label label = new Label("书单");
        label.getStyleClass().add("field-label");
        //
        template = dataApp.basedApp.profileMgr.getProfile(this.profile.template());
        selTemplate = new Button("从模板创建");
        selTemplate.setOnAction(event -> CardChooser.of("选择模板")
                .cards(dataApp.basedApp.profileMgr.getProfiles().stream().filter(p -> !p.isManaged())
                        .map(p -> CardChooser.ofCard(p.toString())
                                .graphic(MaterialIcon.PLAYLIST_ADD.graphic())
                                .userData(p).get())
                        .toList())
                .showAndWait()
                .ifPresent(card -> {
                    template = card.userData();
                    try {
                        setBookList(template);
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }));
        //
        Button selAll = new Button("全选");
        selAll.setOnAction(event -> TreeHelper.walkLeafs(treeView.getRoot(),
                (treeItem, book) -> ((CheckBoxTreeItem<Book>) treeItem).setSelected(true)));
        //
        Button selNone = new Button("全不选");
        selNone.setOnAction(event -> TreeHelper.walkLeafs(treeView.getRoot(),
                (treeItem, book) -> ((CheckBoxTreeItem<Book>) treeItem).setSelected(false)));
        //
        Button selInvert = new Button("反选");
        selInvert.setOnAction(event -> TreeHelper.walkLeafs(treeView.getRoot(),
                (treeItem, book) -> ((CheckBoxTreeItem<Book>) treeItem).setSelected(!((CheckBoxTreeItem<Book>) treeItem).isSelected())));

        HBox toolbar = new HBox(10, selTemplate, selAll, selNone, selInvert);

        treeView = new TreeViewEx<>();
        treeView.setCellFactory(CheckBoxTreeCell.forTreeView());
        VBox.setVgrow(treeView, Priority.ALWAYS);
        //
        VBox vBox = new VBox(10, toolbar, treeView);
        HBox.setHgrow(vBox, Priority.ALWAYS);
        //
        HBox hBox = new HBox(label, vBox);
        VBox.setVgrow(hBox, Priority.ALWAYS);
        this.form.getChildren().addAll(hBox);
    }

    @Override
    protected Button createButton(ButtonType buttonType) {
        Button button = (Button) super.createButton(buttonType);
        if (buttonType == ButtonType.OK)
            button.setText("保存");
        else if (buttonType == ButtonType.CANCEL)
            button.setText("取消");
        return button;
    }

    public Optional<Boolean> showAndWait() {
        final Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("我的书单: ".concat(profile.toString()));
        this.setPrefSize(540, 720);
        dialog.setDialogPane(this);
        //
        final Path profilePath = profile.workspace().resolve(profile.filename());
        try {
            setBookList(profile);
        } catch (Throwable ex) {
            try {
                setBookList(template);
            } catch (Throwable ex2) {
                ex2.printStackTrace();
            }
        }
        //
        dialog.initOwner(dataApp.basedApp.getPrimaryStage());
        dialog.setOnShown(evt -> {
            FxHelper.runThread(1000, () -> {
                if (treeView.getRoot().getChildren().isEmpty()) {
                    selTemplate.fire();
                }
            });
        });
        final Optional<ButtonType> optional = dialog.showAndWait();
        if (optional.isEmpty() || optional.get() == ButtonType.CANCEL) {
            return Optional.of(false);
        }
        //
        //
        String title = this.title.getText().strip();
        if (title.isBlank()) {
            title = profile.title();
        }
        profile.config.setProperty("name", title);
        profile.config.setProperty("desc", this.description.getText().strip());
        profile.config.setProperty("based", template.id());
        profile.config.setProperty("based.ver", template.version());

        Document document = Jsoup.parse("""
                <?xml version="1.0" encoding="utf-8"?>
                <html>
                    <head><meta charset="UTF-8" /></head>
                    <body><nav></nav></body>
                </html>
                """);
        Element nav = document.body().selectFirst("nav");
        if (null != bookList) {
            if (!bookList.getHanLang().name().equals("zh-tw")) {
                nav.attr("han-lang", bookList.getHanLang().name());
            }
        }
        TreeHelper.walkTree(treeView.getRoot(), new TreeHelper.TreeWalker<>() {
            Element node = nav;

            @Override
            public void start(TreeItem<Book> treeItem, Book itemValue) {
                if (null == itemValue) return;
                node = node.appendElement("li");

                Element ele = node.appendElement("span")
                        .attr("v", getSelectedState(treeItem))
                        .attr("t", itemValue.title);
                if (null != itemValue.id && !itemValue.id.isEmpty()) {
                    ele.attr("i", itemValue.id);
                }
                node = node.appendElement("ol");
            }

            @Override
            public void visit(TreeItem<Book> treeItem, Book itemValue) {
                if (null == itemValue) return;
                Element ele = node.appendElement("li")
                        .appendElement("a")
                        .attr("href", itemValue.path)
                        .attr("v", getSelectedState(treeItem))
                        .text(itemValue.title);
                if (null != itemValue.id && !itemValue.id.isEmpty()) {
                    ele.attr("i", itemValue.id);
                }
                if (null != itemValue.authorInfo && !itemValue.authorInfo.isEmpty()) {
                    ele.attr("a", itemValue.authorInfo);
                }
            }

            @Override
            public void close(TreeItem<Book> treeItem, Book itemValue) {
                if (null == itemValue) return;
                node = node.parent();
                if (null != node)
                    node = node.parent();
            }

            private String getSelectedState(TreeItem<Book> treeItem) {
                if (((CheckBoxTreeItem<Book>) treeItem).isSelected())
                    return "1";
                else if (((CheckBoxTreeItem<Book>) treeItem).isIndeterminate())
                    return "2";
                else return "0";
            }
        });
        if (document.body().selectFirst("nav").children().isEmpty()) {
            FileHelper.delete(profilePath);
        } else {
            saveDocument(document, profilePath, true);
        }

        final String oldMd5Ver = profile.version();
        final String newMd5Ver = DigestHelper.md5(profilePath);
        profile.config.setProperty("version", newMd5Ver);
        profile.config.setProperty("mod", FileHelper.fileTime(profilePath));
        profile.config.save();
        dataApp.title2.set(title);

        // 重新保存的文件的MD5与上一次相同认为未发生改变
        if (Objects.equals(oldMd5Ver, newMd5Ver)) {
            return Optional.of(false);
        }

        dataApp.loadProfile();
        return Optional.of(true);
    }

    static boolean saveDocument(Document document, Path targetFile, boolean xmlMode) {
        FileHelper.makeParents(targetFile);
        try {
            if (xmlMode)
                document.outputSettings().prettyPrint(false).syntax(Document.OutputSettings.Syntax.xml);
            Files.writeString(targetFile, document.outerHtml());
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    BookListStatefulTree bookList;

    private void setBookList(Profile profile) {
        if (null == profile) {
            return;
        }
        bookList = new BookListStatefulTree(AppContext.bookMap(), profile);
        final CheckBoxTreeItem<Book> rootItem = bookList.tree();
        rootItem.setExpanded(true);
        rootItem.setIndependent(true);
        treeView.setRoot(rootItem);
    }

    public static class BookListStatefulTree extends BookList<CheckBoxTreeItem<Book>> {
        public BookListStatefulTree(BookMap bookMap, Profile profile) {
            super(bookMap, new CheckBoxTreeItem<>(null), getProfileStream(profile));
        }

        @Override
        protected CheckBoxTreeItem<Book> createTreeItem(Element item, Book itemValue) {
            final CheckBoxTreeItem<Book> treeItem = new CheckBoxTreeItem<>(itemValue);
            switch (item.attr("v")) {
                case "0" -> treeItem.setSelected(false);
                case "1" -> treeItem.setSelected(true);
                case "2" -> treeItem.setIndeterminate(true);
            }
            return treeItem;
        }

        @Override
        protected void relinkChildren(CheckBoxTreeItem<Book> parent, List<CheckBoxTreeItem<Book>> children) {
            parent.getChildren().addAll(children);
        }
    }
}
