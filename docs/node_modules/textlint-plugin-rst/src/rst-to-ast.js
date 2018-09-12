"use strict";

import { execSync } from "child_process";
import traverse from "traverse";
import StructuredSource from "structured-source";
import { syntaxMap, reSTAttributeKeyMap } from "./mapping";

function filterAndReplaceNodeAttributes(node) {
    Object.keys(reSTAttributeKeyMap).forEach(key => {
        let v = node[key];
        node[reSTAttributeKeyMap[key]] = v;
        if (v !== undefined) {
            delete node[key];
        }
    });
}

/**
 * parse reStructuredText and return ast mapped location info.
 * @param {string} text
 * @returns {TxtNode}
 */
export function parse(text) {
    let ast = JSON.parse(execSync("rst2ast -q", {input: text}));
    const src = new StructuredSource(text);
    traverse(ast).forEach(function (node) {
        if (this.notLeaf) {
            filterAndReplaceNodeAttributes(node);
            // type
            if (node.type === null) {
                node.type = "text";
            }
            node.type = syntaxMap[node.type];
            if (!node.type) {
                node.type = "Unknown";
            }
            // raw
            node.raw = node.raw || node.value || "";
            // loc
            let lines = node.raw.split("\n");
            if (node.line) {
                node.loc = {
                    start: {line: node.line.start, column: 0},
                    end: {line: node.line.end, column: lines[lines.length-1].length}
                };
                node.range = src.locationToRange(node.loc);
                delete node.line;
            }
        }
    });
    return ast;
}
