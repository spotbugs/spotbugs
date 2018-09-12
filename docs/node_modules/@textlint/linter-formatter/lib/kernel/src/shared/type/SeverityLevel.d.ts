/**
 * Severity Level list
 * It is used in configuration and message
 */
declare const SeverityLevel: {
    none: number;
    info: number;
    warning: number;
    error: number;
};
export default SeverityLevel;
export declare type SeverityLevelTypes = keyof typeof SeverityLevel;
