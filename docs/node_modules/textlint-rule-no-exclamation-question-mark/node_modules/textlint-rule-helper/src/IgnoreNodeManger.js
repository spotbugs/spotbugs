// LICENSE : MIT
"use strict";
const visit = require('unist-util-visit');
/**
 * Ignore node manager that manager ignored ranges.
 *
 */
export default class IgnoreNodeManger {
    constructor() {
        /**
         * @type {[number,number][]}
         * @private
         */
        this._ignoredRangeList = []
    }

    /**
     * |.......|
     * ^       ^
     * Ignored Range
     *
     *    |........|
     *    ^
     *  index
     * @param {number} index
     * @returns {boolean}
     */
    isIgnoredIndex(index) {
        return this._ignoredRangeList.some(range => {
            const [start, end] = range;
            return start <= index && index <= end;
        })
    }

    /**
     * @param {[number, number]} aRange
     * @returns {boolean}
     */
    isIgnoredRange(aRange) {
        const index = aRange[0];
        return this.isIgnoredIndex(index);
    }

    /**
     * @param {Object} node
     * @returns {boolean}
     */
    isIgnored(node) {
        const index = node.index;
        return this.isIgnoredIndex(index);
    }

    /**
     * add node to ignore range list
     * @param {TxtNode} node
     */
    ignore(node) {
        this.ignoreRange(node.range);
    }

    /**
     * add range to ignore range list
     * @param {[number, number]} range
     */
    ignoreRange(range) {
        this._ignoredRangeList.push(range);
    }

    /**
     * ignore children node of `node`,
     * if the children node has the type that is included in `ignoredNodeTypes`.
     * @param {TxtNode} targetNode
     * @param {string[]} ignoredNodeTypes
     */
    ignoreChildrenByTypes(targetNode, ignoredNodeTypes) {
        visit(targetNode, visitedNode => {
            if (ignoredNodeTypes.indexOf(visitedNode.type) !== -1) {
                this.ignore(visitedNode);
            }
        });
    }
}