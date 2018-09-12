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
        // word(s)
        // if "words" is plural, report as error
        {
            pattern: /([\w-]+)\((\w+)\)/g,
            test: ({ captures }) => {
                const pluralWord = `${captures[0]}${captures[1]}`;
                const pos = getPosFromSingleWord(pluralWord);
                return pos === PosType.PluralNoun || pos === PosType.PluralProperNoun;
            },
            message: () => ` Don't put optional plurals in parentheses.
${DocumentURL}
`
        }
    ];

    const { Syntax, RuleError, getSource, fixer, report } = context;
    return {
        [Syntax.Paragraph](node) {
            return paragraphReporter({
                node,
                Syntax,
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
