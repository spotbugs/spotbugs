// MIT © 2017 azu
"use strict";
import { paragraphReporter, getPosFromSingleWord } from "@textlint-rule/textlint-report-helper-for-google-preset";

const DocumentURL = "https://developers.google.com/style/ellipses";
const report = context => {
    const { Syntax, RuleError, getSource, fixer, report } = context;
    const dictionaries = [
        // NG: Suspension points
        {
            pattern: / \.\.\. (.*?) \.\.\. /g,
            test: ({ captures }) => {
                // if includes punctuation mark, ignore it
                return !/[!?.]/.test(captures[0]);
            },
            message: () =>
                "Disallow to use ellipses as suspension points." +
                "\n" +
                "https://developers.google.com/style/ellipses#suspension-points"
        },
        // NG: in beginning or end of the text.
        {
            pattern: /"\s*?\.\.\./g,
            message: () =>
                "Disallow to use ellipses in beginning of the text" +
                "\n" +
                "https://developers.google.com/style/ellipses#how-to-use-ellipses"
        },
        {
            pattern: /\.\.\.\s*?"/g,
            message: () =>
                "Disallow to use ellipses in end of the text" +
                "\n" +
                "https://developers.google.com/style/ellipses#how-to-use-ellipses"
        },
        // space
        {
            pattern: /(\w+)\.\.\.(\w+)/g,
            replace: ({ captures }) => {
                return `${captures[0]} ... ${captures[1]}`;
            },
            message: () => "Insert one space before and after the ellipsis" + "\n" + DocumentURL
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
