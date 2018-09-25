// LICENSE : MIT
"use strict";
import { EventEmitter } from "events";

const interopRequire = require("interop-require");
const debug = require("debug")("textlint:module-loader");
const isFile = require("is-file");
import { isPluginRuleKey } from "../util/config-util";
import { loadFromDir } from "./rule-loader";
import { Logger } from "../util/logger";
import { TextLintModuleResolver } from "./textlint-module-resolver";
import { TextLintModuleMapper } from "./textlint-module-mapper";
import { Config } from "../config/config";

export class TextLintModuleLoader extends EventEmitter {
    moduleResolver: TextLintModuleResolver;
    config: any;

    static get Event() {
        return {
            rule: "rule",
            filterRule: "filterRule",
            plugin: "plugin",
            error: "error"
        };
    }

    constructor(config: Config) {
        super();
        /**
         * @type {Config} config is need for static prefix value
         */
        this.config = config;
        /**
         * @type {TextLintModuleResolver}
         */
        this.moduleResolver = new TextLintModuleResolver(this.config.constructor, this.config.rulesBaseDirectory);
    }

    /**
     * set up lint rules using {@lint Config} object.
     * The {@lint Config} object was created with initialized {@link TextLintEngine} (as-known Constructor).
     * @param {Config} config the config is parsed object
     */
    loadFromConfig(config: Config) {
        debug("config %O", config);
        // --ruledir
        if (config.rulePaths) {
            // load in additional rules
            config.rulePaths.forEach(rulesDir => {
                debug("Loading rules from %o", rulesDir);
                const rules = loadFromDir(rulesDir);
                Object.keys(rules).forEach(ruleName => {
                    const entry = [ruleName, rules[ruleName]];
                    this.emit(TextLintModuleLoader.Event.rule, entry);
                });
            });
        }
        // --rule
        if (config.rules) {
            // load in additional rules
            config.rules.forEach((ruleName: string) => {
                this.loadRule(ruleName);
            });
        }
        // TODO: --filter
        if (config.filterRules) {
            // load in additional filterRules
            config.filterRules.forEach(ruleName => {
                this.loadFilterRule(ruleName);
            });
        }
        // --preset
        if (config.presets) {
            config.presets.forEach(presetName => {
                this.loadPreset(presetName);
            });
        }
        // --plugin
        if (config.plugins) {
            // load in additional rules from plugin
            config.plugins.forEach(pluginName => {
                this.loadPlugin(pluginName);
            });
        }
    }

    /**
     * load rule from plugin name.
     * plugin module has `rules` object and define rule with plugin prefix.
     * @param {string} pluginName
     */
    loadPlugin(pluginName: string) {
        const pkgPath = this.moduleResolver.resolvePluginPackageName(pluginName);
        debug("Loading rules from plugin: %s", pkgPath);
        const plugin = interopRequire(pkgPath);
        const PLUGIN_NAME_PREFIX = this.config.constructor.PLUGIN_NAME_PREFIX;
        const prefixMatch = new RegExp(`^${PLUGIN_NAME_PREFIX}`);
        const pluginNameWithoutPrefix = pluginName.replace(prefixMatch, "");
        // Notes: plugins not support "rules" and "rulesConfig"
        // https://github.com/textlint/textlint/issues/291
        if (plugin.hasOwnProperty("rules")) {
            throw new Error(`textlint plugins not support "rules" and "rulesConfig".
But ${pluginName} has these filed.
For more details, See https://github.com/textlint/textlint/issues/291`);
        }
        // register plugin.Processor
        if (!plugin.hasOwnProperty("Processor")) {
            throw new Error(`textlint plugin should have "Processor".
For more details. See https://github.com/textlint/textlint/blob/master/docs/plugin.md`);
        }
        const pluginEntry = [pluginNameWithoutPrefix, plugin];
        this.emit(TextLintModuleLoader.Event.plugin, pluginEntry);
    }

