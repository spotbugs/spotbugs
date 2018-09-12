// LICENSE : MIT
"use strict";
import * as assert from "assert";
import SeverityLevel from "./type/SeverityLevel";
import { TextlintRuleOptions } from "../textlint-kernel-interface";

/**
 * get severity level from ruleConfig.
 * @param {Object|boolean|undefined} ruleConfig
 * @returns {number}
 */
export function getSeverity(ruleConfig?: TextlintRuleOptions) {
    if (ruleConfig === undefined) {
        return SeverityLevel.error;
    }
    // rule:<true|false>
    if (typeof ruleConfig === "boolean") {
        return ruleConfig ? SeverityLevel.error : SeverityLevel.none;
    }
    if (ruleConfig.severity) {
        assert(
            SeverityLevel[ruleConfig.severity] !== undefined,
            `please set
"rule-key": {
    "severity": "<warning|error>"
}`
        );
        return SeverityLevel[ruleConfig.severity];
    }
    return SeverityLevel.error;
}
