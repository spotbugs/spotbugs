// MIT © 2017 azu
import { Parent, Text } from "unist-types";
// NLCST spec
// https://github.com/syntax-tree/nlcst
/**
 * Root (Parent) houses all nodes.
 */
export interface Root extends Parent {
    readonly type: "RootNode";
}

/**
 * Paragraph (Parent) represents a self-contained unit of discourse in writing dealing with a particular point or idea.
 */
export interface Paragraph extends Parent {
    readonly type: "ParagraphNode";
}

/**
 * Sentence (Parent) represents grouping of grammatically linked words, that in principle tells a complete thought, although it may make little sense taken in isolation out of context.
 */
export interface Sentence extends Parent {
    readonly type: "SentenceNode";
}

/**
 * Word (Parent) represents the smallest element that may be uttered in isolation with semantic or pragmatic content.
 */
export interface Word extends Parent {
    readonly type: "WordNode";
}

/**
 * Symbol (Text) represents typographical devices like white space, punctuation, signs, and more, different from characters which represent sounds (like letters and numerals).
 */
export interface Symbol extends Text {
    readonly type: "SymbolNode" | "PunctuationNode" | "WhiteSpaceNode" | "SourceNode";
}

/**
 * Punctuation (Symbol) represents typographical devices which aid understanding and correct reading of other grammatical units.
 */
export interface Punctuation extends Symbol {
    readonly type: "PunctuationNode";
}

/**
 * WhiteSpace (Symbol) represents typographical devices devoid of content, separating other grammatical units.
 */
export interface WhiteSpace extends Symbol {
    readonly type: "WhiteSpaceNode";
}

/**
 * Source (Text) represents an external (ungrammatical) value embedded into a grammatical unit: a hyperlink, a line, and such.
 */
export interface Source extends Symbol {
    readonly type: "SourceNode";
}

/**
 * TextNode (Text) represents actual content in an NLCST document: one or more characters. Note that its type property is TextNode, but it is different from the asbtract Text export interface.
 */
export interface TextNode extends Text {
    readonly type: "TextNode";
}
