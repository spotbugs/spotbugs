// MIT © 2017 azu
"use strict";
import { paragraphReporter } from "@textlint-rule/textlint-report-helper-for-google-preset";

const report = context => {
    const dictionaries = [
        // Abbreviations not to use
        {
            pattern: /e\.g\./g,
            message: () =>
                `Don't use "e.g.", instead, use "for example".` +
                "\n" +
                "https://developers.google.com/style/abbreviations#dont-use"
        },
        {
            pattern: /i\.e\./g,
            message: () =>
                `Don't use "i.e.", instead, use "that is".` +
                "\n" +
                "https://developers.google.com/style/abbreviations#dont-use"
        },
        {
            pattern: /\b([A-Z]+)\. /g,
            replace: ({ captures }) => {
                return `${captures[0]} `;
            },
            message: () =>
                `Don't use periods with acronyms or initialisms.` +
                "\n" +
                "https://developers.google.com/style/abbreviations#periods"
        }
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
