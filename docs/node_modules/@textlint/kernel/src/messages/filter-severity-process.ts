// LICENSE : MIT
"use strict";
import SeverityLevel from "../shared/type/SeverityLevel";
import { TextlintKernelConstructorOptions, TextlintMessage } from "../textlint-kernel-interface";

/**
 * Filter messages by their severity.
 * @param {TextlintMessage[]} messages
 * @returns {TextlintMessage[]} filtered messages
 */
export function filterWarningMessages(messages: TextlintMessage[] = []) {
    return messages.filter(message => {
        return message.severity === SeverityLevel.error;
    });
}

/**
 * Pass through all messages.
 * @param {TextlintMessage[]} messages
 * @returns {TextlintMessage[]}
 */
export function through<T>(messages: T[] = []): T[] {
    return messages;
}

/**
 * Create message filter by config.quiet.
 * @param {Config} config
 * @returns {Function} filter function for messages
 */
export default function createSeverityFilter(
    config: TextlintKernelConstructorOptions
): (messages: TextlintMessage[]) => TextlintMessage[] {
    if (config.quiet) {
        return filterWarningMessages;
    } else {
        return through;
    }
}
