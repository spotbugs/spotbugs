import { TextlintFixCommand, TextlintMessage } from "../textlint-kernel-interface";

const debug = require("debug")("textlint:source-code-fixer");
import SourceCode from "../core/source-code";

const BOM = "\uFEFF";

/**
 * Compares items in a messages array by line and column.
 * @param {TextlintMessage} a The first message.
 * @param {TextlintMessage} b The second message.
 * @returns {int} -1 if a comes before b, 1 if a comes after b, 0 if equal.
 * @private
 */
function compareMessagesByLocation(a: TextlintMessage, b: TextlintMessage) {
    const lineDiff = a.line - b.line;

    if (lineDiff === 0) {
        return a.column - b.column;
    } else {
        return lineDiff;
    }
}

function clone(object: any) {
    return JSON.parse(JSON.stringify(object));
}

export interface TextLintMessageFixable extends TextlintMessage {
    fix: TextlintFixCommand;
}

/**
 * Utility for apply fixes to source code.
 * @constructor
 */

export default class SourceCodeFixer {
    /**
     * Applies the fixes specified by the messages to the given text. Tries to be
     * smart about the fixes and won't apply fixes over the same area in the text.
     * @param {SourceCode} sourceCode The source code to apply the changes to.
     * @param {TextlintMessage[]} messages The array of messages reported by ESLint.
     * @returns {Object} An object containing the fixed text and any unfixed messages.
     */
    static applyFixes(sourceCode: SourceCode, messages: TextlintMessage[]) {
        debug("Applying fixes");
        const text = sourceCode.text;
        // As as result, show diff
        const remainingMessages: TextlintMessage[] = [];
        const applyingMessages: TextlintMessage[] = [];
        const cloneMessages = messages.slice();
        const fixes: TextLintMessageFixable[] = [];
        let lastFixPos = text.length + 1;
        let prefix = sourceCode.hasBOM ? BOM : "";
        cloneMessages.forEach(problem => {
            if (problem && problem.fix !== undefined) {
                fixes.push(problem as TextLintMessageFixable);
            } else {
                remainingMessages.push(problem);
            }
        });

        if (fixes.length) {
            debug("Found fixes to apply");

            // sort in reverse order of occurrence
            // FIXME: always has `fix`
            fixes.sort((a: TextLintMessageFixable, b: TextLintMessageFixable) => {
                if (a.fix.range[1] <= b.fix.range[0]) {
                    return 1;
                } else {
                    return -1;
                }
            });

            // split into array of characters for easier manipulation
            const chars = text.split("");

            fixes.forEach(problem => {
                // pickup fix range
                const fix = problem.fix;
                let start = fix.range[0];
                const end = fix.range[1];
                let insertionText = fix.text;

                if (end < lastFixPos) {
                    if (start < 0) {
                        // Remove BOM.
                        prefix = "";
                        start = 0;
                    }
                    if (start === 0 && insertionText[0] === BOM) {
                        // Set BOM.
                        prefix = BOM;
                        insertionText = insertionText.slice(1);
                    }

                    const replacedChars = chars.splice(start, end - start, insertionText);
                    lastFixPos = start;
                    const copyOfMessage = clone(problem);
                    copyOfMessage.fix = {
                        range: [start, start + insertionText.length],
                        text: replacedChars.join("")
                    };
                    applyingMessages.push(copyOfMessage);
                } else {
                    remainingMessages.push(problem);
                }
            });

            return {
                fixed: true,
                messages: cloneMessages, // have order
                applyingMessages: applyingMessages.reverse(), // have order
                remainingMessages: remainingMessages.sort(compareMessagesByLocation), // have not order
                output: prefix + chars.join("")
            };
        } else {
            debug("No fixes to apply");
            return {
                fixed: false,
                messages: cloneMessages,
                applyingMessages,
                remainingMessages,
                output: prefix + text
            };
        }
    }

    /**
     * Sequentially Applies the fixes specified by the messages to the given text.
     * @param {SourceCode} sourceCode The source code to apply the changes to.
     * @param {TextlintMessage[]} applyingMessages The array of TextLintMessage reported by SourceCodeFixer#applyFixes
     * @returns {string} An object containing the fixed text and any unfixed messages.
     */
    static sequentiallyApplyFixes(sourceCode: SourceCode, applyingMessages: TextlintMessage[]) {
        debug("Restore applied fixes");
        let text = sourceCode.text;
        applyingMessages.forEach(message => {
            const newSource = new SourceCode({
                text,
                ast: sourceCode.ast, // it's dummy
                ext: sourceCode.ext,
                filePath: sourceCode.filePath
            });
            const result = SourceCodeFixer.applyFixes(newSource, [message]);
            text = result.output;
        });
        return text;
    }
}
