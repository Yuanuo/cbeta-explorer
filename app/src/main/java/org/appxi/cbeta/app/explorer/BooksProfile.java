package org.appxi.cbeta.app.explorer;

import javafx.geometry.Pos;
import javafx.scene.Node;
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
import org.appxi.cbeta.BooksList;
import org.appxi.cbeta.BooksMap;
import org.appxi.cbeta.app.App;
import org.appxi.cbeta.app.AppContext;
import org.appxi.cbeta.app.event.GenericEvent;
import org.appxi.javafx.control.CardChooser;
import org.appxi.javafx.control.ProgressLayer;
import org.appxi.javafx.control.TreeViewEx;
import org.appxi.javafx.helper.TreeHelper;
import org.appxi.javafx.visual.MaterialIcon;
import org.appxi.prefs.Preferences;
import org.appxi.prefs.PreferencesInProperties;
import org.appxi.prefs.UserPrefs;
import org.appxi.util.DigestHelper;
import org.appxi.util.FileHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class BooksProfile {
    public static final BooksProfile ONE = new BooksProfile();
    private static final Path profileDir = UserPrefs.dataDir().resolve(".profiles");
    private static final Preferences profileMgr = new PreferencesInProperties(profileDir.resolve(".list"));

    private Profile profile;
    private String profileVersion;
    private BooksList<TreeItem<Book>> booksList;
    private final Map<String, Book> managedBooks = new HashMap<>(1024);

    public BooksProfile() {
    }

    public Profile profile() {
        return this.profile;
    }

    public Book getBook(String id) {
        return managedBooks.get(id);
    }

    public Collection<Book> getManagedBooks() {
        return managedBooks.values();
    }

    public BooksList<TreeItem<Book>> booklist() {
        return booksList;
    }

    public boolean loadProfile() {
        final String profile = profileMgr.getString("profile", "");
        return !profile.isBlank() && loadProfile(Profile.valueBy(profile), true);
    }

    private boolean loadProfile(final Profile profile, boolean silent) {
        if (null == profile) return false;
        // same as previous, do nothing
        if (this.profile == profile && Objects.equals(this.profileVersion, profile.version())) return true;
        if (profile.isManaged()) {
            // need attention
            if (!silent && Files.notExists(profileDir.resolve(profile.filename()))) {
                manageProfile(profile);
            }
        }
        try {
            if (profile.isManaged()) {
                this.booksList = new BooksListFilteredTree(AppContext.booksMap(), profile) {
                    @Override
                    protected TreeItem<Book> createTreeItem(Element item, Book itemValue) {
                        if (null != itemValue && null != itemValue.id)
                            managedBooks.put(itemValue.id, itemValue);
                        return super.createTreeItem(item, itemValue);
                    }
                };
            } else {
                this.booksList = new BooksListTree(AppContext.booksMap(), profile) {
                    @Override
                    protected TreeItem<Book> createTreeItem(Element item, Book itemValue) {
                        if (null != itemValue && null != itemValue.id)
                            managedBooks.put(itemValue.id, itemValue);
                        return super.createTreeItem(item, itemValue);
                    }
                };
            }
            this.managedBooks.clear();
            this.profile = profile;
            this.profileVersion = profile.version();
            profileMgr.setProperty("profile", profile.name());
            profileMgr.save();
            //
            ProgressLayer.showAndWait(App.app().getPrimaryGlass(), progressLayer -> {
                this.booksList.tree();
                App.app().eventBus.fireEvent(new GenericEvent(GenericEvent.PROFILE_READY, profile));
            });
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    void selectProfile(Profile profile) {
        while (true) {
            // same as previous, do nothing
            if (null != this.profile && this.profile == profile)
                return;
            // try load profile
            if (this.loadProfile(profile, false))
                return;
            //
            final Optional<CardChooser.Card> optional = CardChooser.of("选择书单")
                    .owner(App.app().getPrimaryStage())
                    .cards(Stream.of(Profile.values())
                            .map(p -> CardChooser.ofCard(p.toString())
                                    .description(p.description())
                                    .graphic(MaterialIcon.PLAYLIST_ADD_CHECK.graphic())
                                    .focused(p == this.profile)
                                    .userData(p)
                                    .get())
                            .toList())
                    .showAndWait();
            profile = optional.isEmpty() || optional.get().userData() == null
                    ? this.profile
                    : optional.get().userData();

            if (profile == null)
                App.app().stop();
        }
    }

    void manageProfiles() {
        CardChooser.of("管理我的书单")
                .owner(App.app().getPrimaryStage())
                .cards(Stream.of(Profile.values()).filter(Profile::isManaged)
                        .map(p -> CardChooser.ofCard(p.toString())
                                .description(p.description())
                                .graphic(MaterialIcon.EDIT_NOTE.graphic())
                                .focused(p == this.profile)
                                .userData(p)
                                .get())
                        .toList())
                .showAndWait()
                .ifPresent(card -> manageProfile(card.userData()));
    }

    void manageProfile(Profile profile) {
        new ProfileEditor(profile).showAndWait();
    }

    public enum Profile {
        bulei("默认书单：部类目录"),
        simple("默认书单：简易原书目录"),
        advance("默认书单：进阶原书目录"),
        profile1("我的书单1：未定义"),
        profile2("我的书单2：未定义"),
        profile3("我的书单3：未定义"),
        profile4("我的书单4：未定义"),
        profile5("我的书单5：未定义"),
        profile6("我的书单6：未定义"),
        profile7("我的书单7：未定义"),
        profile8("我的书单8：未定义"),
        profile9("我的书单9：未定义"),
        profile10("我的书单10：未定义"),
        profile11("我的书单11：未定义"),
        profile12("我的书单12：未定义"),
        profile13("我的书单13：未定义"),
        profile14("我的书单14：未定义"),
        profile15("我的书单15：未定义"),
        profile16("我的书单16：未定义"),
        profile17("我的书单17：未定义"),
        profile18("我的书单18：未定义"),
        profile19("我的书单19：未定义"),
        profile20("我的书单20：未定义"),
        //
        ;
        private final String privateTitle;

        Profile(String privateTitle) {
            this.privateTitle = privateTitle;
        }

        public boolean isManaged() {
            return this.name().startsWith("profile");
        }

        @Override
        public String toString() {
            return !this.isManaged() ? AppContext.hanText(this.privateTitle)
                    : profileMgr.getString(name().concat(".title"), privateTitle);
        }

        public static Profile valueBy(String profile) {
            for (Profile itm : values())
                if (itm.name().equals(profile)) return itm;
            return bulei;// avoid NPE
        }

        public String filename() {
            return !this.isManaged() ? name().concat("_nav.xhtml") : name().concat(".xml");
        }

        public Profile template() {
            return !this.isManaged() ? this
                    : valueBy(profileMgr.getString(name().concat(".based"), "bulei"));
        }

        public String description() {
            return !this.isManaged() ? ""
                    : profileMgr.getString(name().concat(".desc"), "");
        }

        public String version() {
            return !this.isManaged() ? AppContext.bookcase().getVersion()
                    : profileMgr.getString(name().concat(".ver"), "");
        }

        public String indexesId() {
            return this.isManaged() ? this.name() : "profile0";
        }
    }

    private static class ProfileEditor extends DialogPane {
        final BooksProfile.Profile profile;
        final VBox form;

        TextField title, description;
        Profile template;
        TreeView<Book> treeView;

        public ProfileEditor(BooksProfile.Profile profile) {
            super();
            this.profile = profile;

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
            this.template = this.profile.template();
            Button selTemplate = new Button("从模板创建");
            selTemplate.setOnAction(event -> CardChooser.of("选择模板")
                    .cards(Stream.of(Profile.bulei,
                                    Profile.simple,
                                    Profile.advance)
                            .map(p -> CardChooser.ofCard(p.toString())
                                    .graphic(MaterialIcon.PLAYLIST_ADD.graphic())
                                    .userData(p).get())
                            .toList())
                    .showAndWait()
                    .ifPresent(card -> {
                        template = card.userData();
                        try {
                            setBooklist(template);
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
        protected Node createButton(ButtonType buttonType) {
            Button button = (Button) super.createButton(buttonType);
            if (buttonType == ButtonType.OK)
                button.setText("保存");
            else if (buttonType == ButtonType.CANCEL)
                button.setText("取消");
            return button;
        }

        public Optional<ButtonType> showAndWait() {
            final Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("编辑我的书单: ".concat(profile.toString()));
            this.setPrefSize(540, 720);
            dialog.setDialogPane(this);
            //
            final Path profilePath = profileDir.resolve(profile.filename());
            try {
                setBooklist(profile);
            } catch (Throwable ex) {
                try {
                    setBooklist(profile.template());
                } catch (Throwable ex2) {
                    ex2.printStackTrace();
                }
            }
            //
            dialog.initOwner(App.app().getPrimaryStage());
            final Optional<ButtonType> optional = dialog.showAndWait();
            if (optional.isEmpty() || optional.get() == ButtonType.CANCEL) return optional;
            //
            //
            String title = this.title.getText().strip();
            if (title.isBlank()) title = profile.privateTitle;
            profileMgr.setProperty(profile.name().concat(".title"), title);
            profileMgr.setProperty(profile.name().concat(".desc"), this.description.getText().strip());
            profileMgr.setProperty(profile.name().concat(".based"), template.name());
            profileMgr.setProperty(profile.name().concat(".basedVer"), template.version());

            Document document = Jsoup.parse("""
                    <?xml version="1.0" encoding="utf-8"?>
                    <html>
                    	<head><meta charset="UTF-8" /></head>
                        <body><nav></nav></body>
                    </html>
                    """);
            Element nav = document.body().selectFirst("nav");
            TreeHelper.walkTree(treeView.getRoot(), new TreeHelper.TreeWalker<>() {
                Element node = nav;

                @Override
                public void start(TreeItem<Book> treeItem, Book itemValue) {
                    if (null == itemValue) return;
                    node = node.appendElement("li");

                    node.appendElement("span")
                            .attr("v", getSelectedState(treeItem))
                            .attr("t", itemValue.title);
                    node = node.appendElement("ol");
                }

                @Override
                public void visit(TreeItem<Book> treeItem, Book itemValue) {
                    if (null == itemValue) return;
                    node.appendElement("li")
                            .appendElement("a")
                            .attr("href", itemValue.path)
                            .attr("v", getSelectedState(treeItem))
                            .attr("i", itemValue.id)
                            .text(itemValue.title);
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
            if (document.body().selectFirst("nav").children().isEmpty())
                FileHelper.delete(profilePath);
            else saveDocument(document, profilePath, true);

            final String oldMd5Ver = profile.version();
            final String newMd5Ver = DigestHelper.md5(profilePath);
            profileMgr.setProperty(profile.name().concat(".ver"), newMd5Ver);
            profileMgr.setProperty(profile.name().concat(".mod"), FileHelper.fileTime(profilePath));
            profileMgr.save();

            if (!Objects.equals(oldMd5Ver, newMd5Ver)) {
                if (profile == ONE.profile) {
                    ONE.loadProfile();
                }
            }
            //
            return optional;
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

        private void setBooklist(Profile profile) {
            final BooksListStatefulTree booklist = new BooksListStatefulTree(AppContext.booksMap(), profile);
            final CheckBoxTreeItem<Book> rootItem = booklist.tree();
            rootItem.setExpanded(true);
            rootItem.setIndependent(true);
            treeView.setRoot(rootItem);
        }
    }

    private static InputStream getProfileStream(Profile profile) {
        if (profile.isManaged()) {
            final Path profilePath = profileDir.resolve(profile.filename());
            try {
                return Files.newInputStream(profilePath);
            } catch (NoSuchFileException ignore) {
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        } else {
            try {
                return AppContext.bookcase().getContentAsStream(profile.filename());
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public static class BooksListTree extends BooksList<TreeItem<Book>> {
        public BooksListTree(BooksMap booksMap, Profile profile) {
            super(booksMap, new TreeItem<>(null), getProfileStream(profile));
        }

        @Override
        protected TreeItem<Book> createTreeItem(Element item, Book itemValue) {
            return new TreeItem<>(itemValue);
        }

        @Override
        protected void relinkChildren(TreeItem<Book> parent, List<TreeItem<Book>> children) {
            parent.getChildren().addAll(children);
        }
    }

    public static class BooksListFilteredTree extends BooksListTree {
        protected final boolean isProfileManaged;

        public BooksListFilteredTree(BooksMap booksMap, Profile profile) {
            super(booksMap, profile);
            isProfileManaged = profile.isManaged();
        }

        @Override
        protected final boolean acceptDataItem(Element item) {
            return !isProfileManaged || switch (item.attr("v")) {
                case "1", "2" -> true;
                default -> false;
            };
        }
    }

    public static class BooksListStatefulTree extends BooksList<CheckBoxTreeItem<Book>> {
        public BooksListStatefulTree(BooksMap booksMap, Profile profile) {
            super(booksMap, new CheckBoxTreeItem<>(null), getProfileStream(profile));
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
