package org.appxi.cbeta.app.search;

import org.appxi.util.ext.LookupExpression;

import java.util.List;
import java.util.Optional;
import java.util.Set;

class LookupByExpression {
    public static void lookup(String lookupText, int resultLimit,
                              List<LookupLayerEx.LookupResultItem> result,
                              Set<String> usedKeywords) {
        final boolean isInputEmpty = lookupText.isBlank();
//        final boolean isInputFullAscii = !isInputEmpty && lookupText.matches("[a-zA-Z0-9\"\s()]+");
//        final boolean isInputWithAscii = !isInputEmpty && lookupText.matches(".*[a-zA-Z0-9]+.*");

        Optional<LookupExpression> optional = isInputEmpty ? Optional.empty() : LookupExpression.of(lookupText,
                (parent, text) -> new LookupDataFilter(parent, text));
        if (!isInputEmpty && optional.isEmpty()) {
            // not a valid expression
            return;
        }
        final LookupExpression lookupExpression = optional.orElse(null);

        final int size = LookupDatabase.cachedDatabase.size();
        // 此处不能使用forEach！
        for (int i = 0; i < size; i++) {
            LookupDatabase.LookupData data = LookupDatabase.cachedDatabase.get(i);
            double score = 0;
            // 默认未输入时，无法按条件匹配，仅返回一定数量的纯数据即可
            if (isInputEmpty) score += 1;
            else score = lookupExpression.score(data);
            if (score > 0) {
                result.add(new LookupLayerEx.LookupResultItem(data, score));
                if (isInputEmpty && result.size() > resultLimit) break;
            }
        }
        //
        if (null != lookupExpression)
            lookupExpression.keywords().forEach(k -> usedKeywords.add(k.keyword()));
    }

    static class LookupDataFilter extends LookupExpression.Keyword {
        private String asciiKeywordWithSpace;

        public LookupDataFilter(LookupExpression.Grouped parent, String text) {
            super(parent, text);
        }

        @Override
        protected void setKeyword(String text) {
            super.setKeyword(text);
            if (this.isAsciiKeyword()) this.asciiKeywordWithSpace = " ".concat(this.keyword());
        }

        @Override
        public double score(Object obj) {
            if (obj instanceof LookupDatabase.LookupData data) {
                double score = 0;
                if (data.stdBook && null == data.chapterTitle) {
                    // 如果输入字符中没有数字或字母，则匹配bookId无实际意义，只是浪费资源（因为bookId全是非中文字符）
                    if (this.isAsciiKeyword())
                        score += scoreByAsciiAndString(200.0, data.bookId);
                    score += scoreByAsciiAndString(200.0, data.bookTitle);
                    score += scoreByAsciiAndString(5.0, data.authorInfo);
                    score += scoreByAsciiAndString(5.0, data.bookVolsLabel);
                } else if (data.stdBook) {
                    // 对于章节再匹配书名或作译者信息，就会返回太多基本一样的数据，亦无意义
                    score += scoreByAsciiAndString(50.0, data.chapterTitle);
                } else {
                    // 对于非标准的内容，如一些htm文件，至少有一个标题可以匹配
                    score += scoreByAsciiAndString(5.0, data.bookTitle);
                }
                return score;
            }
            return super.score(obj);
        }

        private double scoreByAsciiAndString(double factor, String data) {
            if (data == null) return .0;

            double score = 0;

            final String text = this.keyword();
            // 如果输入为全英文（可能为双引号包含的空格分隔的句子），此时尝试按拼音匹配，否则总是匹配则浪费资源
            if (this.isAsciiKeyword()) {
                // 各种中文的拼音均在初始化时已生成，此时生成会有性能问题。如果获取不到则直接放弃匹配！
                String dataInAscii = LookupDatabase.cachedAsciiMap.get(data);
                if (null != dataInAscii) {
                    if (dataInAscii.equals(text)) score += 1.;
                    else if (dataInAscii.startsWith(text)) score += .6;
                    else if (dataInAscii.contains(this.asciiKeywordWithSpace)) score += .4;
                }
            } else {
                if (data.equals(text)) score += 1.;
                else {
                    if (data.startsWith(text)) score += .6;
                    else if (data.endsWith(text)) score += .2;
                    else if (data.contains(text)) score += .4;
                }
            }
            return score * factor;
        }
    }
}
