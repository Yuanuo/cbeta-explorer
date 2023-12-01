package org.appxi.cbeta.app.search;

import org.appxi.book.Chapter;
import org.appxi.cbeta.Book;
import org.appxi.cbeta.BookDocument;
import org.appxi.cbeta.ChapterTree;
import org.appxi.cbeta.Tripitaka;
import org.appxi.cbeta.VolumeDocument;
import org.appxi.cbeta.app.AppContext;
import org.appxi.cbeta.xml.LinkedXmlFilter;
import org.appxi.holder.StringHolder;
import org.appxi.search.solr.Piece;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.Attributes;
import org.appxi.util.ext.Node;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

abstract class IndexingHelper {
    static void prepareBookBasic(final String dataProject, final String dataVersion, final Tripitaka tripitaka, final Book book) {
        if (null == book || null == book.path) return;
        book.attr("project", dataProject);
        book.attr("version", dataVersion);
        if (!book.hasAttr("category")) {
            final StringHolder library = new StringHolder();
            if (book.path.startsWith("a/")) {
                final BookDocument bookDoc = book.attrOr(BookDocument.class, () -> new BookDocument(AppContext.bookcase(), book));
                final VolumeDocument volDoc = bookDoc.getVolumeDocument(book.path);
                final Elements htmlMetadata = volDoc.getDocument().head().select("meta");
                Optional.ofNullable(htmlMetadata.select("meta[library]").first()).ifPresent(e -> library.value = e.attr("library"));
                Optional.ofNullable(htmlMetadata.select("meta[catalog]").first()).ifPresent(e -> book.catalog = e.attr("catalog"));
                htmlMetadata.select("meta[period]").forEach(ele -> book.periods.add(ele.attr("period")));
                htmlMetadata.select("meta[author]").forEach(ele -> book.authors.add(ele.attr("author")));
            }

            final Map<String, String> category = new LinkedHashMap<>();
            book.attr("category", category);
            buildCategoryPaths(category, "std/tripitaka", StringHelper.isNotBlank(library.value) ? library.value : (null == tripitaka ? "其他" : tripitaka.name));
            buildCategoryPaths(category, "std/catalog", StringHelper.isNotBlank(book.catalog) ? book.catalog : "其他");
            book.authorInfo();// call for init periods/authors from authorInfo
            if (book.periods.isEmpty()) {
                buildCategoryPaths(category, "std/period", "其他");
            } else {
                book.periods.forEach(v -> buildCategoryPaths(category, "std/period", v));
            }
            if (book.authors.isEmpty()) {
                buildCategoryPaths(category, "ext/author", "其他");
            } else {
                book.authors.forEach(v -> buildCategoryPaths(category, "ext/author", v));
            }
        }
    }

    static void prepareBookChapters(final Book book) {
        if (null == book || null == book.path) return;
        if (book.hasAttr("tocChapters") || book.hasAttr("volChapters")) return;
        //
        final ChapterTree chapterTree = new ChapterTree(AppContext.bookcase(), book);
        final Node<Chapter> tocChapters = chapterTree.getTocChapters();
        book.attr("tocChapters", tocChapters);
        final Node<Chapter> volChapters = chapterTree.getVolChapters();
        book.attr("volChapters", volChapters);
    }

