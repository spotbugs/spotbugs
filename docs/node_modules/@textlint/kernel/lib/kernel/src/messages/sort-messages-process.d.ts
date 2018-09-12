import { TextlintMessage } from "../textlint-kernel-interface";
/**
 * sort messages by line and column
 * @param {TextlintMessage[]} messages
 * @returns {TextlintMessage[]}
 */
export default function sortByLineColumn(messages: TextlintMessage[]): TextlintMessage[];
