let resizeBodyTimer;
let markedScrollTop1Selector;
let documentLoaded = false;

function onBodyResizeBefore() {
    if (!documentLoaded) return;
    markedScrollTop1Selector = markedScrollTop1Selector || getScrollTop1Selector();
}

$(document).ready(function () {
    if (urlParams.has('theme')) {
        setWebStyleTheme(urlParams.get('theme'));
    }

    documentLoaded = true;
    document.body.onresize = function () {
        if (!documentLoaded) return;
        if (resizeBodyTimer) clearTimeout(resizeBodyTimer);
        resizeBodyTimer = setTimeout(function () {
            resizeBodyTimer = null;
            if (!markedScrollTop1Selector) return;
            setScrollTop1BySelectors(markedScrollTop1Selector, 0);
            markedScrollTop1Selector = null;
        }, 200);
    };

    try {
        if (rangy) rangy.init();
    } catch (err) {
    }

    $('a[data-note]').each(function () {
        const $this = $(this);
        if ($this.attr('id').startsWith('inline-')) {
            const noteText = decodeURIComponent($this.attr('data-note'));
            $this.removeAttr("data-note");
            $this.html('<blockquote>' + noteText.replace(/\n/g, '<br>') + '</blockquote>');
            if ($this.attr('id').startsWith("inline-alert-"))
                setTimeout(function () { alert(noteText); }, 500);
        }
    });

    tippy('a[data-note]', {
        allowHTML: true,
        animation: false,
        interactive: true,
        interactiveBorder: 10,
        interactiveDebounce: 1000,
        placement: 'top',
        content: (ele) => decodeURIComponent(ele.getAttribute('data-note'))
    });
});

function getValidSelectionAnchorInfo(outMapOrElseStr = true) {
    const validSelection = getValidSelection();
    if (!validSelection) return null;
    const selected = validSelection.toString().trim();
    if (selected.length < 1) return null;
    const startEle = validSelection.baseNode.nodeType === 3
        ? $(validSelection.baseNode.previousElementSibling || validSelection.baseNode.parentElement)
        : $(validSelection.baseNode);
    const endEle = validSelection.anchorNode.nodeType === 3
        ? $(validSelection.anchorNode.nextElementSibling || validSelection.anchorNode.parentElement)
        : $(validSelection.anchorNode);
    let selector = endEle.cssSelectorEx();
    if (!selector) selector = endEle.parent().cssSelectorEx();
    if (!selector) return null;
    const map = {
        "anchor": selector,
        "text": selected,
        "rangy": rangy.serializeSelection()
    };
    return outMapOrElseStr ? map : JSON.stringify(map);
}

function getValidSelectionReferenceInfo(outMapOrElseStr = true) {
    const validSelection = getValidSelection();
    if (!validSelection) return null;
    const selected = validSelection.toString().trim();
    if (selected.length < 1) return null;
    let startLine, endLine;
    if (validSelection.baseNode.nodeType !== 3) {
        let tmp = $(validSelection.baseNode);
        if (tmp.is("span.lb[id]")) startLine = tnp;
    }
    if (!startLine && validSelection.baseNode.nodeType === 3) {
        let tmp = $(validSelection.baseNode.previousElementSibling);
        if (tmp.is("span.lb[id]")) startLine = tmp;
    }
    /*  */
    if (validSelection.baseNode == validSelection.extentNode) endLine = startLine;
    else {
        if (validSelection.extentNode.nodeType !== 3) {
            let tmp = $(validSelection.extentNode);
            if (tmp.is("span.lb[id]")) endLine = tnp;
        }
        if (!endLine && validSelection.extentNode.nodeType === 3) {
            let tmp = $(validSelection.extentNode.previousElementSibling);
            if (tmp.is("span.lb[id]")) endLine = tmp;
        }
    }
    /*  */
    if (!startLine || !endLine) {
        let baseNodeHandled = false, extentNodeHandled = false;
        let previousLine;
        $('body > article').traverse(function (node) {
            if (node.nodeType !== 3 && $(node).is("span.lb[id]")) previousLine = $(node);
            if (!baseNodeHandled) baseNodeHandled = validSelection.baseNode === node;
            if (baseNodeHandled) {
                if (!startLine) startLine = previousLine;
                if (!extentNodeHandled) extentNodeHandled = validSelection.extentNode === node;
                if (extentNodeHandled && !endLine) endLine = previousLine;
            }
            return baseNodeHandled && extentNodeHandled;
        });
    }
    if (!startLine) return null;
    const map = {
        "start": startLine.cssSelectorEx(),
        "end": endLine.cssSelectorEx(),
        "text": selected,
        "rangy": rangy.serializeSelection()
    };
    return outMapOrElseStr ? map : JSON.stringify(map);
}

