// LICENSE : MIT
"use strict";
import ObjectAssign from "object-assign";
import StructuredSource from "structured-source";

export default class StringSource {
    constructor(node) {
        this.rootNode = node;
        this.tokenMaps = [];
        this.generatedString = "";
        // pre calculate
        this._stringify(this.rootNode);
        this.originalSource = new StructuredSource(this.rootNode.raw);
        this.generatedSource = new StructuredSource(this.generatedString);
        /*
         [
         // e.g.) **Str**
         {
         // original range
         // e.g.) [0, 7] = `**Str**`
         original : [start, end]
         // intermediate = trim decoration from Original
         // e.g.) [2, 5]
         intermediate: [start, end]
         // generated value = "Str"
         // e.g.) [0, 3]
         generated : [start, end]
         }]
         */
    }

    toString() {
        return this.generatedString;
    }

    /**
     * @deprecated use originalIndexFromIndex instead of
     * @param targetIndex
     */
    originalIndexFor(targetIndex) {
        return this.originalIndexFromIndex(targetIndex);
    }

    /**
     * @deprecated use originalPositionFromPosition instead of
     * @param generatedPosition
     * @param {boolean}  isEnd - is the position end of the node?

     * @returns {Object}
     */
    originalPositionFor(generatedPosition, isEnd) {
        return this.originalPositionFromPosition(generatedPosition, isEnd);
    }

    /**
     * get original index from generated index value
     * @param {number} generatedIndex - position is a index value.
     * @param {boolean}  isEnd - is the position end of the node?
     * @returns {number|undefined} original
     */
    originalIndexFromIndex(generatedIndex, isEnd = false) {
        let hitTokenMaps = this.tokenMaps.filter((tokenMap, index) => {
            const generated = tokenMap.generated;
            const nextTokenMap = this.tokenMaps[index + 1];
            const nextGenerated = nextTokenMap ? nextTokenMap.generated : null;
            if (nextGenerated) {
                if (generated[0] <= generatedIndex && generatedIndex <= nextGenerated[0]) {
                    return true;
                }
            } else {
                if (generated[0] <= generatedIndex && generatedIndex <= generated[1]) {
                    return true;
                }
            }
        });
        if (hitTokenMaps.length === 0) {
            return;
        }

        /**
         * **Str**ABC
         *     |
         *     |
         *   generatedIndex
         *
         * If isEnd is true, generatedIndex is end of **Str** node.
         * If isEnd is false, generatedIndex is index of ABC node.
         */

        const hitTokenMap = isEnd ? hitTokenMaps[0] : hitTokenMaps[hitTokenMaps.length - 1];
        // <----------->[<------------->|text]
        //              ^        ^
        //   position-generated  intermediate-origin

        // <-------------->[<------------->|text]
        //       |         |
        //  outer adjust   _
        //            inner adjust = 1
        const outerAdjust = generatedIndex - hitTokenMap.generated[0];
        const innerAdjust = hitTokenMap.intermediate[0] - hitTokenMap.original[0];
        return outerAdjust + innerAdjust + hitTokenMap.original[0];
    }

    /**
     * get original position from generated position
     * @param {object} position
     * @param {boolean}  isEnd - is the position end of the node?
     * @returns {object} original position
     */
    originalPositionFromPosition(position, isEnd = false) {
        if (typeof position.line === "undefined" || typeof position.column === "undefined") {
            throw new Error("position.{line, column} should not undefined: " + JSON.stringify(position));
        }
        const generatedIndex = this.generatedSource.positionToIndex(position);
        if (isNaN(generatedIndex)) {
            // Not Found
            return;
        }
        const originalIndex = this.originalIndexFromIndex(generatedIndex, isEnd);
        return this.originalSource.indexToPosition(originalIndex, isEnd);
    }

