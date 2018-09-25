// MIT © 2017 azu
"use strict";
import {
    paragraphReporter,
    getPosFromSingleWord,
    PosType
} from "@textlint-rule/textlint-report-helper-for-google-preset";

const DocumentURL = "https://developers.google.com/style/hyphens";
const report = context => {
    const dictionaries = [
        // Adverbs ending in "ly"
        {
            pattern: /(\w+ly)-(\w+)/g,
            test: ({ captures }) => {
                const pos = getPosFromSingleWord(captures[0]);
                return pos === PosType.Adverb;
            },
            replace: ({ captures }) => {
                return `${captures[0]} ${captures[1]}`;
            },
            message: () => `Don't hyphenate adverbs ending in "ly" except where needed for clarity.`
        },
        // TODO: When to hyphenate
        // TODO: Compound words
        // Range of numbers
        {
            pattern: /(from|between) (\d+-\d+)/g,
            replace: ({ captures }) => {
                return `${captures[1]}`;
            },
            message: () => `Use a hyphen to indicate a range of numbers. Don't add words such as "from" or "between".`
        }
        // Spaces around hyphens => textlint-rule-google-dashes
    ];

    const { Syntax, RuleError, getSource, fixer, report } = context;
    return {
        [Syntax.Paragraph](node) {
            return paragraphReporter({
                Syntax,
                node,
                dictionaries,
                report,
                getSource,
                RuleError,
                fixer
            });
        }
    };
};
module.exports = {
    linter: report,
    fixer: report
};
