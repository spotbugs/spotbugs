// LICENSE : MIT
"use strict";

module.exports = function computeLocationOfNode(node) {
    if (!(node.start_line && node.start_column && node.raw)) {
        return node;
    }
    const LINEBREAKE_MARK = "\n";
    const lines = node.raw.split(LINEBREAKE_MARK);
    const addingColumn = lines.length - 1;
    const lastLine = lines[addingColumn];
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
