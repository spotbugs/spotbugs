import { TxtNode, TxtParentNode } from "@textlint/ast-node-types";
import { AbstractParser } from "./AbstractParser";

const StructureSource = require("structured-source");

export class SourceCode {
    private index: number = 0;
    private source: any;
    private textCharacters: string[];
    private sourceNode?: TxtParentNode;
    private contexts: string[] = [];
    private contextRanges: [number, number][] = [];
    private firstChildPadding: number;
    private startOffset: number;

    constructor(input: string | TxtParentNode) {
        if (typeof input === "string") {
            this.textCharacters = input.split("");
            this.source = new StructureSource(input);
            this.startOffset = 0;
            this.firstChildPadding = 0;
        } else {
            this.sourceNode = input;
            // When pass AST, fist node may be >=
            // Preserve it as `startOffset`
            this.startOffset = this.sourceNode.range[0];
            // start index is startOffset
            this.index = this.startOffset;
            // before line count of Paragraph node
            const lineBreaks = Array.from(new Array(this.sourceNode.loc.start.line - 1)).fill("\n");
            // filled with dummy text
            const offset = Array.from(new Array(this.startOffset - lineBreaks.length)).fill("∯");
            this.textCharacters = offset.concat(lineBreaks, input.raw.split(""));
            this.source = new StructureSource(this.textCharacters.join(""));
            if (this.sourceNode.children[0]) {
                // Header Node's children does not start with index 0
                // Example: # Header
                // It firstChildPadding is `2`
                this.firstChildPadding = this.sourceNode.children[0].range[0] - this.startOffset;
            } else {
                this.firstChildPadding = 0;
            }
        }
    }

    markContextRange(range: [number, number]) {
        this.contextRanges.push(range);
    }

    isInContextRange() {
        const offset = this.offset;
        return this.contextRanges.some(range => {
            return range[0] <= offset && offset < range[1];
        });
    }

    enterContext(context: string) {
        this.contexts.push(context);
    }

    isInContext(context?: string) {
        if (!context) {
            return this.contexts.length > 0;
        }
        return this.contexts.some(targetContext => targetContext === context);
    }

    leaveContext(context: string) {
        const index = this.contexts.lastIndexOf(context);
        if (index !== -1) {
            this.contexts.splice(index, 1);
        }
    }

    /**
     * Return current offset value
     * @returns {number}
     */
    get offset() {
        return this.index + this.firstChildPadding;
    }

    /**
     * Return current position object.
     * It includes line, column, offset.
     */
    now() {
        const indexWithChildrenOffset = this.offset;
        const position = this.source.indexToPosition(indexWithChildrenOffset);
        return {
            line: position.line as number,
            column: position.column as number,
            offset: indexWithChildrenOffset
        };
    }

    /**
     * Return true, no more read char
     */
    get hasEnd() {
        return this.read() === false;
    }

    /**
     * read char
     * if can not read, return empty string
     * @returns {string}
     */
    read(over: number = 0) {
        const index = this.offset + over;
        if (index < this.startOffset) {
            return false;
        }
        if (0 <= index && index < this.textCharacters.length) {
            return this.textCharacters[index];
        }
        return false;
    }

    /**
     * read node
     * if can not read, return empty string
     * @returns {node}
     */
    readNode(over: number = 0) {
        if (!this.sourceNode) {
            return false;
        }
        const index = this.offset + over;
        if (index < this.startOffset) {
            return false;
        }
        const matchNodeList = this.sourceNode.children.filter(node => {
            // <p>[node]</p>
            //         ^
            //        range[1]
            // `< range[1]` prevent infinity loop
            // https://github.com/azu/sentence-splitter/issues/9
            return node.range[0] <= index && index < node.range[1];
        });
        if (matchNodeList.length > 0) {
            // last match
            // because, range is overlap two nodes
            return matchNodeList[matchNodeList.length - 1];
        }
        return false;
    }

    /**
     * Increment current index
     */
    peek() {
        this.index += 1;
    }

    /**
     * Increment node range
     */
    peekNode(node: TxtNode) {
        this.index += node.range[1] - node.range[0];
    }

    /**
     * Seek and Peek
     */
    seekNext(
        parser: AbstractParser
    ): {
        value: string;
        startPosition: {
            line: number;
            column: number;
            offset: number;
        };
        endPosition: {
            line: number;
            column: number;
            offset: number;
        };
    } {
        const startPosition = this.now();
        parser.seek(this);
        const endPosition = this.now();
        const value = this.sliceRange(startPosition.offset, endPosition.offset);
        return {
            value,
            startPosition,
            endPosition
        };
    }

    /**
     * Slice text form the range.
     * @param {number} start
     * @param {number} end
     * @returns {string}
     */
    sliceRange(start: number, end: number): string {
        return this.textCharacters.slice(start, end).join("");
    }
}
