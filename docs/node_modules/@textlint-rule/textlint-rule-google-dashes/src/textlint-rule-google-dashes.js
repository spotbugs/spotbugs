// MIT © 2017 azu
"use strict";
import { strReporter, getPos, PosType } from "@textlint-rule/textlint-report-helper-for-google-preset";

const { RuleHelper } = require("textlint-rule-helper");
const DocumentURL = "https://developers.google.com/style/dashes";
const report = context => {
    const { Syntax, RuleError, getSource, fixer, report } = context;
    // Notes: the order is important when Apply fixes
    const dictionaries = [
        {
            // Prefer colon to dash.
            // Partial support:
            // use colon instead of dash or hyphen
            pattern: /((?:^.* )?(\w+)) ([—-]) ((\w+) .*)$/,
            test: ({ all, captures }) => {
                const dashes = captures[2];
                const afterText = captures[3];
                // OK:
                // The food — which was delicious — reminded me of home.
                if (afterText.includes(dashes)) {
                    return false;
                }
                const afterWord = captures[4];
                const afterWordPos = getPos(all, afterWord);
                // example - This is a example
                //           ^^^^
                if (
                    !(
                        afterWordPos === PosType.WhDeterminer ||
                        afterWordPos === PosType.WhPronoun ||
                        afterWordPos === PosType.Determiner
                    )
                ) {
                    return false;
                }
                const pos = getPos(all, captures[1]);
                return pos === PosType.Noun;
            },
            replace: ({ captures }) => {
                return `${captures[0]}: ${captures[3]}`;
            },
            message: () => "Use colons(:) instead of dashes(-) in lists" + "\n" + DocumentURL
        },
        {
            // use "—"(em dash) instead of " - "(hyphen)
            // Notes: Allow to use hyphen for Ranges of numbers
            // https://developers.google.com/style/numbers#ranges-of-numbers
            pattern: /([a-zA-Z]+) - ([a-zA-Z]+)/g,
            replace: ({ captures }) => {
                return `${captures[0]}—${captures[1]}`;
            },
            message: () => 'Use "—"(em dash) instead of " - "(hyphen)' + "\n" + DocumentURL
        }
    ];

    const helper = new RuleHelper(context);
    return {
        [Syntax.Str](node) {
            if (helper.isChildNode(node, [Syntax.Link, Syntax.Image, Syntax.BlockQuote, Syntax.Emphasis])) {
                return;
            }
            // use colon instead of dash or hyphen can't work on Paragraph
            // Because, replace `code` with wrong range...
            // Temporary, we use strReporter
            return strReporter({
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