    static void prepareBookContents(final Book book, final boolean contentInVols) {
        if (null == book || null == book.path) return;
        //
        final BookDocument bookDoc = book.path.startsWith("a/") ? book.removeAttr(BookDocument.class) : new BookDocument(AppContext.bookcase(), book);
        final Node<Chapter> tocChapters = book.attr("tocChapters");
        final Node<Chapter> volChapters = book.attr("volChapters");
        //
        if (!book.path.startsWith("toc/")) {
            tocChapters.traverse((tocLvl, tocNode, tocChapter) -> {
                if (null == tocChapter.path || tocNode.hasChildren() || "title".equals(tocChapter.type))
                    return;
                //
                VolumeDocument volDocument = bookDoc.getVolumeDocument(tocChapter.path);
                // change vol chapters
                tocChapter.addParagraph(null, volDocument.getStandardHtml().text());
            });
            return;
        }

        //TODO correct/compact chapter with just few text
        //
        tocChapters.relinkChildren();
        // for speed up
        final Map<String, List<Node<Chapter>>> tocChaptersGroupByVol = new HashMap<>();
        tocChapters.traverse((tocLvl, tocNode, tocChapter) -> {
            if (null != tocChapter.path) {
                // keep in mem to boost lookup
                book.attr(tocChapter.id, tocChapter);
                tocChaptersGroupByVol.computeIfAbsent(tocChapter.path, k -> new ArrayList<>()).add(tocNode);
            }
        });
        //
        volChapters.traverse((volLvl, volNode, volChapter) -> {
            if (null == volChapter.path || volNode.hasChildren() || "title".equals(volChapter.type))
                return;
            // keep in mem to boost lookup
            book.attr(volChapter.id, volChapter);
            //
            VolumeDocument volDocument = bookDoc.getVolumeDocument(volChapter.path);
            if (!volDocument.exists()) {
                volChapter.type = "title";
                volChapter.title = StringHelper.concat(volChapter.title, "（暂无）");
                return;
            }
            //
            boolean contentsInVols;
            if (volChapter.hasAttr("contentInVols"))
                contentsInVols = volChapter.attr("contentInVols");
            else if (book.hasAttr("contentInVols"))
                contentsInVols = book.attr("contentInVols");
            else contentsInVols = contentInVols;
            //
            List<Node<Chapter>> tocNodesGroupedByVol = tocChaptersGroupByVol.computeIfAbsent(volChapter.path, k -> new ArrayList<>());
            //
            if (!contentsInVols && !tocNodesGroupedByVol.isEmpty()) {
                boolean success = initVolChapterInTocChapters(book, volChapter, volDocument, tocNodesGroupedByVol);
                // if not success, then process in vol
                contentsInVols = !success;
            }

            if (contentsInVols || tocNodesGroupedByVol.isEmpty()) {
                // change toc chapters
                for (Node<Chapter> tocNode : tocNodesGroupedByVol) {
                    if (!tocNode.hasChildren()) {
                        Chapter tocChapter = tocNode.value;
                        tocChapter.type = "link";
                        tocChapter.attr("linkTarget", null == tocChapter.anchor ? volChapter.id : (volChapter.id + "#" + tocChapter.anchor));
                        tocChapter.attr("linkTargetType", "article");
                        if (volChapter.hasParagraphs())
                            volChapter.paragraphs().clear();
                    }
                }
                // change vol chapters
                volChapter.addParagraph(null, volDocument.getStandardHtml().text());
            }
        });
        //
        volChapters.relinkChildren((level, node, volChapter) -> {
            if (null == volChapter.path || node.hasChildren() || "title".equals(volChapter.type) || "link".equals(volChapter.type)) {
                return false;
            }
            //
            if (!volChapter.title.startsWith(book.title))
                volChapter.title = StringHelper.concat(book.title, " ", volChapter.title);
            if (!volChapter.hasParagraphs()) {
                VolumeDocument volDocument = bookDoc.getVolumeDocument(volChapter.path);
                volChapter.addParagraph(null, volDocument.getStandardHtml().text());
                return true;
            }
            return true;
        });
    }