    loadPreset(presetName: string) {
        /*
         Caution: Rules of preset are defined as following.
             {
                "rules": {
                    "preset-gizmo": {
                        "ruleA": false

                }
            }

        It mean that "ruleA" is defined as "preset-gizmo/ruleA"

         */
        const RULE_NAME_PREFIX = this.config.constructor.RULE_NAME_PREFIX;
        // Strip **rule** prefix
        // textlint-rule-preset-gizmo -> preset-gizmo
        const prefixMatch = new RegExp(`^${RULE_NAME_PREFIX}`);
        const presetRuleNameWithoutPrefix = presetName.replace(prefixMatch, "");
        // ignore plugin's rule
        if (isPluginRuleKey(presetRuleNameWithoutPrefix)) {
            Logger.warn(`${presetRuleNameWithoutPrefix} is Plugin's rule. This is unknown case, please report issue.`);
            return;
        }

        const pkgPath = this.moduleResolver.resolvePresetPackageName(presetName);
        debug("Loading rules from preset: %s", pkgPath);
        const preset = interopRequire(pkgPath);
        const entities = TextLintModuleMapper.createEntities(preset.rules, presetRuleNameWithoutPrefix);
        entities.forEach(entry => {
            this.emit(TextLintModuleLoader.Event.rule, entry);
        });
    }

    /**
     * load rule file with `ruleName` and define rule.
     * if rule is not found, then throw ReferenceError.
     * if already rule is loaded, do not anything.
     * @param {string} ruleName
     */
    loadRule(ruleName: string) {
        /*
           Task
             - check already define
             - resolve package name
             - load package
             - emit rule
        */
        // ruleName is filePath
        if (isFile(ruleName)) {
            const ruleCreator = interopRequire(ruleName);
            const ruleEntry = [ruleName, ruleCreator];
            this.emit(TextLintModuleLoader.Event.rule, ruleEntry);
            return;
        }
        // ignore already defined rule
        // ignore rules from rulePaths because avoid ReferenceError is that try to require.
        const RULE_NAME_PREFIX = this.config.constructor.RULE_NAME_PREFIX;
        const prefixMatch = new RegExp(`^${RULE_NAME_PREFIX}`);
        const definedRuleName = ruleName.replace(prefixMatch, "");
        // ignore plugin's rule
        if (isPluginRuleKey(definedRuleName)) {
            Logger.warn(`${definedRuleName} is Plugin's rule. This is unknown case, please report issue.`);
            return;
        }
        const pkgPath = this.moduleResolver.resolveRulePackageName(ruleName);
        debug("Loading rules from %s", pkgPath);
        const ruleCreator = interopRequire(pkgPath);
        const ruleEntry = [definedRuleName, ruleCreator];
        this.emit(TextLintModuleLoader.Event.rule, ruleEntry);
    }

    /**
     * load filter rule file with `ruleName` and define rule.
     * if rule is not found, then throw ReferenceError.
     * if already rule is loaded, do not anything.
     * @param {string} ruleName
     */
    loadFilterRule(ruleName: string) {
        /*
           Task
             - check already define
             - resolve package name
             - load package
             - emit rule
        */
        // ignore already defined rule
        // ignore rules from rulePaths because avoid ReferenceError is that try to require.
        if (isFile(ruleName)) {
            const ruleCreator = interopRequire(ruleName);
            const ruleEntry = [ruleName, ruleCreator];
            this.emit(TextLintModuleLoader.Event.filterRule, ruleEntry);
            return;
        }
        const RULE_NAME_PREFIX = this.config.constructor.FILTER_RULE_NAME_PREFIX;
        const prefixMatch = new RegExp(`^${RULE_NAME_PREFIX}`);
        const definedRuleName = ruleName.replace(prefixMatch, "");
        // ignore plugin's rule
        if (isPluginRuleKey(definedRuleName)) {
            Logger.warn(`${definedRuleName} is Plugin's rule. This is unknown case, please report issue.`);
            return;
        }
        const pkgPath = this.moduleResolver.resolveFilterRulePackageName(ruleName);
        debug("Loading filter rules from %s", pkgPath);
        const ruleCreator = interopRequire(pkgPath);
        const ruleEntry = [definedRuleName, ruleCreator];
        this.emit(TextLintModuleLoader.Event.filterRule, ruleEntry);
    }
}
