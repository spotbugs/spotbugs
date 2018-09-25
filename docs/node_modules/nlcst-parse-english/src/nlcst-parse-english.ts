// MIT © 2017 azu
import { Root, Sentence, Word } from "nlcst-types";
import { Tag } from "en-pos";

const visit = require("unist-util-visit");
const toString = require("nlcst-to-string");
const English = require("parse-english");
// fix for the word "constructor" which is not in the lexicon (and returns a function which for
// https://github.com/noblesamurai/node-pos-tag/blob/master/index.js
const lexicon = require("en-lexicon");
lexicon.lexicon["constructor"] = "NNP";

export type PosType =
    | "NN"
    | "NNS"
    | "NNP"
    | "NNPS"
    | "VB"
    | "VBP"
    | "VBZ"
    | "VBG"
    | "VBD"
    | "VBN"
    | "MD"
    | "JJ"
    | "JJR"
    | "JJS"
    | "RB"
    | "RBR"
    | "RBS"
    | "DT"
    | "PDT"
    | "PRP"
    | "PRP$"
    | "POS"
    | "IN"
    | "PR"
    | "TO"
    | "WDT"
    | "WP"
    | "WP$"
    | "WRB"
    | "EX"
    | "CC"
    | "CD"
    | "LS"
    | "UH"
    | "FW"
    | ","
    | ":"
    | "."
    | "("
    | ")"
    | "#"
    | "$"
    | "SYM"
    | "EM";

export interface PosWordNode extends Word {
    data: {
        // POS tag: https://github.com/finnlp/en-pos
        pos: PosType;
    };
}

export class EnglishParser {
    private parser: { parse: (text: string) => Root };

    constructor() {
        this.parser = new English();
    }

    parse(text: string): Root {
        const NLCST = this.parser.parse(text);
        visit(NLCST, "SentenceNode", function(node: Sentence) {
            const sentenceChildren = node.children.filter(c =>
                ["WordNode", "PunctuationNode", "SymbolNode"].includes(c.type)
            );
            const strings = sentenceChildren.map(c => toString(c));
            // HACK: Truncate any word longer than 40 chars as en-pos will not be performant.
            const tags = new Tag(strings.map(word => word.slice(0, 40))).initial().smooth().tags;
            sentenceChildren.forEach((node, i) => {
                node.data = {
                    pos: tags[i]
                };
            });
        });
        return NLCST;
    }
}
