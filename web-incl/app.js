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

function getScrollTop1Selector(ele = null) {
    ele = ele || getScrollTop1Element();
    if (!ele) return null;
    if (ele.is("[id]"))
        return ele.tagName() + '#' + ele.attr('id');
    else if (ele.is("span.note"))
        return "span." + ele.attr("class").replaceAll("  ", " ").replaceAll(" ", ".") + "[data-n='" + (ele.attr('data-n')) + "']";
    else if (ele.is("[data-n]"))
        return ele.tagName() + "." + ele.attr("class").replaceAll("  ", " ").replaceAll(" ", ".") + "[data-n='" + (ele.attr('data-n')) + "']";
    else if (ele.is("span.lb"))
        return "span.lb[data-n='" + (ele.attr('data-n')) + "']:first";
    return ele.cssSelector();
}

function getScrollTop1AnchorInfo(outMapOrElseStr = true) {
    const $ele = getScrollTop1Element();
    if (!$ele) return;
    const selector = getScrollTop1Selector($ele);
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
        "text": text
    };
    return outMapOrElseStr ? map : JSON.stringify(map);
}

function setScrollTop1BySelectors(selector, percent = 0) {
    let scrollTop = 0;
    const target = selector && $(selector);
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

function getValidSelectionInfo() {
    const validSelection = getValidSelection();
    if (!validSelection) return null;
}

function getValidSelectionText() {
    const validSelection = getValidSelection();
    if (!validSelection) return null;
    const selected = validSelection.toString().trim();
    const selectedIsEmpty = selected.length <= 0;
}

/* ************************************************************************************************************************************* */

function handleOnWrapLines() {
    const markPos = getScrollTop1Selector();
    const article = $('body > article');
    article.toggleClass('wrap-lines-on');
    setScrollTop1BySelectors(markPos);
}

function handleOnFirstLetterIndent() {
    const article = $('body > article');
    const markOn = 'first-letter-indent-on';
    // first time need to detect
    if (!article.prop(markOn)) {
        article.find('p').each(function () {
            const para = $(this);
            if (para.text().trimLeft().match(/^[““『]/)) {
                para.addClass('first-letter-indent');
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
                    let tmp = $(nodes[0]);
                    if (nodes[0].nodeType === 3) {
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
                        if (appMod) tmp.attr("data-t", appMod.attr("data-t").replaceAll("】，", "】<hr>"));
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

function handleOnSearchInPage() {
    if ($("#finder").is("[active]")) return;
    $("body [data-finder-activator]:first").click();
}
