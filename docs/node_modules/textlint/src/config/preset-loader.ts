// LICENSE : MIT
"use strict";
const interopRequire = require("interop-require");
const ObjectAssign = require("object-assign");
import { TextLintModuleMapper } from "../engine/textlint-module-mapper";
import { TextLintModuleResolver } from "../engine/textlint-module-resolver";

/**
 * create `<plugin>/<rule>` option
 * @param {Object} [rulesConfig]
 * @param {string} presetName
 * @returns {Object}
 */
export function mapRulesConfig(rulesConfig: { [index: string]: string }, presetName: string): object {
    const mapped = {};
    // missing "rulesConfig"
    if (rulesConfig === undefined || typeof rulesConfig !== "object") {
        return mapped;
    }
    return TextLintModuleMapper.createMappedObject(rulesConfig, presetName);
}

// load rulesConfig from plugins
/**
 *
 * @param ruleNames
 * @param {TextLintModuleResolver} moduleResolver
 * @returns {{}}
 */
export function loadRulesConfigFromPresets(ruleNames: string[] = [], moduleResolver: TextLintModuleResolver): {} {
    const presetRulesConfig = {};
    ruleNames.forEach(ruleName => {
        const pkgPath = moduleResolver.resolvePresetPackageName(ruleName);
        const preset = interopRequire(pkgPath);
        if (!preset.hasOwnProperty("rules")) {
            throw new Error(`${ruleName} has not rules`);
        }
        if (!preset.hasOwnProperty("rulesConfig")) {
            throw new Error(`${ruleName} has not rulesConfig`);
        }
        // set config of <rule> to "<preset>/<rule>"
        ObjectAssign(presetRulesConfig, mapRulesConfig(preset.rulesConfig, ruleName));
    });
    return presetRulesConfig;
}