    private static boolean initVolChapterInTocChapters(Book book, Chapter volChapter,
                                                       VolumeDocument volDocument, List<Node<Chapter>> tocNodesGroupedByVol) {
        final Element volDocBody = volDocument.getDocumentBody();
        final Elements volDocTocList = volDocBody.select("cb|mulu");
        int volDocTocIdx = 0;

        // 1, loop for detect selector for each chapter
        for (Node<Chapter> tocNode : tocNodesGroupedByVol) {
            Chapter tocChapter = tocNode.value;
            if ("title".equals(tocChapter.type) || "link".equals(tocChapter.type))
                continue;
            //
            Element volDocTocEle = null, tmp;
            for (int i = volDocTocIdx; i < volDocTocList.size(); ) {
                tmp = volDocTocList.get(i++);
                if (tocChapter.title.equals(tmp.text())) {
                    volDocTocEle = tmp;
                    volDocTocIdx = i;
                    break;
                }
            }
            String startCss = null != volDocTocEle ? volDocTocEle.cssSelector() : null;
            String startId = null;
            if (null != tocChapter.anchor) {
                startId = (String) tocChapter.anchor;
                startId = StringHelper.concat("[n=", startId.charAt(0) == 'p' ? startId.substring(1) : startId, "]");
            }
            tocChapter.attr("startCss", startCss);
            tocChapter.attr("startId", startId);
        }

        final List<ChapterFilterInfo> filterInfos = new ArrayList<>();
        // 2, loop for calculate filters
        for (Node<Chapter> tocNode : tocNodesGroupedByVol) {
            Chapter tocChapter = tocNode.value;
            if (tocNode.hasChildren() || "title".equals(tocChapter.type) || "link".equals(tocChapter.type))
                continue;
            //
            String currStartCss = tocChapter.attrStr("startCss");
            String currStartId = tocChapter.attrStr("startId");
            Node<Chapter> nextNode = tocNode.getLinkedNext();
            boolean currSameNext = null != nextNode && tocChapter.path.equals(nextNode.value.path);
            String nextStartCss = currSameNext ? nextNode.value.attrStr("startCss") : null;
            String nextStartId = currSameNext ? nextNode.value.attrStr("startId") : null;
            // first time visit a volume xml
            if (filterInfos.isEmpty()) {
                // get all things from begin of body as paragraph 1
                filterInfos.add(new ChapterFilterInfo(tocChapter,
                        null,
                        null == currStartCss && null == currStartId ? null : (null != currStartCss ? ele -> ele.is(currStartCss) : ele -> ele.is(currStartId))
                ));
            }
            //
            filterInfos.add(new ChapterFilterInfo(tocChapter,
                    null == currStartCss && null == currStartId ? null : (null != currStartCss ? ele -> ele.is(currStartCss) : ele -> ele.is(currStartId)),
                    null == nextStartCss && null == nextStartId ? null : (null != nextStartCss ? ele -> ele.is(nextStartCss) : ele -> ele.is(nextStartId))
            ));
        }
        // no filters, cannot fill data for every toc-chapter
        if (filterInfos.isEmpty())
            return false;
        //  3, parse by filters and fill data in every toc-chapter
        LinkedXmlFilter rootFilter = null, currFilter, prevFilter = null;
        for (ChapterFilterInfo filter : filterInfos) {
            currFilter = new LinkedXmlFilter(filter.startFilter, filter.stopFilter, (contentEle) -> {
                String contentTxt = contentEle.text();
                if (StringHelper.isNotBlank(contentTxt)) {
                    filter.chapter.addParagraph(null, contentTxt);
                }
            });
            if (null == rootFilter)
                rootFilter = currFilter;
            // make link
            if (null != prevFilter)
                prevFilter.next = currFilter;
            // keep for next
            prevFilter = currFilter;
        }
        volDocument.filterStandardHtml(rootFilter);
        // 4, check again to validation all toc-chapters is data filled
        final Set<Chapter> filteredChapters = new LinkedHashSet<>();
        filterInfos.forEach(filter -> filteredChapters.add(filter.chapter));
        Chapter lastFilteredChapterWithData = null;
        for (Chapter filteredChapter : filteredChapters) {
            if (!filteredChapter.hasParagraphs()) {
                if (null == lastFilteredChapterWithData)
                    throw new RuntimeException("chapter data is empty, and no at least one avail");
                // link current toc-chapter to last one with data
                filteredChapter.type = "link";
                filteredChapter.attr("linkTarget", lastFilteredChapterWithData.id + "#" + filteredChapter.anchor);
                filteredChapter.attr("linkTargetType", lastFilteredChapterWithData.type);
            } else {
                // link this toc-chapter with in vol-chapter
                volChapter.addParagraph("@", filteredChapter.id);
                // keep for next
                lastFilteredChapterWithData = filteredChapter;
            }
        }
        // all toc-chapters processed, this vol-chapter no need to search
        volChapter.attr("prop.search.exclude", "true");
        volChapter.attr("data", "para");
        return true;
    }

    private static void buildCategoryPaths(Map<String, String> category, String group, String catalog) {
        if (StringHelper.isBlank(catalog))
            return;
        final String[] catalogTitles = catalog.split("/");
        final String[] catalogNames = new String[catalogTitles.length];
        for (int i = 0; i < catalogTitles.length; i++) {
            catalogNames[i] = catalogTitles[i];
        }
        final List<String> catalogPaths = StringHelper.getFlatPaths(StringHelper.join("/", catalogNames));
        for (int i = 0; i < catalogPaths.size(); i++) {
            category.put(group.concat("/").concat(catalogPaths.get(i)), catalogTitles[i]);
        }
    }

