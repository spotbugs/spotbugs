// MIT © 2017 azu
"use strict";
import { paragraphReporter, getPos } from "@textlint-rule/textlint-report-helper-for-google-preset";

// https://developers.google.com/style/clause-order
export const defaultMessage =
    "Put conditional clauses before instructions, not after.\n" +
    "URL: https://developers.google.com/style/clause-order";
const report = context => {
    const dictionaries = [
        {
            pattern: /See (.*) for more (information|details|detail)./,
            replace: ({ captures }) => {
                return `For more ${captures[1]}, see ${captures[0]}.`;
            },
            message: () => defaultMessage
        },
        {
            pattern: /Click ([\w-]+) if you want to (.+)./,
            test: ({ all, captures }) => {
                return /^VB/.test(getPos(all, captures[0]));
            },
            replace: ({ captures }) => {
                return `To ${captures[1]}, click ${captures[0]}.`;
            },
            message: () => defaultMessage
        }
    ];

    const { Syntax, RuleError, getSource, fixer, report } = context;
    return {
        [Syntax.Paragraph](node) {
            paragraphReporter({
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
