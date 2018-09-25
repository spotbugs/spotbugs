// LICENSE : MIT
"use strict";
/**
 * Severity Level list
 * It is used in configuration and message
 */
const SeverityLevel = {
    none: 0,
    info: 0,
    warning: 1,
    error: 2
};
export default SeverityLevel;
export type SeverityLevelTypes = keyof typeof SeverityLevel;
