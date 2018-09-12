export interface RuleOf {
    presets: string[];
    available: string[];
    disable: string[];
}
/**
 * Get rule keys from `.textlintrc` config object.
 * @param {Object} [rulesConfig]
 * @returns {{available: string[], disable: string[]}}
 */
export declare function separateAvailableOrDisable(rulesConfig: any): RuleOf;
