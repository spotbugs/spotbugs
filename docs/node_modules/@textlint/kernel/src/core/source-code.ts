import { TxtNode } from "@textlint/ast-node-types";

const assert = require("assert");
const StructuredSource = require("structured-source");
import { ASTNodeTypes } from "@textlint/ast-node-types";

/**
 * Validates that the given AST has the required information.
 * @param {TxtNode} [ast] The Program node of the AST to check.
 * @throws {Error} If the AST doesn't contain the correct information.
 * @returns {void}
 * @private
 */
function validate(ast: TxtNode) {
    if (!ast.loc) {
        throw new Error("AST is missing location information.");
    }

    if (!ast.range) {
        throw new Error("AST is missing range information");
    }
}

export interface SourceCodePosition {
    line: number;
    column: number;
}

/**
 * Line number starts with 1.
 * Column number starts with 0.
 */
export interface SourceCodeLocation {
    start: SourceCodePosition;
    end: SourceCodePosition;
}

export type SourceCodeRange = [number, number];
export type SourceCodeArgs = { text: string; ast: TxtNode; ext: string; filePath?: string };
/**
 * This class represent of source code.
 */
export default class SourceCode {
    hasBOM: boolean;
    text: string;
    ast: TxtNode;
    filePath: string | undefined;
    ext: string;

    private _structuredSource: any;

    /**
     * @param {string} text
     * @param {Object} ast
     * @param {string} ext
     * @param {string} [filePath]
     */
    constructor({ text = "", ast, ext, filePath }: SourceCodeArgs) {
        validate(ast);
        assert(ext || filePath, "should be set either of fileExt or filePath.");
        this.hasBOM = text.charCodeAt(0) === 0xfeff;
        this.text = this.hasBOM ? text.slice(1) : text;
        /**
         * @type StructuredSource
         */
        this._structuredSource = new StructuredSource(this.text);
        this.ast = ast;
        this.filePath = filePath;
        /**
         * fileType .md .txt ...
         * @type {string}
         */
        this.ext = ext;
    }

    /**
     * @returns {ASTNodeTypes}
     */
    getSyntax() {
        return ASTNodeTypes;
    }

    /**
     * get filePath
     * @returns {string|undefined}
     */
    getFilePath() {
        return this.filePath;
    }

    /**
     * Gets the source code for the given node.
     * @param {TxtNode=} node The AST node to get the text for.
     * @param {int=} beforeCount The number of characters before the node to retrieve.
     * @param {int=} afterCount The number of characters after the node to retrieve.
     * @returns {string} The text representing the AST node.
     */
    getSource(node?: TxtNode, beforeCount?: number, afterCount?: number): string {
        const currentText = this.text;
        if (node) {
            const start = Math.max(node.range[0] - (beforeCount || 0), 0);
            const end = node.range[1] + (afterCount || 0);
            return currentText.slice(start, end);
        } else {
            return currentText;
        }
    }

    // StructuredSource wrapper
    /**
     * @param {SourceCodeLocation} loc - location indicator.
     * @return {[ number, number ]} range.
     */
    locationToRange(loc: SourceCodeLocation): SourceCodeRange {
        return this._structuredSource.locationToRange(loc);
    }

    /**
     * @param {[ number, number ]} range - pair of indice.
     * @return {SourceCodeLocation} location.
     */
    rangeToLocation(range: SourceCodeRange): SourceCodeLocation {
        return this._structuredSource.rangeToLocation(range);
    }

    /**
     * @param {Position} pos - position indicator.
     * @return {number} index.
     */
    positionToIndex(pos: SourceCodePosition): number {
        return this._structuredSource.positionToIndex(pos);
    }

    /**
     * @param {number} index - index to the source code.
     * @return {Position} position.
     */
    indexToPosition(index: number): SourceCodePosition {
        return this._structuredSource.indexToPosition(index);
    }
}
