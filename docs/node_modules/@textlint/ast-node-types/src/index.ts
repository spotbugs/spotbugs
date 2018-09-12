// MIT © 2017 azu
"use strict";

/**
 * AST Node types list on TxtNode.
 * Constant value of types
 * @see https://github.com/textlint/textlint/blob/master/docs/txtnode.md
 */
export enum ASTNodeTypes {
    Document = "Document",
    Paragraph = "Paragraph",
    BlockQuote = "BlockQuote",
    ListItem = "ListItem",
    List = "List",
    Header = "Header",
    CodeBlock = "CodeBlock",
    HtmlBlock = "HtmlBlock",
    ReferenceDef = "ReferenceDef",
    HorizontalRule = "HorizontalRule",
    Comment = "Comment",
    // inline
    Str = "Str",
    Break = "Break", // well-known Hard Break
    Emphasis = "Emphasis",
    Strong = "Strong",
    Html = "Html",
    Link = "Link",
    Image = "Image",
    Code = "Code",
    Delete = "Delete"
}

/**
 * Key of ASTNodeTypes or any string
 * For example, TxtNodeType is "Document".
 */
export type TxtNodeType = keyof typeof ASTNodeTypes | string;

/**
 * Basic TxtNode
 * Probably, Real TxtNode implementation has more properties.
 */
export interface TxtNode {
    type: TxtNodeType;
    raw: string;
    range: TextNodeRange;
    loc: TxtNodeLineLocation;
    // parent is runtime information
    // Not need in AST
    // For example, top Root Node like `Document` has not parent.
    parent?: TxtNode;

    [index: string]: any;
}

/**
 * Location
 */
export interface TxtNodeLineLocation {
    start: TxtNodePosition;
    end: TxtNodePosition;
}

/**
 * Position's line start with 1.
 * Position's column start with 0.
 * This is for compatibility with JavaScript AST.
 * https://gist.github.com/azu/8866b2cb9b7a933e01fe
 */
export interface TxtNodePosition {
    line: number; // start with 1
    column: number; // start with 0
}

/**
 * Range start with 0
 */
export type TextNodeRange = [number, number];

/**
 * Text Node.
 * Text Node has inline value.
 * For example, `Str` Node is an TxtTextNode.
 */
export interface TxtTextNode extends TxtNode {
    value: string;
}

/**
 * Parent Node.
 * Parent Node has children that are consist of TxtNode or TxtTextNode
 */
export interface TxtParentNode extends TxtNode {
    children: Array<TxtNode | TxtTextNode>;
}
