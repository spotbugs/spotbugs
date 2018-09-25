// MIT © 2017 azu
"use strict";
import {
    paragraphReporter,
    getPosFromSingleWord,
    PosType,
    isSameGroupPosType
} from "@textlint-rule/textlint-report-helper-for-google-preset";

const DocumentURL = "https://developers.google.com/style/hyphens";
const report = context => {
    const dictionaries = [
        // Serial commas
        {
            pattern: /([\w-]+), (.*?([\w-]+)) (and|or) ([\w-]+)/g,
            test: ({ captures }) => {
                const word1 = captures[0];
                const word2 = captures[2];
                const word3 = captures[4];
                const pos1 = getPosFromSingleWord(word1);
                const pos2 = getPosFromSingleWord(word2);
                const pos3 = getPosFromSingleWord(word3);
                // For example, word1-3 are attached to same group
                // Word1 is NN, Word2 is NNP, Word3 is NN => true
                return isSameGroupPosType(pos1, pos2) && isSameGroupPosType(pos2, pos3);
            },
            replace: ({ captures }) => {
                return `${captures[0]}, ${captures[1]}, ${captures[3]} ${captures[4]}`;
                //                                    ^ <= add ,
            },
            message: () => {
                return `In a series of three or more items, use a comma before the final "and" or "or.".
https://developers.google.com/style/commas#serial-commas
`;
            }
        },
        // Commas after introductory words and phrases
        // Commas separating two independent clauses
        // Steps:
        // 1. <first sentence> (and|but|nor|for|so|or|yet) <second sentence>
        // 2. <first sentence> and <second sentence> is complete statement.
        // 3. <first sentence> and <second sentence> is longer than 3 words
        // 4. Report Error and ","
        // {
        //     pattern: /^([^,]+) (and|but|nor|for|so|or|yet) ([^,]+)$/,
        //     test: ({ captures }) => {
        //         const prePhase = captures[0];
        //         const postPhase = captures[2];
        //     }
        // }
        // Commas separating independent from dependent clauses
        // Setting off other kinds of clauses
        {
            pattern: /^(however|otherwise|therefore) /i,
            replace: ({ captures }) => `${captures[0]}, `,
            message: () => {
                return `In general, put a semicolon or a period or a dash before a conjunctive adverb, such as "otherwise" or "however" or "therefore," and put a comma after the conjunctive adverb.
https://developers.google.com/style/commas#setting-off-other-kinds-of-clauses
`;
            }
        },
        {
            pattern: /([.;–]) (however|otherwise|therefore) /g,
            replace: ({ captures }) => `${captures[0]} ${captures[1]}, `,
            message: () => {
                return `In general, put a semicolon or a period or a dash before a conjunctive adverb, such as "otherwise" or "however" or "therefore," and put a comma after the conjunctive adverb.
https://developers.google.com/style/commas#setting-off-other-kinds-of-clauses
`;
            }
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