    /**
     * get original index from generated position
     * @param {object} generatedPosition
     * @param {boolean}  isEnd - is the position end of the node?
     * @returns {number} original index
     */
    originalIndexFromPosition(generatedPosition, isEnd = false) {
        const originalPosition = this.originalPositionFromPosition(generatedPosition);
        return this.originalSource.positionToIndex(originalPosition, isEnd);
    }

    /**
     * get original position from generated index
     * @param {number} generatedIndex
     * @param {boolean} isEnd - is the position end of the node?
     * @return {object} original position
     */
    originalPositionFromIndex(generatedIndex, isEnd = false) {
        let originalIndex = this.originalIndexFromIndex(generatedIndex);
        return this.originalSource.indexToPosition(originalIndex, isEnd);
    }


    isParagraphNode(node) {
        return node.type === "Paragraph";
    }

    isStringNode(node) {
        return node.type === "Str";
    }

    /**
     *
     * @param node
     * @returns {string|undefined}
     * @private
     */
    _getValue(node) {
        if (node.value) {
            return node.value;
        } else if (node.alt) {
            return node.alt;
        } else if (node.title) {
            // See https://github.com/azu/textlint-rule-sentence-length/issues/6
            if (node.type === "Link") {
                return;
            }
            return node.title;
        }
    }

    _nodeRangeAsRelative(node) {
        // relative from root
        return [
            node.range[0] - this.rootNode.range[0],
            node.range[1] - this.rootNode.range[0]
        ]
    }

    _valueOf(node, parent) {
        if (!node) {
            return;
        }


        // [padding][value][padding]
        // =>
        // [value][value][value]
        const value = this._getValue(node);
        if (!value) {
            return;
        }
        if (parent === null || parent === undefined) {
            return;
        }
        // <p><Str /></p>
        if (this.isParagraphNode(parent) && this.isStringNode(node)) {
            return {
                original: this._nodeRangeAsRelative(node),
                intermediate: this._nodeRangeAsRelative(node),
                value: value
            };
        }
        // <p><code>code</code></p>
        // => container is <p>
        // <p><strong><Str /></strong></p>
        // => container is <strong>
        let container = this.isParagraphNode(parent) ? node : parent;
        let rawValue = container.raw;
        // avoid match ! with ![
        // TODO: indexOf(value, 1) 1 is unexpected ...
        let paddingLeft = rawValue.indexOf(value, 1) === -1 ? 0 : rawValue.indexOf(value, 1);
        let paddingRight = rawValue.length - (paddingLeft + value.length);
        // original range should be relative value from rootNode
        let originalRange = this._nodeRangeAsRelative(container);
        let intermediateRange = [
            originalRange[0] + paddingLeft,
            originalRange[1] - paddingRight
        ];
        return {
            original: originalRange,
            intermediate: intermediateRange,
            value: value
        };

    }

    _addTokenMap(tokenMap) {
        if (tokenMap == null) {
            return;
        }
        let addedTokenMap = ObjectAssign({}, tokenMap);
        if (this.tokenMaps.length === 0) {
            let textLength = addedTokenMap.intermediate[1] - addedTokenMap.intermediate[0];
            addedTokenMap["generated"] = [0, textLength];
        } else {
            let textLength = addedTokenMap.intermediate[1] - addedTokenMap.intermediate[0];
            addedTokenMap["generated"] = [this.generatedString.length, this.generatedString.length + textLength];
        }
        this.generatedString += tokenMap.value;
        this.tokenMaps.push(addedTokenMap);
    }

    /**
     * Compute text content of a node.  If the node itself
     * does not expose plain-text fields, `toString` will
     * recursivly try its children.
     *
     * @param {Node} node - Node to transform to a string.
     * @param {Node} [parent] - Parent Node of the `node`.
     */
    _stringify(node, parent) {
        let value = this._valueOf(node, parent);
        if (value) {
            return value;
        }
        if (!node.children) {
            return;
        }
        node.children.forEach((childNode) => {
            let tokenMap = this._stringify(childNode, node);
            if (tokenMap) {
                this._addTokenMap(tokenMap);
            }
        });
    }
}
