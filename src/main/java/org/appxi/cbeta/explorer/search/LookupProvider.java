package org.appxi.cbeta.explorer.search;

import appxi.cbeta.Book;
import appxi.cbeta.BookHelper;
import org.appxi.cbeta.explorer.AppContext;
import org.appxi.cbeta.explorer.DisplayHelper;
import org.appxi.util.ext.FiFunctionX3;

import java.util.*;
import java.util.stream.IntStream;

class LookupProvider implements LookupController.LookupProvider {
    private static final List<LookupController.LookupData> cachedDatabase = new ArrayList<>(10240);
    private static final Map<String, String> cachedAsciiMap = new HashMap<>(10240);

    public void setupInitialize() {
        long st = System.currentTimeMillis();
        new Thread(() -> {
            cachedDatabase.clear();
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            final Collection<Book> books = new ArrayList<>(AppContext.booklistProfile.getManagedBooks());
            books.parallelStream().forEachOrdered(book -> {
                cachedDatabase.add(new LookupController.LookupData(
                        book.path.startsWith("toc/"),
                        book.id, book.volumes.size(), book.title,
                        null, null,
                        book.authorInfo, null));
                if (null != book.id)
                    cachedAsciiMap.put(book.id, book.id.toLowerCase());
                if (null != book.title)
                    cachedAsciiMap.computeIfAbsent(book.title, DisplayHelper::prepareAscii);
                if (null != book.authorInfo)
                    cachedAsciiMap.computeIfAbsent(book.authorInfo, DisplayHelper::prepareAscii);
            });
            books.parallelStream().forEachOrdered(book -> {
                final Collection<LookupController.LookupData> items = new ArrayList<>();
                final boolean stdBook = book.path.startsWith("toc/");
                BookHelper.walkTocChaptersByXmlSAX(AppContext.bookcase(), book, (href, text) -> {
                    items.add(new LookupController.LookupData(
                            stdBook, book.id, book.volumes.size(), book.title,
                            href, text, null, null));
                    if (null != text)
                        cachedAsciiMap.computeIfAbsent(text, DisplayHelper::prepareAscii);
                });
                cachedDatabase.addAll(items);
                items.clear();
            });
            // 统一生成卷数的拼音（目前似乎最大为600卷，但仍生成1-999）
            IntStream.range(1, 999).mapToObj(String::valueOf)
                    .forEach(i -> cachedAsciiMap.put(i.concat("卷"), i.concat("juan")));
            System.out.println("init lookup-in-memory items used time: " + (System.currentTimeMillis() - st));
            System.out.println("init lookup-in-memory items size: " + cachedDatabase.size());
            System.gc();
        }).start();
    }

