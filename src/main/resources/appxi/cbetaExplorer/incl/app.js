function scrollTop1Element() {
    // return $('span.lb:in-viewport:first');
    var list = $("*:in-viewport");
    var scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
    for (var i = 0; i < list.length; i++) {
        var item = $(list[i]);
        if (item.offset().top >= scrollTop)
            return item;
    }
    return null;
}

function scrollTop1Selector(ele = null) {
    ele = ele || scrollTop1Element();
    if (!ele) return null;
    if (ele.is("[id]"))
        return ele.tagName() + '#' + ele.attr('id');
    else if (ele.is("span.lb"))
        return "span.lb[data-n='" + (ele.attr('data-n')) + "']:first";
    return ele.cssSelector();
}

function scrollTop1BySelectors(selector, percent = 0) {
    var scrollTop = 0;
    var target = selector && $(selector);
    if (target && target.length > 0)
        scrollTop = target.offset().top;
    else scrollTop = percent * document.body.scrollHeight;
    scrollTop = scrollTop - 3;
    scrollTop = scrollTop < 0 ? 0 : scrollTop;
    $("html, body").animate({scrollTop: scrollTop}, 150);
}


var javaConnector;
var resizeBodyTimer;
var markedScrollTop1Selector;
var documentLoaded = false;

function beforeOnResizeBody() {
    if (!documentLoaded) return;
    markedScrollTop1Selector = markedScrollTop1Selector || scrollTop1Selector();
}

function handleOnResizeBody() {
    if (!documentLoaded) return;
    if (resizeBodyTimer) clearTimeout(resizeBodyTimer);
    resizeBodyTimer = setTimeout(function () {
        resizeBodyTimer = null;
        if (!markedScrollTop1Selector) return;
        scrollTop1BySelectors(markedScrollTop1Selector, 0);
        markedScrollTop1Selector = null;
    }, 200);
}

$(document).ready(function () {
    documentLoaded = true;
    document.body.onresize = handleOnResizeBody;
});

function handleOnOpenFinder() {
    if (!documentLoaded) return;
    let finder = $("#finder:first");
    if (finder.is("[active]")) return;
    $("[data-finder-activator]:first").click();
}