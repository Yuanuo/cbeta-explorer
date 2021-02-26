package org.appxi.cbeta.explorer.dao;

import org.appxi.tome.cbeta.CbetaBook;
import org.appxi.tome.model.Book;
import org.appxi.tome.model.Chapter;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.Attributes;
import org.appxi.util.ext.Node;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public interface PiecesRepository extends PieceRepository {

    default void saveCbetaBook(CbetaBook book) {
        final List<Piece> pieces = new ArrayList<>();
        //
        final Piece piece = Piece.of();
        pieces.add(piece);
        piece.project = book.attrStr("project");
        piece.version = book.attrStr("version");
        piece.id = book.id;
        piece.path = book.path;
        piece.type = "book";
        piece.title = book.title;
        //
        piece.fields.put("book_s", book.id);
        if (StringHelper.isNotBlank(book.authorInfo()))
            piece.fields.put("authors_s", book.authorInfo);

        String textFieldSuffix = book.attrStr("text.field.suffix");
        if (null != textFieldSuffix) {
            piece.fields.put("title_txt_".concat(textFieldSuffix), book.title);
        }

        if (StringHelper.isNotBlank(book.summary))
            piece.fields.put("summary_s", book.summary);
        //
        if (StringHelper.isNotBlank(book.location))
            piece.fields.put("location_s", book.location);
        // chapters
        InternalPiecesByCbetaBookBuilder.buildBookChapters(pieces, book, book.chapters);
        //
        if (book.hasAttr("priority"))
            piece.priority = book.attr("priority");
        if (book.hasAttr("sequence"))
            piece.fields.put("sequence_s", book.attrStr("sequence"));
        //
        // add category
        InternalPiecesByCbetaBookBuilder.buildBookCategories(piece, book.path, book);

        // real save
        if (!pieces.isEmpty()) {
            saveAll(pieces);
        }
    }

    class InternalPiecesByCbetaBookBuilder {
        private static void buildBookChapters(List<Piece> pieces, Book book, Node<Chapter> chapterNode) {
            if (null != chapterNode.parent()) {
                final Chapter chapter = chapterNode.value;
                if ("title".equals(chapter.type)) {
                    // do nothing
                } else if ("link".equals(chapter.type)) {
                    // do nothing
                } else if (!chapterNode.hasChildren()) {
                    if (!chapter.hasParagraphs()) {
                        System.out.println(chapter.title);
                    } else {
                        final boolean dataReplacedByParas = chapter.hasAttr("data", "para");
                        final Piece piece = Piece.of();
                        pieces.add(piece);
                        piece.project = book.attrStr("project");
                        piece.version = book.attrStr("version");
                        piece.id = chapter.id;
                        piece.path = chapter.path;
                        piece.type = dataReplacedByParas ? "volume" : "article";
                        piece.title = chapter.title;
                        //
                        piece.fields.put("book_s", book.id);
                        if (chapter.start instanceof String anchor && StringHelper.isNotBlank(anchor))
                            piece.fields.put("anchor_s", anchor);

                        if (StringHelper.isNotBlank(book.authorInfo()))
                            piece.fields.put("authors_s", book.authorInfo);

                        String textFieldSuffix = chapter.attrStr("text.field.suffix");
                        if (null == textFieldSuffix)
                            textFieldSuffix = book.attrStr("text.field.suffix");
                        if (null != textFieldSuffix) {
                            piece.fields.put("title_txt_".concat(textFieldSuffix), chapter.title);
                        }
                        if (StringHelper.isNotBlank(chapter.description))
                            piece.fields.put("summary_s", chapter.description);

                        if (chapter.hasAttr("source"))
                            piece.fields.put("source_s", chapter.attrStr("source"));
                        else piece.fields.put("source_s", book.title);

                        if (chapter.hasAttr("location"))
                            piece.fields.put("location_s", chapter.attrStr("location"));
                        else if (StringHelper.isNotBlank(book.location))
                            piece.fields.put("location_s", book.location);
                        else piece.fields.put("location_s", book.title);

                        //
                        if (!chapter.hasParagraphs()) {
                            //
                        } else if (dataReplacedByParas) {
                            //
                        } else if (chapter.hasParagraphs()) {
                            final StringBuilder buf = new StringBuilder();
                            chapter.paragraphs.forEach(paragraph -> {
                                if (StringHelper.isNotBlank(paragraph.caption) && !paragraph.caption.equals(chapter.title))
                                    buf.append(paragraph.caption).append("。");
                                if (StringHelper.isNotBlank(paragraph.content))
                                    buf.append(paragraph.content);
                            });
                            if (null != textFieldSuffix) {
                                piece.contents.put("content_txt_".concat(textFieldSuffix), buf.toString());
                            }
                        }
                        //
                        if (chapter.hasAttr("priority"))
                            piece.priority = chapter.attr("priority");
                        if (chapter.hasAttr("sequence"))
                            piece.fields.put("sequence_s", chapter.attrStr("sequence"));

                        // add category
                        buildBookCategories(piece, chapter.path, chapter, book);
                    }
                }
            }
            for (Node<Chapter> child : chapterNode.children())
                buildBookChapters(pieces, book, child);
        }

        private static void buildBookCategories(Piece piece, String path, Attributes... attrs) {
            Map<String, String> category = null;
            for (Attributes attr : attrs) {
                category = attr.attr("category");
                if (null != category)
                    break;
            }
            if (null != category) {
                category.forEach((k, v) -> {
                    if (!piece.categories.contains(k))
                        piece.categories.add(k);
                });
            }
        }
    }
}
