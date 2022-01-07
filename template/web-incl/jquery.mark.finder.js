const markFinder = {
    resultsCount: 0,
    currentResult: 0,

    clear: () => {
        $(document.body).unmark();
        markFinder.currentResult = 0;
        markFinder.resultsCount = 0;
        markFinder.updateState();
    },

    find: (term, scrollToVisible = true) => {
        // highlight results
        $(document.body).unmark();
        $(document.body).mark(term);

        // count results
        markFinder.resultsCount = $('mark').length;

        if (markFinder.resultsCount) {
            // there are results, scroll to first one
            markFinder.currentResult = markFinder.preferCurrent() || 1;
            if (scrollToVisible) markFinder.scrollToCurrent();
        } else {
            // no results
            markFinder.currentResult = 0;
        }

        // term not found
        if (!markFinder.resultsCount && term) markFinder.currentResult = -1;

        markFinder.updateState();
    },

    findPrev: () => {
        if (markFinder.resultsCount) {
            if (markFinder.currentResult > 1) markFinder.currentResult--;
            else markFinder.currentResult = markFinder.resultsCount;
            markFinder.scrollToCurrent();
        }
        markFinder.updateState();
    },

    findNext: () => {
        if (markFinder.resultsCount) {
            if (markFinder.currentResult < markFinder.resultsCount) markFinder.currentResult++;
            else markFinder.currentResult = 1;
            markFinder.scrollToCurrent();
        }
        markFinder.updateState();
    },
    
    active: (idx) => {
        if (markFinder.resultsCount) {
            markFinder.currentResult = idx;
            markFinder.scrollToCurrent();
        }
        markFinder.updateState();
    },

    updateState: () => {
        if (javaApp) javaApp.updateFinderState(markFinder.currentResult, markFinder.resultsCount);
    },

    scrollToCurrent: () => {
        let i = markFinder.currentResult - 1;
        $('mark').removeClass('active');
        $(`mark:eq(${i})`).addClass('active');

        $(document.body).scrollTo('mark.active', {
            offset: {
                left: 0,
                top: -100,
            },
        });
    },

    preferCurrent: () => {
        const list = $("body > article mark");
        const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
        for (let i = 0; i < list.length; i++) {
            const item = $(list[i]);
            if (item.offset().top >= scrollTop) return i + 1;
        }
        return 1;
    },

    getHighlights: (textCutLen = 21) => {
        const result = [];
        $("body > article mark").each(function(i) {
            result.push((i+1) + "#" + $(this).centerText(textCutLen));
        });
        return result.join("\n");
    },
}