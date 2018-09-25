// MIT © 2017 azu
import { Paragraph, Punctuation, Root, Sentence, Source, TextNode, WhiteSpace, Word } from "./nlcst-types";
import { Text } from "unist-types";

export const isRoot = (v: any): v is Root => {
    return v && v.type === "RootNode";
};
export const isParagraph = (v: any): v is Paragraph => {
    return v && v.type === "ParagraphNode";
};
export const isSentence = (v: any): v is Sentence => {
    return v && v.type === "SentenceNode";
};
export const isWord = (v: any): v is Word => {
    return v && v.type === "WordNode";
};
export const isText = (v: any): v is Text => {
    return v && v.type === "TextNode";
};
export const isSymbol = (v: any): v is Symbol => {
    return v && v.type === "SymbolNode";
};
export const isPunctuation = (v: any): v is Punctuation => {
    return v && v.type === "PunctuationNode";
};
export const isWhiteSpace = (v: any): v is WhiteSpace => {
    return v && v.type === "WhiteSpaceNode";
};
export const isSource = (v: any): v is Source => {
    return v && v.type === "SourceNode";
};
export const isTextNode = (v: any): v is TextNode => {
    return v && v.type === "TextNode";
};
