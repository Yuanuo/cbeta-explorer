package org.appxi.cbeta.app;

import javafx.scene.control.TreeItem;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.BookList;
import org.appxi.cbeta.BookMap;
import org.appxi.cbeta.Bookcase;
import org.appxi.cbeta.Profile;
import org.appxi.util.FileHelper;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataContext {
    private final FileLock fileLock;
    public final Bookcase bookcase;
    public final BookMap bookMap;
    public final Profile profile;
    private BookList<TreeItem<Book>> bookList;

    private final Map<String, Book> managedBooks = new HashMap<>(1024);

    public DataContext(Bookcase bookcase, BookMap bookMap, Profile profile) throws Exception {
        this.bookcase = bookcase;
        this.bookMap = bookMap;
        this.profile = profile;

        // check lockable
        final Path lockFile = profile.workspace().resolve(".lock");
        if (FileHelper.exists(lockFile)) {
            FileHelper.delete(lockFile);
            if (FileHelper.exists(lockFile)) {
                throw new IllegalAccessException("书单被锁定，无法打开。");
            }
        }
        // try lock this workspace
        FileHelper.makeDirs(profile.workspace());
        this.fileLock = new RandomAccessFile(lockFile.toFile(), "rw").getChannel().tryLock();
        if (null == fileLock) {
            throw new RuntimeException();
        }
    }

    public void reloadBookList() {
        if (profile.isManaged()) {
            this.bookList = new BookListFilteredTree(bookMap, profile) {
                @Override
                protected TreeItem<Book> createTreeItem(Element item, Book itemValue) {
                    if (null != itemValue && null != itemValue.id) {
                        managedBooks.put(itemValue.id, itemValue);
                    }
                    return super.createTreeItem(item, itemValue);
                }
            };
        } else {
            this.bookList = new BookListTree(bookMap, profile) {
                @Override
                protected TreeItem<Book> createTreeItem(Element item, Book itemValue) {
                    if (null != itemValue && null != itemValue.id) {
                        managedBooks.put(itemValue.id, itemValue);
                    }
                    return super.createTreeItem(item, itemValue);
                }
            };
        }
    }

    public BookList<TreeItem<Book>> booklist() {
        return bookList;
    }

    public Book getBook(String id) {
        return managedBooks.get(id);
    }

    static InputStream getProfileStream(Profile profile) {
        if (profile.isManaged()) {
            final Path profileFile = profile.workspace().resolve(profile.filename());
            try {
                return Files.newInputStream(profileFile);
            } catch (NoSuchFileException ignore) {
                return new ByteArrayInputStream("""
                        <!--?xml version="1.0" encoding="utf-8"?-->
                        <html>
                        	<head><meta charset="UTF-8" /></head>
                            <body><nav></nav></body>
                        </html>
                        """.getBytes(StandardCharsets.UTF_8));
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

    public Collection<Book> getManagedBooks() {
        return managedBooks.values();
    }

    public void release() {
        if (null != fileLock) {
            try {
                fileLock.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        FileHelper.delete(profile.workspace().resolve(".lock"));
    }

    public static class BookListTree extends BookList<TreeItem<Book>> {
        public BookListTree(BookMap bookMap, Profile profile) {
            super(bookMap, new TreeItem<>(null), getProfileStream(profile));
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

    public static class BookListFilteredTree extends BookListTree {
        protected final boolean isProfileManaged;

        public BookListFilteredTree(BookMap bookMap, Profile profile) {
            super(bookMap, profile);
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
}