    private record ChapterFilterInfo(Chapter chapter, Predicate<Element> startFilter,
                                     Predicate<Element> stopFilter) {
    }

    private IndexingHelper() {
    }

    static List<Piece> buildBookToPieces(Book book) {
        final List<Piece> pieces = new ArrayList<>();
        //
        final Piece piece = Piece.of();
        pieces.add(piece);
//        piece.project = book.attrStr("project");
//        piece.version = book.attrStr("version");
        piece.id = book.id;
        piece.field("file_s", book.path);
        piece.type = "book";
        piece.title = book.title;
        //
        piece.field("book_s", book.id);
        if (StringHelper.isNotBlank(book.authorInfo())) {
            piece.field("author_txt_aio", book.authorInfo);
        }

        piece.field("title_txt_aio", book.title);

        //
        if (StringHelper.isNotBlank(book.location))
            piece.field("location_s", book.location);
        // chapters
        buildBookChapters(pieces, book, book.chapters);
        //
        if (book.hasAttr("priority"))
            piece.priority = book.attr("priority");
        if (book.hasAttr("sequence"))
            piece.field("sequence_s", book.attrStr("sequence"));
        //
        // add category
        buildBookCategories(piece, book.path, book);

        return pieces;
    }

    private static void buildBookChapters(List<Piece> pieces, Book book, Node<? extends Chapter> chapterNode) {
        if (null != chapterNode.parent()) {
            if ("title".equals(chapterNode.value.type)) {
                // do nothing
            } else if ("link".equals(chapterNode.value.type)) {
                // do nothing
            } else if (!chapterNode.hasChildren()) {
                final Chapter chapter = chapterNode.value;
                final boolean dataReplacedByParas = chapter.hasAttr("data", "para");
                final Piece piece = Piece.of();
                pieces.add(piece);
//                        piece.project = book.attrStr("project");
//                    piece.version = book.attrStr("version");
                piece.id = chapter.id;
                piece.field("file_s", chapter.path);
                piece.type = dataReplacedByParas ? "volume" : "article";
                piece.title = chapter.title;
                //
                piece.field("book_s", book.id);
                if (StringHelper.isNotBlank(chapter.anchor))
                    piece.field("anchor_s", chapter.anchor);

                if (StringHelper.isNotBlank(book.authorInfo)) {
                    piece.field("author_txt_aio", book.authorInfo);
                }

                piece.field("title_txt_aio", chapter.title);

                if (chapter.hasAttr("source"))
                    piece.field("source_s", chapter.attrStr("source"));
                else piece.field("source_s", book.title);

                if (chapter.hasAttr("location"))
                    piece.field("location_s", chapter.attrStr("location"));
                else if (StringHelper.isNotBlank(book.location))
                    piece.field("location_s", book.location);
                else piece.field("location_s", book.title);

                //
                if (!chapter.hasParagraphs()) {
                    //
                } else if (dataReplacedByParas) {
                    //
                } else if (chapter.hasParagraphs()) {
                    final StringBuilder buf = new StringBuilder();
                    chapter.paragraphs().forEach(paragraph -> {
                        if (StringHelper.isNotBlank(paragraph.caption) && !paragraph.caption.equals(chapter.title))
                            buf.append(paragraph.caption).append("。");
                        if (StringHelper.isNotBlank(paragraph.content))
                            buf.append(paragraph.content);
                    });
                    piece.text("text_txt_aio", buf.toString());
                }
                //
                if (chapter.hasAttr("priority"))
                    piece.priority = chapter.attr("priority");
                if (chapter.hasAttr("sequence"))
                    piece.field("sequence_s", chapter.attrStr("sequence"));

                // add category
                buildBookCategories(piece, chapter.path, chapter, book);
            }
        }
        for (Node<? extends Chapter> child : chapterNode.children())
            buildBookChapters(pieces, book, child);
    }

    private static void buildBookCategories(Piece piece, String path, Attributes... attrs) {
        Map<String, String> category = null;
        for (Attributes attr : attrs) {
            category = attr.attr("category");
            if (null != category) break;
        }
        if (null != category) {
            category.forEach((k, v) -> {
                if (k.startsWith("project/")) {
                    piece.projects.add(k.substring(8));
                } else {
                    piece.categories.add(k);
                }
            });
        }
    }
}
