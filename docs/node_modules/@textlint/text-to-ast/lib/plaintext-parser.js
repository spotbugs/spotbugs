// LICENSE : MIT
"use strict";

var Syntax = require("./plaintext-syntax");
var LINEBREAKE_MARK = /\r?\n/g;
function parseLine(lineText, lineNumber, startIndex) {
    // Inline Node have `value`. It it not part of TxtNode.
    // TODO: https://github.com/textlint/textlint/issues/141
    return {
        type: Syntax.Str,
        raw: lineText,
        value: lineText,
        range: [startIndex, startIndex + lineText.length],
        loc: {
            start: {
                line: lineNumber,
                column: 0
            },
            end: {
                line: lineNumber,
                column: lineText.length
            }
        }
    };
}
/**
 * create BreakNode next to StrNode
 * @param {TxtNode} prevNode previous node from BreakNode
 */
function createEndedBRNode(prevNode) {
    return {
        type: Syntax.Break,
        raw: "\n",
        value: "\n",
        range: [prevNode.range[1], prevNode.range[1] + 1],
        loc: {
            start: {
                line: prevNode.loc.end.line,
                column: prevNode.loc.end.column
            },
            end: {
                line: prevNode.loc.end.line,
                column: prevNode.loc.end.column + 1
            }
        }
    };
}
/**
 * create BreakNode next to StrNode
 */
function createBRNode(lineNumber, startIndex) {
    return {
        type: Syntax.Break,
        raw: "\n",
        range: [startIndex, startIndex + 1],
        loc: {
            start: {
                line: lineNumber,
                column: 0
            },
            end: {
                line: lineNumber,
                column: 1
            }
        }
    };
}
/**
 * create paragraph node from TxtNodes
 * @param {[TxtNode]} nodes
 * @returns {TxtNode} Paragraph node
 */
function createParagraph(nodes) {
    var firstNode = nodes[0];
    var lastNode = nodes[nodes.length - 1];
    return {
        type: Syntax.Paragraph,
        raw: nodes.map(function (node) {
            return node.raw;
        }).join(""),
        range: [firstNode.range[0], lastNode.range[1]],
        loc: {
            start: {
                line: firstNode.loc.start.line,
                column: firstNode.loc.start.column
            },
            end: {
                line: lastNode.loc.end.line,
                column: lastNode.loc.end.column
            }
        },
        children: nodes
    };
}

/**
 * parse text and return ast mapped location info.
 * @param {string} text
 * @returns {TxtNode}
 */
function parse(text) {
    var textLineByLine = text.split(LINEBREAKE_MARK);
    // it should be alternately Str and Break
    var startIndex = 0;
    var lastLineIndex = textLineByLine.length - 1;
    var isLasEmptytLine = function isLasEmptytLine(line, index) {
        return index === lastLineIndex && line === "";
    };
    var isEmptyLine = function isEmptyLine(line, index) {
        return index !== lastLineIndex && line === "";
    };
    var children = textLineByLine.reduce(function (result, currentLine, index) {
        var lineNumber = index + 1;
        if (isLasEmptytLine(currentLine, index)) {
            return result;
        }
        // \n
        if (isEmptyLine(currentLine, index)) {
            var emptyBreakNode = createBRNode(lineNumber, startIndex);
            startIndex += emptyBreakNode.raw.length;
            result.push(emptyBreakNode);
            return result;
        }

        // (Paragraph > Str) -> Br?
        var strNode = parseLine(currentLine, lineNumber, startIndex);
        var paragraph = createParagraph([strNode]);
        startIndex += paragraph.raw.length;
        result.push(paragraph);
        if (index !== lastLineIndex) {
            var breakNode = createEndedBRNode(paragraph);
            startIndex += breakNode.raw.length;
            result.push(breakNode);
        }
        return result;
    }, []);
    return {
        type: Syntax.Document,
        raw: text,
        range: [0, text.length],
        loc: {
            start: {
                line: 1,
                column: 0
            },
            end: {
                line: textLineByLine.length,
                column: textLineByLine[textLineByLine.length - 1].length
            }
        },
        children: children
    };
}
module.exports = parse;
//# sourceMappingURL=plaintext-parser.js.map