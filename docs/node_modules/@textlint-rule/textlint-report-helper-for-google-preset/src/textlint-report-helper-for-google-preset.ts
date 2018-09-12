// MIT © 2017 azu
import { matchTestReplace, TestMatchReplaceReturnDict } from "match-test-replace";
import { ASTNodeTypes } from "@textlint/ast-node-types";

const { RuleHelper, IgnoreNodeManager } = require("textlint-rule-helper");
const StringSource = require("textlint-util-to-string");

export { getPos, getPosFromSingleWord, PosType, isSameGroupPosType } from "./en-pos-util";
// str
export const shouldIgnoreNodeOfStrNode = (node: any, context: any) => {
    const helper = new RuleHelper(context);
    const Syntax = context.Syntax;
    return helper.isChildNode(node, [Syntax.Link, Syntax.Image, Syntax.BlockQuote, Syntax.Emphasis]);
};

export interface StrReporterArgs {
    node: any;
    dictionaries: TestMatchReplaceReturnDict[];
    report: (node: any, message: any) => void;
    RuleError: any;
    fixer: any;
    getSource: (node: any, beforeCount?: number, afterCount?: number) => string;
}

export const strReporter = ({ node, dictionaries, report, RuleError, fixer, getSource }: StrReporterArgs) => {
    const text = getSource(node);
    dictionaries.forEach(dict => {
        const matchTestReplaceReturn = matchTestReplace(text, dict);
        if (matchTestReplaceReturn.ok === false) {
            return;
        }
        matchTestReplaceReturn.results.forEach(result => {
            const index = result.index;
            if (!result.replace) {
                report(
                    node,
                    new RuleError(result.message, {
                        index
                    })
                );
                return;
            }
            const endIndex = result.index + result.match.length;
            const range = [index, endIndex];
            report(
                node,
                new RuleError(result.message, {
                    index,
                    fix: fixer.replaceTextRange(range, result.replace)
                })
            );
        });
    });
};

export interface ParagraphReporterArgs {
    Syntax: typeof ASTNodeTypes;
    node: any;
    dictionaries: TestMatchReplaceReturnDict[];
    report: (node: any, message: any) => void;
    RuleError: any;
    fixer: any;
    getSource: (node: any, beforeCount?: number, afterCount?: number) => string;
}

export const paragraphReporter = ({
    Syntax,
    node,
    dictionaries,
    getSource,
    report,
    RuleError,
    fixer
}: ParagraphReporterArgs) => {
    const originalText = getSource(node);
    const source = new StringSource(node);
    const text = source.toString();
    const ignoreNodeManager = new IgnoreNodeManager();
    // Ignore following pattern
    // Paragraph > Link Code Html ...
    ignoreNodeManager.ignoreChildrenByTypes(node, [Syntax.Code, Syntax.Link, Syntax.BlockQuote, Syntax.Html]);
    dictionaries.forEach(dict => {
        const matchTestReplaceReturn = matchTestReplace(text, dict);
        if (matchTestReplaceReturn.ok === false) {
            return;
        }
        matchTestReplaceReturn.results.forEach(result => {
            // relative index
            const indexFromNode = source.originalIndexFromIndex(result.index);
            const endIndexFromNode = source.originalIndexFromIndex(result.index + result.match.length);
            const rangeFromNode = [indexFromNode, endIndexFromNode];
            // absolute index
            const absoluteRange = [node.range[0] + rangeFromNode[0], node.range[1] + rangeFromNode[1]];
            // if the error is ignored, don't report
            if (ignoreNodeManager.isIgnoredRange(absoluteRange)) {
                return;
            }
            if (!result.replace) {
                report(
                    node,
                    new RuleError(result.message, {
                        index: indexFromNode
                    })
                );
                return;
            }
            const beforeText = originalText.slice(indexFromNode, endIndexFromNode);
            if (beforeText !== result.match) {
                report(
                    node,
                    new RuleError(result.message, {
                        index: indexFromNode
                    })
                );
                return;
            }
            report(
                node,
                new RuleError(result.message, {
                    index: indexFromNode,
                    fix: fixer.replaceTextRange(rangeFromNode, result.replace)
                })
            );
        });
    });
};
