// LICENSE : MIT
"use strict";
import { TxtNode, TxtParentNode, ASTNodeTypes, TxtTextNode } from "@textlint/ast-node-types";
import { SourceCode } from "./parser/SourceCode";
import { AbstractParser } from "./parser/AbstractParser";
import { NewLineParser } from "./parser/NewLineParser";
import { SpaceParser } from "./parser/SpaceParser";
import { SeparatorParser } from "./parser/SeparatorParser";
import { AnyValueParser } from "./parser/AnyValueParser";
import { AbbrMarker } from "./parser/AbbrMarker";
import { PairMaker } from "./parser/PairMaker";
import { debugLog } from "./logger";

export const Syntax = {
    WhiteSpace: "WhiteSpace",
    Punctuation: "Punctuation",
    Sentence: "Sentence",
    Str: "Str"
};

export interface ToTypeNode<T extends string> extends TxtTextNode {
    readonly type: T;
}

export interface WhiteSpaceNode extends TxtTextNode {
    readonly type: "WhiteSpace";
}

export interface PunctuationNode extends TxtTextNode {
    readonly type: "Punctuation";
}

export interface StrNode extends TxtTextNode {
    readonly type: "Str";
}

export interface SentenceNode extends TxtParentNode {
    readonly type: "Sentence";
}

export class SplitParser {
    private nodeList: TxtParentNode[] = [];
    private results: (TxtParentNode | TxtNode)[] = [];
    public source: SourceCode;

    constructor(text: string | TxtParentNode) {
        this.source = new SourceCode(text);
    }

    get current(): TxtParentNode | undefined {
        return this.nodeList[this.nodeList.length - 1];
    }

    pushNodeToCurrent(node: TxtNode) {
        const current = this.current;
        if (current) {
            current.children.push(node);
        } else {
            // Under the root
            this.results.push(node);
        }
    }

    // open with ParentNode
    open(parentNode: TxtParentNode) {
        this.nodeList.push(parentNode);
    }

    isOpened() {
        return this.nodeList.length > 0;
    }

    nextLine(parser: AbstractParser) {
        const { value, startPosition, endPosition } = this.source.seekNext(parser);
        this.pushNodeToCurrent(createWhiteSpaceNode(value, startPosition, endPosition));
        return endPosition;
    }

    nextSpace(parser: AbstractParser) {
        const { value, startPosition, endPosition } = this.source.seekNext(parser);
        this.pushNodeToCurrent(createNode("WhiteSpace", value, startPosition, endPosition));
    }

    nextValue(parser: AbstractParser) {
        const { value, startPosition, endPosition } = this.source.seekNext(parser);
        this.pushNodeToCurrent(createTextNode(value, startPosition, endPosition));
    }

    // close current Node and remove it from list
    close(parser: AbstractParser) {
        const { value, startPosition, endPosition } = this.source.seekNext(parser);
        if (startPosition.offset !== endPosition.offset) {
            this.pushNodeToCurrent(createPunctuationNode(value, startPosition, endPosition));
        }
        const currentNode = this.nodeList.pop();
        if (!currentNode) {
            return;
        }
        if (currentNode.children.length === 0) {
            return;
        }
        const firstChildNode: TxtNode = currentNode.children[0];
        const endNow = this.source.now();
        currentNode.loc = {
            start: firstChildNode.loc.start,
            end: nowToLoc(endNow)
        };
        const rawValue = this.source.sliceRange(firstChildNode.range[0], endNow.offset);
        currentNode.range = [firstChildNode.range[0], endNow.offset];
        currentNode.raw = rawValue;
        this.results.push(currentNode);
    }

    toList() {
        return this.results;
    }
}

/**
 * split `text` into Sentence nodes
 */
