//package org.appxi.cbeta.app.widget;
//
//import javafx.geometry.Pos;
//import javafx.scene.Node;
//import javafx.scene.control.Alert;
//import javafx.scene.control.Button;
//import javafx.scene.control.ComboBox;
//import javafx.scene.control.Label;
//import javafx.scene.control.TextField;
//import javafx.scene.control.TreeItem;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.Priority;
//import javafx.scene.layout.VBox;
//import javafx.stage.DirectoryChooser;
//import javafx.stage.FileChooser;
//import org.appxi.cbeta.Book;
//import org.appxi.cbeta.BookList;
//import org.appxi.cbeta.BookMap;
//import org.appxi.cbeta.TripitakaMap;
//import org.appxi.cbeta.app.AppContext;
//import org.appxi.cbeta.app.explorer.BooksProfile;
//import org.appxi.holder.IntHolder;
//import org.appxi.holder.RawHolder;
//import org.appxi.javafx.helper.TreeHelper;
//import org.appxi.util.DigestHelper;
//import org.appxi.util.FileHelper;
//import org.appxi.util.StringHelper;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.zip.ZipFile;
//
//class ImplEpubRenamer extends Widget {
//    ImplEpubRenamer(WidgetsController controller) {
//        super(controller);
//    }
//
//    @Override
//    String getName() {
//        return "CBETA EPUB 重构";
//    }
//
//    @Override
//    Node getViewport() {
//        final Label info = new Label("由于CBETA提供的EPUB电子书使用图书ID作为文件名，若是人来查找需要的图书电子档，实在不太方便。\n此工具用于将所有EPUB文件名称规范化并且导出到合适的目录结构中，以解决此问题。\n");
//        info.setWrapText(true);
//
//        Label sourcePathLabel = new Label("输入（ZIP压缩包）");
//        sourcePathLabel.setStyle("-fx-padding: 1em 0;");
//
//        TextField sourcePathField = new TextField();
//        sourcePathField.setEditable(false);
//        HBox.setHgrow(sourcePathField, Priority.ALWAYS);
//        Button sourcePathBtn = new Button("...");
//        RawHolder<File> sourceFileHold = new RawHolder<>();
//        sourcePathBtn.setOnAction(evt -> {
//            FileChooser fc = new FileChooser();
//            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("zip", "*.zip"));
//            sourceFileHold.value = fc.showOpenDialog(this.controller.app.getPrimaryStage());
//            if (null != sourceFileHold.value) {
//                sourcePathField.setText(sourceFileHold.value.getAbsolutePath());
//            }
//        });
//        HBox sourcePathHBox = new HBox(sourcePathField, sourcePathBtn);
//
//        Label targetTypeLabel = new Label("使用目录模式");
//        targetTypeLabel.setStyle("-fx-padding: 1em 0;");
//
//        ComboBox<BooksProfile.Profile> targetTypeBox = new ComboBox<>();
//        targetTypeBox.getItems().addAll(
//                BooksProfile.Profile.bulei,
//                BooksProfile.Profile.simple,
//                BooksProfile.Profile.advance);
//        targetTypeBox.getSelectionModel().select(0);
//
//        Label targetPathLabel = new Label("输出到文件夹");
//        targetPathLabel.setStyle("-fx-padding: 1em 0;");
//
//        TextField targetPathField = new TextField();
//        targetPathField.setEditable(false);
//        HBox.setHgrow(targetPathField, Priority.ALWAYS);
//        Button targetPathBtn = new Button("...");
//        targetPathBtn.setOnAction(evt -> {
//            DirectoryChooser dc = new DirectoryChooser();
//            if (null != sourceFileHold.value)
//                dc.setInitialDirectory(sourceFileHold.value.getParentFile());
//            File selDir = dc.showDialog(this.controller.app.getPrimaryStage());
//            if (null != selDir)
//                targetPathField.setText(selDir.getAbsolutePath());
//        });
//        HBox targetPathHBox = new HBox(targetPathField, targetPathBtn);
//
//        Button applyBtn = new Button("Apply");
//        applyBtn.setOnAction(event -> {
//            String result = handleApplyAction(sourcePathField.getText(),
//                    targetTypeBox.getSelectionModel().getSelectedItem(), targetPathField.getText());
//            Alert alert = new Alert(Alert.AlertType.INFORMATION);
//            alert.setHeaderText(null);
//            alert.setContentText(result);
//            alert.initOwner(controller.app.getPrimaryStage());
//            alert.show();
//        });
//        HBox applyBtnHBox = new HBox(applyBtn);
//        applyBtnHBox.setAlignment(Pos.CENTER);
//        applyBtnHBox.setStyle("-fx-padding: 1em 0;");
//
//        return new VBox(info,
//                sourcePathLabel, sourcePathHBox,
//                targetTypeLabel, targetTypeBox,
//                targetPathLabel, targetPathHBox,
//                applyBtnHBox);
//    }
//
//    private String handleApplyAction(String sourcePath, BooksProfile.Profile profile, String targetPath) {
//        if (sourcePath.isBlank() || profile == null || targetPath.isBlank())
//            return "未指定 输入文件 或 目录模式 或 输出目录";
//
//        Path source = Path.of(sourcePath);
//        if (FileHelper.notExists(source))
//            return "输入文件不正确";
//        Path target = Path.of(targetPath);
//        FileHelper.makeDirs(target);
//        if (!Files.isWritable(target))
//            return "输出目录不正确";
//
//        final TripitakaMap tripitakaMap = new TripitakaMap(AppContext.bookcase());
//        final BookMap bookMap = new BookMap(tripitakaMap);
//        final BookList<TreeItem<Book>> bookList = new BooksProfile.BooksListTree(bookMap, profile);
//
//        final Map<String, TreeItem<Book>> booklistNodes = new HashMap<>(512);
//        TreeHelper.walkLeafs(bookList.tree(), (treeItem, book) -> {
//            if (null == book || null == book.path) return;
//            booklistNodes.put(book.id, treeItem);
//        });
//
//        IntHolder totalNum = new IntHolder(0);
//        IntHolder savedNum = new IntHolder(0);
//        try (ZipFile zipFile = new ZipFile(source.toFile())) {
//            zipFile.stream().filter(entry -> entry.getName().endsWith(".epub")).forEach(entry -> {
//                totalNum.value++;
//                String bookPath = entry.getName().replace("\\", "/");
//                bookPath = bookPath.substring(bookPath.indexOf("/") + 1);
//
//                String bookName;
//                String bookId = bookPath.substring(bookPath.lastIndexOf("/") + 1);
//                bookPath = bookPath.substring(bookPath.lastIndexOf("/") + 1);
//                bookId = bookId.substring(0, bookId.length() - 5);// remove ".epub"
//                TreeItem<Book> bookNode = booklistNodes.get(bookId);
//                if (null == bookNode)
//                    bookNode = booklistNodes.get(bookId + "a");
//                if (null != bookNode) {
//                    bookPath = TreeHelper.path(bookNode);
//                    bookPath = bookPath.substring(0, bookPath.lastIndexOf('/'));
//                    bookPath = bookPath.replaceAll("[<>:\"|?*]", "");
//                    // build name
//                    bookName = bookNode.getValue().title;
//                    if (StringHelper.isNotBlank(bookNode.getValue().authorInfo()))
//                        bookName = bookName + " - " + bookNode.getValue().authorInfo.replace(" ", "");
//                    bookName = bookName.replaceAll("[<>:\"|?*]", "");
//                    if (bookName.length() > 200)
//                        bookName = bookName.substring(0, 200) + ",etc";
//                } else {
//                    bookName = bookId;
//                }
//                try {
//                    Path targetFile = target.resolve(bookPath).resolve(bookName + ".epub");
//                    if (FileHelper.exists(targetFile))
//                        targetFile = target.resolve(bookPath).resolve(bookName + "(" + bookId + ")" + ".epub");
//                    if (FileHelper.exists(targetFile))
//                        targetFile = target.resolve(bookPath).resolve(bookName + "(" + DigestHelper.uid() + ")" + ".epub");
//                    FileHelper.makeParents(targetFile);
//                    Files.copy(zipFile.getInputStream(entry), targetFile);
//                    savedNum.value++;
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            });
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return StringHelper.concat("操作完成。导出 / 总数：", savedNum.value, " / ", totalNum.value);
//    }
//
//    @Override
//    void activeViewport(boolean firstTime) {
//
//    }
//}
