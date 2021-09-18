jQuery.fn.cssSelectorEx = function () {
    if (this.is("[id]"))
        return this.tagName() + '#' + this.attr('id');
    else if (this.is("span.note"))
        return "span." + this.attr("class").replaceAll("  ", " ").replaceAll(" ", ".") + "[data-n='" + (this.attr('data-n')) + "']";
    else if (this.is("[data-n]"))
        return this.tagName() + "." + this.attr("class").replaceAll("  ", " ").replaceAll(" ", ".") + "[data-n='" + (this.attr('data-n')) + "']";
    else if (this.is("span.lb"))
        return "span.lb[data-n='" + (this.attr('data-n')) + "']:first";
    return this.cssSelector();
};

function getScrollTopNElements(num = 10) {
    const list = $("body > article *:in-viewport");
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
    const result = [];
    for (let i = 0; i < list.length; i++) {
        const item = $(list[i]);
        if (item.offset().top >= scrollTop) {
            result.push(item);
            if (result.length >= num)
                break;
        }
    }
    return result;
}

function getScrollTop1Element() {
    const list = getScrollTopNElements(1);
    return list.length > 0 ? list[0] : null;
}

function getScrollTop1Selector($ele = null) {
    $ele = $ele || getScrollTop1Element();
    return $ele && $ele.cssSelectorEx();
}

function getScrollTop1AnchorInfo(outMapOrElseStr = true) {
    const $ele = getScrollTop1Element();
    if (!$ele) return;
    const selector = $ele.cssSelectorEx();
    if (!selector) return; // cannot restore
    const ele = $ele[0];
    let text = "";
    let handled = false;
    $('body > article').traverse(function (node) {
        if (!handled)
            handled = node === ele;
        if (handled && node.nodeType === 3)
            text = text + $(node).text();
        return text.length > 64;
    });
    if (!handled || text.length === 0) return;
    const map = {
        "anchor": selector,
        "text": text,
        "rangyPos": rangy.serializePosition(ele, 0, document)
    };
    return outMapOrElseStr ? map : JSON.stringify(map);
}

function setScrollTop1BySelectors(selector, percent = 0) {
    let scrollTop = 0;
    let target = selector && $(selector);
    if (selector && !target) target = $('#' + selector);
    if (target && target.length > 0)
        scrollTop = target.offset().top;
    else scrollTop = percent * document.body.scrollHeight;
    scrollTop = scrollTop - 3;
    scrollTop = scrollTop < 0 ? 0 : scrollTop;
    $("html, body").animate({scrollTop: scrollTop}, 150);
}


var dataApi;
let resizeBodyTimer;
let markedScrollTop1Selector;
let documentLoaded = false;

function beforeOnResizeBody() {
    if (!documentLoaded) return;
    markedScrollTop1Selector = markedScrollTop1Selector || getScrollTop1Selector();
}

function handleOnResizeBody() {
    if (!documentLoaded) return;
    if (resizeBodyTimer) clearTimeout(resizeBodyTimer);
    resizeBodyTimer = setTimeout(function () {
        resizeBodyTimer = null;
        if (!markedScrollTop1Selector) return;
        setScrollTop1BySelectors(markedScrollTop1Selector, 0);
        markedScrollTop1Selector = null;
    }, 200);
}

$(document).ready(function () {
    documentLoaded = true;
    document.body.onresize = handleOnResizeBody;
    handleOnPrettyIndent();
    if (rangy) rangy.init();
});

function getValidSelection() {
    const selection = window.getSelection();
    if (!selection.anchorNode || !selection.anchorNode.parentElement)
        return null;
    const parentEle = $(selection.anchorNode.parentElement);
    if (parentEle.is('article') || parentEle.parents('article'))
        return selection;
    return null;
}

function getValidSelectionText() {
    const validSelection = getValidSelection();
    if (!validSelection) return null;
    const selected = validSelection.toString().trim();
    return selected.length < 1 ? null : selected;
}

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

function handleOnWrapLines() {
    const markPos = getScrollTop1Selector();
    const article = $('body > article');
    article.toggleClass('wrap-lines-on');
    setScrollTop1BySelectors(markPos);
}

function handleOnWrapPages() {
    const markPos = getScrollTop1Selector();
    const article = $('body > article');
    article.toggleClass('wrap-pages-on');
    setScrollTop1BySelectors(markPos);
}

function handleOnPrettyIndent() {
    const article = $('body > article');
    const markOn = 'pretty-indent-on';
    // first time need to detect
    if (!article.prop(markOn)) {
        article.find('p, lg').each(function () {
            const $ele = $(this);
            const txt = $ele.text().trimLeft();
            if (txt.match(/^((“「)|(“『)|(「『)|(『「))/)) {
                if ($ele.hasClass("lg")) {
                    $ele.addClass('indent-first2-letter-lg');
                    if (txt.trimRight().match(/((”」)|(”』)|(」』)|(』」))$/))
                        $ele.addClass('indent-last2-letter-lg');
                    else if (txt.trimRight().match(/[”」』]$/))
                        $ele.addClass('indent-last-letter-lg');
                } else $ele.addClass('indent-first2-letter');
            } else if (txt.match(/^[“「『]/)) {
                if ($ele.hasClass("lg")) {
                    $ele.addClass('indent-first-letter-lg');
                    if (txt.trimRight().match(/[”」』]$/))
                        $ele.addClass('indent-last-letter-lg');
                } else $ele.addClass('indent-first-letter');
            }
        });
        article.prop(markOn, true);
    }
    article.toggleClass(markOn);
}

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
                placement: 'top',
                content: (tmp) => tmp.getAttribute('data-t')
            });
        } else if (type === 3) {
            tippyInstances = tippy('.' + markOn + ' span.app > .lem > .tmp', {
                allowHTML: true,
                animation: false,
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

/* ************************************************************************************************************************************* */

function getBookmarkAnchorInfo() {
    const map = getScrollTop1AnchorInfo();
    if (!map) return null;
    return JSON.stringify(map);
}

function getFavoriteAnchorInfo() {
    let map = getValidSelectionAnchorInfo();
    if (!map) map = getScrollTop1AnchorInfo();
    if (!map) return null;
    return JSON.stringify(map);
}

