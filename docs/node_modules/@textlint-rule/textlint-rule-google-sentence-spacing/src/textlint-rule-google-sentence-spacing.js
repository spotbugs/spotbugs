// MIT © 2017 azu
"use strict";

const { RuleHelper, IgnoreNodeManager } = require("textlint-rule-helper");
const StringSource = require("textlint-util-to-string");
const { split, Syntax: SentenceSyntax } = require("sentence-splitter");
const DocumentURL = "https://developers.google.com/style/sentence-spacing";
const report = context => {
    const { Syntax, RuleError, fixer, report } = context;
    const helper = new RuleHelper(context);
    // Ignore following pattern
    // Paragraph > Link Code Html ...
    return {
        [Syntax.Paragraph](node) {
            if (helper.isChildNode(node, [Syntax.Image, Syntax.BlockQuote, Syntax.Emphasis])) {
                return;
            }
            const ignoreNodeManager = new IgnoreNodeManager();
            ignoreNodeManager.ignoreChildrenByTypes(node, [Syntax.Code, Syntax.Link, Syntax.BlockQuote, Syntax.Html]);
            const source = new StringSource(node);
            const sourceText = source.toString();
            const sentences = split(sourceText);
            /**
             * @type {[{index:number,indent:number]}
             */
            const spaces = [
                {
                    index: 0,
                    indent: 0
                }
            ];
            const getSpace = () => {
                return spaces[spaces.length - 1];
            };
            /**
             * create and set next space
             * @param {number} index next start index
             */
            const nextSpace = index => {
                spaces.push({
                    index,
                    indent: 0
                });
            };
            const setSpace = value => {
                spaces[spaces.length - 1] = value;
            };
            const incrementCurrentSpace = () => {
                const space = getSpace();
                setSpace({
                    index: space.index,
                    indent: space.indent + 1
                });
            };
            // counting
            sentences.forEach(sentenceOrWhiteSpace => {
                if (sentenceOrWhiteSpace.type === SentenceSyntax.WhiteSpace && sentenceOrWhiteSpace.value === " ") {
                    return incrementCurrentSpace();
                }
                nextSpace(sentenceOrWhiteSpace.range[1]);
            });

            // Report based on space
            spaces
                .filter(space => space.indent >= 2)
                .filter(space => {
                    // Allow to write first space and last space.
                    // This rule only treat "space between sentences"
                    const isFirstSpace = space.index === 0;
                    const isLastSpace = space.index + space.indent === sentences[sentences.length - 1].range[1];
                    if (isFirstSpace) {
                        return false;
                    } else if (isLastSpace) {
                        return false;
                    }
                    const originalIndex = source.originalIndexFromIndex(space.index);
                    // if the error is ignored, don't report
                    if (ignoreNodeManager.isIgnoredIndex(originalIndex)) {
                        return false;
                    }
                    // other should report
                    return true;
                })
                .forEach(space => {
                    const originalIndex = source.originalIndexFromIndex(space.index);
                    const message = `Leave only one space between sentences. Number of space: ${space.indent}
${DocumentURL}`;
                    report(
                        node,
                        new RuleError(message, {
                            index: originalIndex,
                            fix: fixer.replaceTextRange([originalIndex, originalIndex + space.indent], " ")
                        })
                    );
                });
        }
    };
};
module.exports = {
    linter: report,
    fixer: report
};
