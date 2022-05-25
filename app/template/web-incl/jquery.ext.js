jQuery.fn.tagName = function () {
    return this.prop("tagName").toLowerCase();
};
jQuery.fn.cssSelector = function () {
    if (this.is("[id]"))
        return this.tagName() + '#' + this.attr('id');
    const paths = [];
    this.each(function (index, element) {
        let path, $node = jQuery(element);
        while ($node.length) {
            let realNode = $node.get(0), name = realNode.localName;
            if (!name) {
                break;
            }
            name = name.toLowerCase();
            if ($node.is("[id]")) {
                path = name + '#' + $node.attr('id') + (path ? ' > ' + path : '');
                break;
            }
            const parent = $node.parent();
            const sameTagSiblings = parent.children(name);

            if (sameTagSiblings.length > 1) {
                const allSiblings = parent.children();
                const index = allSiblings.index(realNode) + 1;
                if (index > 0) {
                    name += ':nth-child(' + index + ')';
                }
            }
            path = name + (path ? ' > ' + path : '');
            $node = parent;
        }
        paths.push(path);
    });
    return paths.join(',');
};
jQuery.fn.traverse = function (predicate) {
    if (predicate(this[0]))
        return true;
    const list = this.contents();
    for (let i = 0; i < list.length; i++) {
        const node = list[i];
        if (node.nodeType === 3) {
            if (predicate(node))
                return true;
        } else if ($(node).traverse(predicate)) {
            return true;
        }
    }
    return false;
};

jQuery.fn.centerText = function(len = 15) {
    return this.prevText(len) + this.text() + this.nextText(len);
};
jQuery.fn.prevText = function(len = 15) {
	let result = '';
	let $node = this;
	do {
        let $contents = $node.parent().contents();
        for (let i = $contents.index($node) - 1; i > -1; i--) {
			result = $contents.eq(i).text() + result;
			if (result.length >= len) return result.substring(result.length - len, result.length);
        }
		$node = $node.parent();
		if ($node.is('body')) break;
	}
	while($node.length > 0);
	return result;
};
jQuery.fn.nextText = function(len = 15) {
	let result = '';
	let $node = this;
	do {
        let $contents = $node.parent().contents();
        for (let i = $contents.index($node) + 1; i < $contents.length; i++) {
			result = result + $contents.eq(i).text();
			if (result.length >= len) return result.substring(0, len);
        }
        $node = $node.parent();
		if ($node.is('body')) break;
	}
	while($node.length > 0);
	return result;
};
