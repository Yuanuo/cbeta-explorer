//package org.appxi.cbeta.app.explorer;
//
//import javafx.geometry.Pos;
//import javafx.scene.Node;
//import javafx.scene.control.Button;
//import javafx.scene.control.ButtonType;
//import javafx.scene.control.CheckBoxTreeItem;
//import javafx.scene.control.Dialog;
//import javafx.scene.control.DialogPane;
//import javafx.scene.control.Label;
//import javafx.scene.control.TextField;
//import javafx.scene.control.TreeItem;
//import javafx.scene.control.TreeView;
//import javafx.scene.control.cell.CheckBoxTreeCell;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.Priority;
//import javafx.scene.layout.VBox;
//import org.appxi.cbeta.Book;
//import org.appxi.cbeta.Profile;
//import org.appxi.cbeta.app.App;
//import org.appxi.cbeta.app.AppContext;
//import org.appxi.javafx.control.CardChooser;
//import org.appxi.javafx.control.TreeViewEx;
//import org.appxi.javafx.helper.TreeHelper;
//import org.appxi.javafx.visual.MaterialIcon;
//import org.appxi.util.DigestHelper;
//import org.appxi.util.FileHelper;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Optional;
//import java.util.stream.Stream;
//
//public class BooksProfile {
//    public static final BooksProfile ONE = new BooksProfile();
//
//    private Profile profile;
//    private String profileVersion;
//
//    public Profile profile() {
//        return this.profile;
//    }
//
//    public Collection<Book> getManagedBooks() {
//        return managedBooks.values();
//    }
//}
