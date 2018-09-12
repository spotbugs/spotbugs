// MIT © 2017 azu
"use strict";
export const checkBoldTextPrecedingColon = ({ node, Syntax, RuleError, getSource, fixer, report }) => {
    const children = node.children;
    if (!children) {
        return;
    }
    const BoldNodeList = children.filter(childNode => {
        return childNode.type === Syntax.Strong;
    });

    BoldNodeList.forEach(boldNode => {
        const currentIndex = children.indexOf(boldNode);
        const nextNodeOfBold = children[currentIndex + 1];
        if (!nextNodeOfBold) {
            return;
        }
        if (nextNodeOfBold.type !== Syntax.Str) {
            return;
        }
        const nextNodeValue = getSource(nextNodeOfBold);
        if (!nextNodeValue) {
            return;
        }
        const nextCharacter = nextNodeValue.charAt(0);
        if (nextCharacter !== ":") {
            return;
        }
        // add `:` to current node
        const message = `When the text preceding a colon is bold, make the colon bold too.
https://developers.google.com/style/colons#bold-text-preceding-colon
`;
        if (!Array.isArray(boldNode.children)) {
            return;
        }
        const strNodeOfBoldNode = boldNode.children[0];
        if (!strNodeOfBoldNode || strNodeOfBoldNode.type !== Syntax.Str) {
            return;
        }
        report(
            strNodeOfBoldNode,
            new RuleError(message, {
                index: strNodeOfBoldNode.range[0] - node.range[0],
                fix: fixer.replaceText(strNodeOfBoldNode, `${getSource(strNodeOfBoldNode)}:`)
            })
        );
        // remove `:` from next node
        report(
            nextNodeOfBold,
            new RuleError(message, {
                index: 0,
                fix: fixer.removeRange([0, 1])
            })
        );
    });
};
