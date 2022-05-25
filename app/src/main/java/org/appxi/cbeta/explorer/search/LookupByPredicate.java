package org.appxi.cbeta.explorer.search;

import org.appxi.util.StringHelper;
import org.appxi.util.ext.FiFunctionX3;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

class LookupByPredicate {
    record LookupKeyword(String text, boolean isFullAscii) {
    }

    public static void lookup(String lookupText, int resultLimit,
                              List<LookupLayerEx.LookupResultItem> result,
                              Set<String> lookupKeywords) {
        final List<LookupKeyword> keywords =
                Stream.of(StringHelper.split(lookupText, " ", "[()（）“”\"]"))
                        .map(str -> str.replaceAll("[()（）“”\"]", "")
                                .replaceAll("\s+", " ").toLowerCase().strip())
                        .filter(str -> !str.isEmpty())
                        .map(str -> new LookupKeyword(str, str.matches("[a-zA-Z0-9\s]+")))
                        .toList();
        //
        final boolean isInputEmpty = keywords.isEmpty();
        final boolean isInputFullAscii = !isInputEmpty && keywords.get(0).isFullAscii();
        final boolean isInputWithAscii = !isInputEmpty && lookupText.matches(".*[a-zA-Z0-9]+.*");

        // 此处使用一些优化机制，比如如果输入中不包含中文时，再做匹配也是浪费资源和占用时间
        final FiFunctionX3<List<LookupKeyword>, Double, String, Double> predicate;
        if (isInputFullAscii) predicate = predicateByAscii; // 输入为纯ASCII字符时，仅按拼音匹配
        else if (isInputWithAscii) predicate = predicateByAsciiAndString; // 输入中混合了中英文时，按中文和拼音混合匹配（性能会差一些）
        else predicate = predicateByString; // 其他情况，也是一般情况，仅按纯字符匹配，不匹配拼音

        // 此处的检索机制为按不同规则打分并排序显示，因此此处必须要检索全部数据（除未输入时），
        // 可能会有一些性能问题，但相比使用任何带数据结构的内置外置数据系统，此是最方便的方案。可以后续不断优化
        final int size = LookupDatabase.cachedDatabase.size();
        // 此处不能使用forEach！
        for (int i = 0; i < size; i++) {
            LookupDatabase.LookupData data = LookupDatabase.cachedDatabase.get(i);
            double score = 0;
            // 默认未输入时，无法按条件匹配，仅返回一定数量的纯数据即可
            if (isInputEmpty) score += 1;
            else {
                if (data.stdBook && null == data.chapterTitle) {
                    if (isInputWithAscii) // 如果输入字符中没有数字或字母，则匹配bookId无实际意义，只是浪费资源（因为bookId全是非中文字符）
                        score += predicate.apply(keywords, 200.0, data.bookId);
                    score += predicate.apply(keywords, 200.0, data.bookTitle);
                    score += predicate.apply(keywords, 5.0, data.authorInfo);
                    score += predicate.apply(keywords, 5.0, data.bookVolsLabel);
                } else if (data.stdBook) {
                    // 对于章节再匹配书名或作译者信息，就会返回太多基本一样的数据，亦无意义
                    score += predicate.apply(keywords, 50.0, data.chapterTitle);
                } else {
                    // 对于非标准的内容，如一些htm文件，至少有一个标题可以匹配
                    score += predicate.apply(keywords, 5.0, data.bookTitle);
                }
            }
            if (score > 0) {
                result.add(new LookupLayerEx.LookupResultItem(data, score));
                if (isInputEmpty && result.size() > resultLimit) break;
            }
        }
        //
        keywords.forEach(k -> lookupKeywords.add(k.text));
    }

    private static final FiFunctionX3<List<LookupKeyword>, Double, String, Double> predicateByAsciiAndString =
            (keywords, weightFactor, data) -> {
                if (data == null) return .0;

                String dataInAscii = null;
                double score = 0;
                for (LookupKeyword keyword : keywords) {
                    // 如果输入为全英文（可能为双引号包含的空格分隔的句子），此时尝试按拼音匹配，否则总是匹配则浪费资源
                    if (keyword.isFullAscii()) {
                        // 各种中文的拼音均在初始化时已生成，此时生成会有性能问题。如果获取不到则直接放弃匹配！
                        if (null == dataInAscii) dataInAscii = LookupDatabase.cachedAsciiMap.get(data);
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

    private static final FiFunctionX3<List<LookupKeyword>, Double, String, Double> predicateByString =
            (keywords, weightFactor, data) -> {
                if (data == null) return .0;

                double score = 0;
                for (LookupKeyword keyword : keywords) {
                    if (data.equals(keyword.text())) score += 1.;
                    else {
                        if (data.startsWith(keyword.text())) score += .6;
                        else if (data.endsWith(keyword.text())) score += .2;
                        else if (data.contains(keyword.text())) score += .4;
                    }
                }
                return score * weightFactor;
            };

    private static final FiFunctionX3<List<LookupKeyword>, Double, String, Double> predicateByAscii =
            (keywords, weightFactor, data) -> {
                if (data == null) return .0;

                final String dataInPinyin = LookupDatabase.cachedAsciiMap.get(data);
                if (null == dataInPinyin) return .0;

                double score = 0;
                for (LookupKeyword keyword : keywords) {
                    // 对于拼音，如果再匹配contains或endsWith，就会返回太多数据，无实际意义
                    if (dataInPinyin.equals(keyword.text())) score += 1.;
                    else if (dataInPinyin.startsWith(keyword.text())) score += .6;
                }
                return score * weightFactor;
            };
}
