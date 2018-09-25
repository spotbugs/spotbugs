// MIT © 2017 azu
"use strict";
const { RuleHelper } = require("textlint-rule-helper");
const { splitAST, Syntax: SentenceSyntax } = require("sentence-splitter");
import { getPosFromSingleWord, isCapitalized, upperFirstCharacter } from "./captalize";

const REPORT_TYPE = {
    Heading: "Heading",
    Paragraph: "Paragraph",
    List: "List"
};
const shouldNotCapitalized = (string, allowWords) => {
    // allow words
    const shouldAllowed = allowWords.some(allowWord => {
        return allowWord === string;
    });
    if (shouldAllowed) {
        return true;
    }
    // A quotation
    if (!/^\w/.test(string)) {
        return true;
    }
    // proper word
    const pos = getPosFromSingleWord(string);
    if (/^NNP/.test(pos)) {
        return true;
    }
    return false;
};

/**
 * @param node
 * @param Syntax
 * @param {function} getSource
 * @param report
 * @param RuleError
 * @param fixer
 * @param {boolean} allowFigures enable figures check
 * @param {string[]} allowWords allow lower-case words
 * @param {string} reportType REPORT_TYPE
 */
const checkNode = ({ node, Syntax, getSource, report, RuleError, fixer, allowFigures, allowWords, reportType }) => {
    const DocumentURL = "https://owl.english.purdue.edu/owl/resource/592/01/";
    const paragraphNode = splitAST(node);
    paragraphNode.children.filter(sentence => sentence.type === SentenceSyntax.Sentence).forEach(sentence => {
        const sentenceFirstNode = sentence.children[0];
        if (!sentenceFirstNode) {
            return;
        }
        // check first word is String
        if (sentenceFirstNode.type === Syntax.Str) {
            const text = sentenceFirstNode.value;
            const firstWord = text.split(/\s/)[0];
            if (isCapitalized(firstWord) || shouldNotCapitalized(firstWord, allowWords)) {
                return;
            }
            const index = 0;
            return report(
                sentenceFirstNode,
                new RuleError(
                    `${reportType}: Follow the standard capitalization rules for American English.
See ${DocumentURL}`,
                    {
                        index: index,
                        fix: fixer.replaceTextRange([index, index + firstWord.length], upperFirstCharacter(firstWord))
                    }
                )
            );
        } else if (
            allowFigures &&
            sentenceFirstNode.type === Syntax.Image &&
            typeof sentenceFirstNode.alt === "string"
        ) {
            const text = sentenceFirstNode.alt;
            if (isCapitalized(text) || shouldNotCapitalized(text, allowWords)) {
                return;
            }
            return report(
                sentenceFirstNode,
                new RuleError(
                    `Image alt: Follow the standard capitalization rules for American English
See ${DocumentURL}`
                )
            );
        }
    });
};

const DefaultOptions = {
    // allow lower-case words in Header
    allowHeading: true,
    // allow lower-case words in Image alt
    allowFigures: true,
    // allow lower-case words in ListItem
    allowLists: true,
    // allow lower-case words in anywhere
    allowWords: []
};
const report = (context, options = {}) => {
    const { Syntax, RuleError, getSource, fixer, report } = context;
    const allowHeading = options.allowHeading !== undefined ? options.allowHeading : DefaultOptions.allowHeading;
    const allowLists = options.allowLists !== undefined ? options.allowLists : DefaultOptions.allowLists;
    const allowFigures = options.allowFigures !== undefined ? options.allowFigures : DefaultOptions.allowFigures;
    const allowWords = Array.isArray(options.allowWords) ? options.allowWords : DefaultOptions.allowWords;
    const helper = new RuleHelper(context);
    return {
        [Syntax.Header](node) {
            // options
            if (!allowHeading) {
                return;
            }
            checkNode({
                node,
                Syntax,
                getSource,
                report,
                RuleError,
                fixer,
                allowFigures,
                allowWords,
                reportType: REPORT_TYPE.Heading
            });
        },
        [Syntax.Paragraph](node) {
            if (helper.isChildNode(node, [Syntax.Link, Syntax.Image, Syntax.BlockQuote, Syntax.Emphasis])) {
                return;
            }
            if (helper.isChildNode(node, [Syntax.ListItem])) {
                return;
            }
            checkNode({
                node,
                Syntax,
                getSource,
                report,
                RuleError,
                fixer,
                allowFigures,
                allowWords,
                reportType: REPORT_TYPE.Paragraph
            });
        },
        [Syntax.ListItem](node) {
            if (!allowLists) {
                return;
            }
            node.children.forEach(paragraph => {
                checkNode({
                    node: paragraph,
                    Syntax,
                    getSource,
                    report,
                    RuleError,
                    fixer,
                    allowFigures,
                    allowWords,
                    reportType: REPORT_TYPE.List
                });
            });
        }
    };
};
module.exports = {
    linter: report,
    fixer: report
};
