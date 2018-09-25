import { TextLintModuleResolver } from "../engine/textlint-module-resolver";
/**
 * create `<plugin>/<rule>` option
 * @param {Object} [rulesConfig]
 * @param {string} presetName
 * @returns {Object}
 */
export declare function mapRulesConfig(rulesConfig: {
    [index: string]: string;
}, presetName: string): object;
/**
 *
 * @param ruleNames
 * @param {TextLintModuleResolver} moduleResolver
 * @returns {{}}
 */
export declare function loadRulesConfigFromPresets(ruleNames: string[] | undefined, moduleResolver: TextLintModuleResolver): {};
