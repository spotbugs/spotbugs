import { TxtNode, TxtParentNode } from "@textlint/ast-node-types";
import { AbstractParser } from "./AbstractParser";
export declare class SourceCode {
    private index;
    private source;
    private textCharacters;
    private sourceNode?;
    private contexts;
    private contextRanges;
    private firstChildPadding;
    private startOffset;
    constructor(input: string | TxtParentNode);
    markContextRange(range: [number, number]): void;
    isInContextRange(): boolean;
    enterContext(context: string): void;
    isInContext(context?: string): boolean;
    leaveContext(context: string): void;
    /**
     * Return current offset value
     * @returns {number}
     */
    readonly offset: number;
    /**
     * Return current position object.
     * It includes line, column, offset.
     */
    now(): {
        line: number;
        column: number;
        offset: number;
    };
    /**
     * Return true, no more read char
     */
    readonly hasEnd: boolean;
    /**
     * read char
     * if can not read, return empty string
     * @returns {string}
     */
    read(over?: number): string | false;
    /**
     * read node
     * if can not read, return empty string
     * @returns {node}
     */
    readNode(over?: number): false | TxtNode;
    /**
     * Increment current index
     */
    peek(): void;
    /**
     * Increment node range
     */
    peekNode(node: TxtNode): void;
    /**
     * Seek and Peek
     */
    seekNext(parser: AbstractParser): {
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
    };
    /**
     * Slice text form the range.
     * @param {number} start
     * @param {number} end
     * @returns {string}
     */
    sliceRange(start: number, end: number): string;
}
