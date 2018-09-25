// MIT © 2017 azu
"use strict";
import { shouldIgnoreNodeOfStrNode, strReporter } from "@textlint-rule/textlint-report-helper-for-google-preset";
import { classifyArticle } from "english-article-classifier";

const DocumentURL = "https://developers.google.com/style/articles";
const isCapital = text => {
    return /^[A-Z]/.test(text);
};
const report = (context, options = {}) => {
    const forceA = Array.isArray(options.a) ? options.a : [];
    const forceAn = Array.isArray(options.an) ? options.an : [];
    const classifyOptions = {
        forceA,
        forceAn
    };
    const dictionaries = [
        {
            pattern: /\b(a) ([\w.-]+)\b/i,
            test: ({ captures }) => {
                const result = classifyArticle(captures[1], classifyOptions);
                return result.type === "an";
            },
            replace: ({ captures }) => {
                const an = isCapital(captures[0]) ? "An" : "an";
                return `${an} ${captures[1]}`;
            },
            message: ({ captures }) => {
                const result = classifyArticle(captures[1], classifyOptions);
                return `Should be begin with "an"` + "\nReason: " + result.reason + "\n" + DocumentURL;
            }
        },
        {
            pattern: /\b(an) ([\w.-]+)\b/i,
            test: ({ captures }) => {
                const result = classifyArticle(captures[1], classifyOptions);
                return result.type === "a";
            },
            replace: ({ captures }) => {
                const a = isCapital(captures[0]) ? "A" : "a";
                return `${a} ${captures[1]}`;
            },
            message: ({ captures }) => {
                const result = classifyArticle(captures[1], classifyOptions);
                return `Should be begin with "a"` + "\nReason: " + result.reason + "\n" + DocumentURL;
            }
        }
    ];
    const { Syntax, RuleError, getSource, fixer, report } = context;
    return {
        [Syntax.Str](node) {
            if (shouldIgnoreNodeOfStrNode(node, context)) {
                return;
            }
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
