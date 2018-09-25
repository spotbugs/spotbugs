// MIT © 2017 azu
"use strict";
import { paragraphReporter, getPosFromSingleWord } from "@textlint-rule/textlint-report-helper-for-google-preset";

const DocumentURL = "https://developers.google.com/style/possessives";
const report = context => {
    const { Syntax, RuleError, getSource, fixer, report } = context;
    const dictionaries = [
        // NG: plural word + 's
        {
            pattern: /(\w+)'s/,
            test: ({ captures }) => {
                const word = captures[0];
                // if plural word is ended in "s", ignore it.
                const isEndedS = word[word.length - 1] === "s";
                const wordPos = getPosFromSingleWord(word);
                // Plural word
                return wordPos === "NNS" && isEndedS;
            },
            message: () =>
                'A plural noun that does end in "s", add an apostrophe(\') but no additional "s" or use "of"' +
                "\n" +
                DocumentURL
        },
        // NG: singular noun + '
        {
            pattern: /([\w\s]+)'(?!s)/,
            test: ({ captures }) => {
                // ... word's
                // or ... the word's
                const words = captures[0].split(" ");
                const determinerWord = words[words.length - 2];
                const targetWord = words[words.length - 1];
                // if "the word's", ignore this
                if (determinerWord !== undefined) {
                    const determinerType = getPosFromSingleWord(determinerWord);
                    // skip: the a
                    if (determinerType === "DT") {
                        return false;
                    }
                }
                const wordPos = getPosFromSingleWord(targetWord);
                // singular noun(singular noun or Proper noun)
                return wordPos === "NNP" || wordPos === "NN";
            },
            message: () =>
                'To form a possessive of a singular noun (regardless of whether it ends in s) or a plural noun that doesn\'t end in "s", add "\'s" to the end of the word' +
                "\n" +
                DocumentURL
        }
    ];

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
