// LICENSE : MIT
"use strict";

module.exports = function computeLocationOfNode(node) {
    if (!(node.start_line && node.start_column && node.raw)) {
        return node;
    }
    var LINEBREAKE_MARK = "\n";
    var lines = node.raw.split(LINEBREAKE_MARK);
    var addingColumn = lines.length - 1;
    var lastLine = lines[addingColumn];
    return {
        loc: {
            start: {
                line: node.start_line,
                column: node.start_column
            },
            end: {
                line: node.start_line + addingColumn,
                column: addingColumn.length > 0 ? lastLine.length : node.start_line + lastLine.length
            }
        }
    };
};
//# sourceMappingURL=plaintext-position-node.js.map