    @Override
    public List<LookupViewExt.LookupResultItem> lookup(LookupViewExt.LookupRequest lookupRequest) {
        final boolean isInputEmpty = lookupRequest.keywords().isEmpty();
        final boolean isInputFullAscii = !isInputEmpty && lookupRequest.keywords().get(0).isFullAscii();
        final boolean isInputWithAscii = !isInputEmpty && lookupRequest.text().matches(".*[a-zA-Z0-9]+.*");
        final List<LookupViewExt.LookupResultItem> result = new ArrayList<>();

        // 此处使用一些优化机制，比如如果输入中不包含中文时，再做匹配也是浪费资源和占用时间
        final FiFunctionX3<LookupViewExt.LookupRequest, Double, String, Double> predicate;
        if (isInputFullAscii) predicate = predicateByAscii; // 输入为纯ASCII字符时，仅按拼音匹配
        else if (isInputWithAscii) predicate = predicateByAsciiAndString; // 输入中混合了中英文时，按中文和拼音混合匹配（性能会差一些）
        else predicate = predicateByString; // 其他情况，也是一般情况，仅按纯字符匹配，不匹配拼音

        // 此处的检索机制为按不同规则打分并排序显示，因此此处必须要检索全部数据（除未输入时），
        // 可能会有一些性能问题，但相比使用任何带数据结构的内置外置数据系统，此是最方便的方案。可以后续不断优化
        final int size = cachedDatabase.size();
        // 此处不能使用forEach！
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < size; i++) {
            LookupController.LookupData data = cachedDatabase.get(i);
            double score = 0;
            // 默认未输入时，无法按条件匹配，仅返回一定数量的纯数据即可
            if (isInputEmpty) score += 1;
            else {
                if (data.stdBook() && null == data.chapterTitle()) {
                    if (isInputWithAscii) // 如果输入字符中没有数字或字母，则匹配bookId无实际意义，只是浪费资源（因为bookId全是非中文字符）
                        score += predicate.apply(lookupRequest, 200.0, data.bookId());
                    score += predicate.apply(lookupRequest, 200.0, data.bookTitle());
                    score += predicate.apply(lookupRequest, 5.0, data.authorInfo());
                    score += predicate.apply(lookupRequest, 5.0, String.valueOf(data.bookVols()).concat("卷"));
                } else if (data.stdBook()) {
                    // 对于章节再匹配书名或作译者信息，就会返回太多基本一样的数据，亦无意义
                    score += predicate.apply(lookupRequest, 50.0, data.chapterTitle());
                } else {
                    // 对于非标准的内容，如一些htm文件，至少有一个标题可以匹配
                    score += predicate.apply(lookupRequest, 5.0, data.bookTitle());
                }
            }
            if (score > 0) {
                result.add(new LookupViewExt.LookupResultItem(data, score));
                if (isInputEmpty && result.size() > lookupRequest.resultLimit()) break;
            }
        }
        return result;
    }

    private static final FiFunctionX3<LookupViewExt.LookupRequest, Double, String, Double> predicateByAsciiAndString =
            (lookupRequest, weightFactor, data) -> {
                if (data == null) return .0;

                String dataInAscii = null;
                double score = 0;
                for (LookupViewExt.LookupKeyword keyword : lookupRequest.keywords()) {
                    // 如果输入为全英文（可能为双引号包含的空格分隔的句子），此时尝试按拼音匹配，否则总是匹配则浪费资源
                    if (keyword.isFullAscii()) {
                        // 各种中文的拼音均在初始化时已生成，此时生成会有性能问题。如果获取不到则直接放弃匹配！
                        if (null == dataInAscii) dataInAscii = cachedAsciiMap.get(data);
                        if (null != dataInAscii) {
                            if (dataInAscii.equals(keyword.text())) score += 1.;
                            else if (dataInAscii.startsWith(keyword.text())) score += .6;
                        }
                    } else {
                        if (data.equals(keyword.text())) score += 1.;
                        else {
                            if (data.startsWith(keyword.text())) score += .6;
                            else if (data.endsWith(keyword.text())) score += .2;
                            else if (data.contains(keyword.text())) score += .4;
                        }
                    }
                }
                return score * weightFactor;
            };

    private static final FiFunctionX3<LookupViewExt.LookupRequest, Double, String, Double> predicateByString =
            (lookupRequest, weightFactor, data) -> {
                if (data == null) return .0;

                double score = 0;
                for (LookupViewExt.LookupKeyword keyword : lookupRequest.keywords()) {
                    if (data.equals(keyword.text())) score += 1.;
                    else {
                        if (data.startsWith(keyword.text())) score += .6;
                        else if (data.endsWith(keyword.text())) score += .2;
                        else if (data.contains(keyword.text())) score += .4;
                    }
                }
                return score * weightFactor;
            };

    private static final FiFunctionX3<LookupViewExt.LookupRequest, Double, String, Double> predicateByAscii =
            (lookupRequest, weightFactor, data) -> {
                if (data == null) return .0;

                final String dataInPinyin = cachedAsciiMap.get(data);
                if (null == dataInPinyin) return .0;

                double score = 0;
                for (LookupViewExt.LookupKeyword keyword : lookupRequest.keywords()) {
                    // 对于拼音，如果再匹配contains或endsWith，就会返回太多数据，无实际意义
                    if (dataInPinyin.equals(keyword.text())) score += 1.;
                    else if (dataInPinyin.startsWith(keyword.text())) score += .6;
                }
                return score * weightFactor;
            };
}
