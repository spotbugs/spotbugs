import { TextlintMessage } from "../textlint-kernel-interface";
/**
 * filter duplicated messages
 * @param {TextlintMessage[]} messages
 * @returns {TextlintMessage[]} filtered messages
 */
export default function filterDuplicatedMessages(messages?: TextlintMessage[]): TextlintMessage[];
