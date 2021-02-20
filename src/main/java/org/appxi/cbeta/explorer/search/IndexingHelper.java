package org.appxi.cbeta.explorer.search;

import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.dao.PiecesRepository;
import org.appxi.prefs.UserPrefs;
import org.appxi.tome.cbeta.*;
import org.appxi.tome.model.Chapter;
import org.appxi.tome.xml.LinkedXmlFilter;
import org.appxi.util.DigestHelper;
import org.appxi.util.StringHelper;
import org.appxi.util.ext.Node;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.function.Predicate;

public abstract class IndexingHelper {
    private static final String VERSION = "21.02.18.7";

    public static final String PROJECT = "cbeta";

    public static void prepareBook(final String dataProject, final String dataVersion,
                                   final Tripitaka tripitaka, final CbetaBook book, final boolean contentInVols) {
        if (null == book || null == book.path)
            return;
        book.attr("project", dataProject);
        book.attr("version", dataVersion);
        book.attr("text.field.suffix", "cjk");
        final Map<String, String> category = new LinkedHashMap<>();
        book.attr("category", category);
        buildCategoryPaths(category, "std/tripitaka", null == tripitaka ? "其他" : tripitaka.name);
        buildCategoryPaths(category, "std/catalog", StringHelper.isBlank(book.catalog) ? "其他" : book.catalog);
        book.authorInfo();// call for init periods/authors from authorInfo
        if (book.periods.isEmpty())
            buildCategoryPaths(category, "std/period", "其他");
        else book.periods.forEach(v -> buildCategoryPaths(category, "std/period", v));
        if (book.authors.isEmpty())
            buildCategoryPaths(category, "ext/author", "其他");
        else book.authors.forEach(v -> buildCategoryPaths(category, "ext/author", v));
        //
        final BookDocument bookDoc = new BookDocument(book);
        final ChapterTree chapterTree = new ChapterTree(book);
        final Node<Chapter> tocChapters = chapterTree.getTocChapters();
        final Node<Chapter> volChapters = chapterTree.getVolChapters();
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
            if (volDocument.notExists()) {
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
                        tocChapter.attr("linkTarget", null == tocChapter.start ? volChapter.id : (volChapter.id + "#" + tocChapter.start));
                        tocChapter.attr("linkTargetType", "article");
                        if (tocChapter.hasParagraphs())
                            tocChapter.paragraphs.clear();
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

    private static boolean initVolChapterInTocChapters(CbetaBook book, Chapter volChapter,
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
            if (null != tocChapter.start) {
                startId = (String) tocChapter.start;
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
                filteredChapter.attr("linkTarget", lastFilteredChapterWithData.id + "#" + filteredChapter.start);
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

    private static record ChapterFilterInfo(Chapter chapter, Predicate<Element> startFilter,
                                            Predicate<Element> stopFilter) {
    }

    private IndexingHelper() {
    }


    public static String currentVersion() {
        return DigestHelper.crc32c(String.valueOf(CbetaHelper.getDataTimeNewest()), VERSION);
    }

    public static String indexedVersion() {
        return UserPrefs.prefs.getString("cbeta.indexed", null);
    }

    public static boolean indexedIsValid() {
        final String indexedVersion = IndexingHelper.indexedVersion();
        final String currentVersion = IndexingHelper.currentVersion();
        return Objects.equals(indexedVersion, currentVersion);
    }


    private static final Object piecesRepository_lock = new Object();
    private static PiecesRepository piecesRepository;

    public static PiecesRepository getPiecesRepository() {
        if (null != piecesRepository)
            return piecesRepository;

        synchronized (piecesRepository_lock) {
            if (null != piecesRepository)
                return piecesRepository;
            while (null == piecesRepository) {
                piecesRepository = AppContext.beans().getBean(PiecesRepository.class);
                if (null == piecesRepository) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
        return piecesRepository;
    }
}