export function split(text: string): (TxtParentNode | TxtNode)[] {
    const newLine = new NewLineParser();
    const space = new SpaceParser();
    const separator = new SeparatorParser();
    const abbrMarker = new AbbrMarker();
    const pairMaker = new PairMaker();
    // anyValueParser has multiple parser and markers.
    // anyValueParse eat any value if it reach to other value.
    const anyValueParser = new AnyValueParser({
        parsers: [newLine, separator],
        markers: [abbrMarker, pairMaker]
    });
    const splitParser = new SplitParser(text);
    const sourceCode = splitParser.source;
    while (!sourceCode.hasEnd) {
        if (newLine.test(sourceCode)) {
            splitParser.nextLine(newLine);
        } else if (space.test(sourceCode)) {
            // Add WhiteSpace
            splitParser.nextSpace(space);
        } else if (separator.test(sourceCode)) {
            splitParser.close(separator);
        } else {
            if (!splitParser.isOpened()) {
                splitParser.open(createEmptySentenceNode());
            }
            splitParser.nextValue(anyValueParser);
        }
    }
    splitParser.close(space);
    return splitParser.toList();
}

/**
 * Convert Paragraph Node to Paragraph node that convert children to Sentence node
 * This Node is based on TxtAST.
 * See https://github.com/textlint/textlint/blob/master/docs/txtnode.md
 */
export function splitAST(paragraphNode: TxtParentNode): TxtParentNode {
    const newLine = new NewLineParser();
    const space = new SpaceParser();
    const separator = new SeparatorParser();
    const abbrMarker = new AbbrMarker();
    const pairMaker = new PairMaker();
    const anyValue = new AnyValueParser({
        parsers: [newLine, separator],
        markers: [abbrMarker, pairMaker]
    });
    const splitParser = new SplitParser(paragraphNode);
    const sourceCode = splitParser.source;
    while (!sourceCode.hasEnd) {
        const currentNode = sourceCode.readNode();
        if (!currentNode) {
            break;
        }
        if (currentNode.type === ASTNodeTypes.Str) {
            if (space.test(sourceCode)) {
                debugLog("space");
                splitParser.nextSpace(space);
            } else if (separator.test(sourceCode)) {
                debugLog("separator");
                splitParser.close(separator);
            } else if (newLine.test(sourceCode)) {
                debugLog("newline");
                splitParser.nextLine(newLine);
            } else {
                if (!splitParser.isOpened()) {
                    debugLog("open -> createEmptySentenceNode()");
                    splitParser.open(createEmptySentenceNode());
                }
                splitParser.nextValue(anyValue);
            }
        } else {
            if (!splitParser.isOpened()) {
                splitParser.open(createEmptySentenceNode());
            }
            splitParser.pushNodeToCurrent(currentNode);
            sourceCode.peekNode(currentNode);
        }
    }

    // It follow some text that is not ended with period.
    // TODO: space is correct?
    splitParser.close(space);
    return {
        ...paragraphNode,
        children: splitParser.toList()
    };
}

/**
 * WhiteSpace is space or linebreak
 */
export function createWhiteSpaceNode(
    text: string,
    startPosition: {
        line: number;
        column: number;
        offset: number;
    },
    endPosition: {
        line: number;
        column: number;
        offset: number;
    }
) {
    return createNode("WhiteSpace", text, startPosition, endPosition);
}

export function createPunctuationNode(
    text: string,
    startPosition: {
        line: number;
        column: number;
        offset: number;
    },
    endPosition: {
        line: number;
        column: number;
        offset: number;
    }
): PunctuationNode {
    return createNode("Punctuation", text, startPosition, endPosition);
}

export function createTextNode(
    text: string,
    startPosition: {
        line: number;
        column: number;
        offset: number;
    },
    endPosition: {
        line: number;
        column: number;
        offset: number;
    }
): StrNode {
    return createNode("Str", text, startPosition, endPosition);
}

export function createEmptySentenceNode(): SentenceNode {
    return {
        type: "Sentence",
        raw: "",
        loc: {
            start: { column: NaN, line: NaN },
            end: { column: NaN, line: NaN }
        },
        range: [NaN, NaN],
        children: []
    };
}

export function createNode<T extends string>(
    type: T,
    text: string,
    startPosition: {
        line: number;
        column: number;
        offset: number;
    },
    endPosition: {
        line: number;
        column: number;
        offset: number;
    }
): ToTypeNode<T> {
    return {
        type: type,
        raw: text,
        value: text,
        loc: {
            start: nowToLoc(startPosition),
            end: nowToLoc(endPosition)
        },
        range: [startPosition.offset, endPosition.offset]
    };
}

function nowToLoc(now: { line: number; column: number; offset: number }) {
    return {
        line: now.line,
        column: now.column
    };
}
