// MIT © 2017 azu
"use strict";
import {
    paragraphReporter,
    getPos,
    getPosFromSingleWord
} from "@textlint-rule/textlint-report-helper-for-google-preset";

// https://developers.google.com/style/clause-order
export const nounVerbMessage =
    "Noun+verb contractions: In general, avoid contractions formed from nouns and verbs.\n" +
    "URL: https://developers.google.com/style/contractions";
export const noDoubleContractions =
    "Don't use double contractions: Double contractions contain not just one but two contracted words.\n" +
    "URL: https://developers.google.com/style/contractions";
const report = context => {
    const dictionaries = [
        {
            pattern: /(\w+)'s (\w+)/,
            test: ({ all, captures }) => {
                // name
                return (
                    /^NN/.test(getPosFromSingleWord(captures[0])) &&
                    // Adverb
                    /^RB/.test(getPos(all, captures[1]))
                );
            },
            replace: ({ captures }) => {
                return `${captures[0]} is ${captures[1]}`;
            },
            message: () => nounVerbMessage
        },
        {
            // These machines’re slow.
            pattern: /(\w+)'re (\w+)/,
            test: ({ all, captures }) => {
                // name
                return (
                    /^NN/.test(getPosFromSingleWord(captures[0])) &&
                    // Adverb or Adjective
                    /^RB|JJ/.test(getPos(all, captures[1]))
                );
            },
            replace: ({ captures }) => {
                return `${captures[0]} are ${captures[1]}`;
            },
            message: () => nounVerbMessage
        },
        {
            // The following guides're (a) good way to learn to use Universal Analytics.
            pattern: /(\w+)'re (\w+) (\w+)/,
            test: ({ all, captures }) => {
                // name
                return (
                    /^NN/.test(getPosFromSingleWord(captures[0])) &&
                    // Determiner
                    /DT/.test(getPos(all, captures[1])) &&
                    // Adverb or Adjective
                    /^RB|JJ/.test(getPos(all, captures[2]))
                );
            },
            replace: ({ captures }) => {
                return `${captures[0]} are ${captures[1]} ${captures[2]}`;
            },
            message: () => nounVerbMessage
        },
        // Don't use double contractions
        {
            pattern: /mightn't've/,
            replace: () => "might not have",
            message: () => noDoubleContractions
        },
        {
            pattern: /mustn't've/,
            replace: () => "must not have",
            message: () => noDoubleContractions
        },
        {
            pattern: /wouldn't've/,
            replace: () => "would not have",
            message: () => noDoubleContractions
        },
        {
            pattern: /shouldn't've/,
            replace: () => "should not have",
            message: () => noDoubleContractions
        }
    ];

    const { Syntax, RuleError, getSource, fixer, report } = context;
    return {
        [Syntax.Paragraph](node) {
            paragraphReporter({
                Syntax,
                node,
                dictionaries,
                getSource,
                report,
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