function getValidSelectionReferenceInfo2() {
    const map = getValidSelectionReferenceInfo(true);
    return map == null ? "||" : map.start + "|" + map.end + "|" + map.text;
}

/* ************************************************************************************************************************************* */

let tippyInstances;

function destroyTippyInstances() {
    if (!tippyInstances) return;
    if (tippyInstances.destroy) {
        tippyInstances.destroy();
        return;
    }
    if (tippyInstances.length)
        for (let i = 0; i < tippyInstances.length; i++) {
            tippyInstances[i].destroy()
        }
}

function handleOnEditMark(type) {
    const article = $('body > article');
    const markOn0 = 'edit-orig-inline-on';
    const markOn1 = 'edit-mod-sharp-on';
    const markOn2 = 'edit-mod-color-on';
    const markOn3 = 'edit-mod-popover-on';
    const markOn4 = 'edit-mod-inline-on';
    // clean
    if (type === -1) {
        destroyTippyInstances();
        article.removeClass([markOn0, markOn1, markOn2, markOn3, markOn4]);
        return;
    }
    //
    let markOn;
    if (type === 0) markOn = markOn0;
    else if (type === 1) markOn = markOn1;
    else if (type === 2) markOn = markOn2;
    else if (type === 3) markOn = markOn3;
    else if (type === 4) markOn = markOn4;
    if (!markOn) return; // unhandled
    if (article.hasClass(markOn)) return; // same as current
    // reset
    destroyTippyInstances();
    article.removeClass([markOn0, markOn1, markOn2, markOn3, markOn4]);
    article.addClass(markOn);

    if (type === 1) {
        tippyInstances = tippy('.' + markOn + ' span.note.mod', {
            allowHTML: true,
            animation: false,
            interactive: true,
            interactiveBorder: 10,
            interactiveDebounce: 1000,
            placement: 'top',
            content: (mod) => mod.getAttribute('data-t')
        });
    }
    if (type === 2 || type === 3) {
        // do some fix is required at first time
        if (!article.prop("edit-mark-lem-fixed")) {
            article.find("span.app > .lem").each(function () {
                const lem = $(this);
                const app = lem.parent();
                const nodes = lem.contents();
                if (nodes && nodes.length > 0) {
                    let idx = 0;
                    let tmp = $(nodes[idx]);
                    if (tmp && tmp.is("span.lb") && nodes.length > 1) {
                        idx = 1;
                        tmp = $(nodes[idx]);
                    }
                    if (nodes[idx].nodeType === 3) {
                        tmp.replaceWith('<span class="tmp">' + tmp.text() + '</span>');
                        tmp = lem.find(" > span.tmp");
                        let tips = [];
                        tips[tips.length] = lem.attr("wit") + "：" + tmp.text();
                        app.find(" > span.rdg").each(function () {
                            const rdg = $(this);
                            tips[tips.length] = rdg.attr("wit").replaceAll('】 【', '】<br>【') + "："
                                + (!rdg.attr("data-t") ? "〔－〕" : rdg.attr("data-t"));
                        });
                        tmp.attr("data-t", tips.join("<hr>"));
                    } else {
                        tmp.addClass("tmp");
                        const appMod = app.prev();
                        if (appMod && appMod.attr("data-t"))
                            tmp.attr("data-t", appMod.attr("data-t").replaceAll("】，", "】<hr>"));
                        else tmp.attr("data-t", "UNKNOWN");
                    }
                }
            });
            article.prop("edit-mark-lem-fixed", true);
        }
        if (type === 2) {
            tippyInstances = tippy('.' + markOn + ' span.app > .lem > .tmp', {
                allowHTML: true,
                animation: false,
                interactive: true,
                interactiveBorder: 10,
                interactiveDebounce: 1000,
                placement: 'top',
                content: (tmp) => tmp.getAttribute('data-t')
            });
        } else if (type === 3) {
            tippyInstances = tippy('.' + markOn + ' span.app > .lem > .tmp', {
                allowHTML: true,
                animation: false,
                interactive: true,
                interactiveBorder: 10,
                interactiveDebounce: 1000,
                placement: 'top',
                content: function (tmp) {
                    const lem = $(tmp).parent();
                    const app = lem.parent();
                    const mod = app.prev('.note.mod');
                    const orig = mod && mod.length > 0 ? mod.prev('.note.orig') : null;
                    const txt = orig && orig.length > 0 ? ('【原】：' + orig.attr('data-t') + '<hr>') : '';
                    return txt + tmp.getAttribute('data-t');
                }
            });
        }
    }
}
