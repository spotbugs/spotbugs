// LICENSE : MIT
"use strict";
const objectAssign = require("object-assign");
const md5 = require("md5");
const fs = require("fs");
const assert = require("assert");
const concat = require("unique-concat");
const path = require("path");
import { loadConfig } from "./config-loader";
import { isPresetRuleKey } from "../util/config-util";
import { loadRulesConfigFromPresets, mapRulesConfig } from "./preset-loader";
import { getPluginConfig, getPluginNames } from "./plugin-loader";
import { TextLintModuleResolver } from "../engine/textlint-module-resolver";
import { separateAvailableOrDisable } from "./separate-by-config-option";

/**
 * Convert config of preset to rulesConfig flat path format.
 *
 * e.g.)
 * {
 *  "preset-a" : { "key": "value"}
 * }
 * => {"preset-a/key": "value"}
 *
 * @param rulesConfig
 * @returns {{string: string}}
 */
function convertRulesConfigToFlatPath(rulesConfig: any) {
    if (!rulesConfig) {
        return {};
    }
    const filteredConfig: { [index: string]: any } = {};
    Object.keys(rulesConfig).forEach(key => {
        if (isPresetRuleKey(key)) {
            // <preset>/<rule>
            objectAssign(filteredConfig, mapRulesConfig(rulesConfig[key], key));
            return;
        }
        filteredConfig[key] = rulesConfig[key];
    });
    return filteredConfig;
}

/**
 * @type {TextlintConfig}
 */
const defaultOptions = Object.freeze({
    // rule package names
    rules: [],
    // disabled rule package names
    // always should start with empty
    disabledRules: [],
    // rules config object
    rulesConfig: {},
    // filter rule package names
    filterRules: [],
    disabledFilterRules: [],
    // rules config object
    filterRulesConfig: {},
    // preset package names
    // e.g.) ["preset-foo"]
    presets: [],
    // plugin package names
    plugins: [],
    // plugin config
    pluginsConfig: {},
    // base directory for loading {rule, config, plugin} modules
    rulesBaseDirectory: undefined,
    // ".textlint" file path
    configFile: undefined,
    // rule directories
    rulePaths: [],
    // formatter file name
    // e.g.) stylish.js => set "stylish"
    // NOTE: default formatter is defined in Engine,
    // because There is difference between TextLintEngine and TextFixEngine.
    formatterName: undefined,
    // --quiet
    quiet: false,
    // --no-color
    color: true,
    // --no-textlintrc
    textlintrc: true,
    // --cache : enable or disable
    cache: false,
    // --cache-location: cache file path
    cacheLocation: path.resolve(process.cwd(), ".textlintcache")
});

export interface ConfigStatics {
    CONFIG_PACKAGE_PREFIX: string;
    FILTER_RULE_NAME_PREFIX: string;
    RULE_NAME_PREFIX: string;
    RULE_PRESET_NAME_PREFIX: string;
    PLUGIN_NAME_PREFIX: string;
}

// Priority: CLI > Code options > config file
export class Config {
    rules: any;
    rulesBaseDirectory: string | undefined;
    configFile: string | undefined;
    disabledRules: string[];
    filterRules: string[];
    disabledFilterRules: string[];
    presets: string[];
    plugins: string[];
    pluginsConfig: { [index: string]: any };
    rulesConfig: { [index: string]: any };
    filterRulesConfig: { [index: string]: any };
    rulePaths: string[];
    formatterName: string | undefined;
    quiet: boolean;
    color: boolean;
    cache: boolean;
    cacheLocation: string;

    /**
     * @return {string} rc config filename
     * it's name use as `.<name>rc`
     */
    static get CONFIG_FILE_NAME() {
        return "textlint";
    }

    /**
     * @return {string} config package prefix
     */
    static get CONFIG_PACKAGE_PREFIX() {
        return "textlint-config-";
    }

    /**
     * @return {string} rule package's name prefix
     */
    static get RULE_NAME_PREFIX() {
        return "textlint-rule-";
    }

    /**
     * @return {string} filter rule package's name prefix
     */
    static get FILTER_RULE_NAME_PREFIX() {
        return "textlint-filter-rule-";
    }

    /**
     * @return {string} rule preset package's name prefix
     */
    static get RULE_PRESET_NAME_PREFIX() {
        return "textlint-rule-preset-";
    }

    /**
     * @return {string} plugins package's name prefix
     */
    static get PLUGIN_NAME_PREFIX() {
        return "textlint-plugin-";
    }

    /**
     * Create config object form command line options
     * See options.js
     * @param {object} cliOptions the options is command line option object. @see options.js
     * @returns {Config}
     */
    static initWithCLIOptions(cliOptions: any) {
        const options: { [index: string]: any } = {};
        options.rules = cliOptions.rule ? cliOptions.rule : defaultOptions.rules;
        // TODO: CLI --filter <rule>?
        options.filterRules = defaultOptions.filterRules;
        options.disabledFilterRules = defaultOptions.disabledFilterRules;
        // TODO: CLI --disable <rule>?
        options.disabledRules = defaultOptions.disabledRules;
        options.presets = cliOptions.preset ? cliOptions.preset : defaultOptions.presets;
        options.plugins = cliOptions.plugin ? cliOptions.plugin : defaultOptions.plugins;
        options.configFile = cliOptions.config ? cliOptions.config : defaultOptions.configFile;
        options.rulePaths = cliOptions.rulesdir ? cliOptions.rulesdir : defaultOptions.rulePaths;
        options.formatterName = cliOptions.format ? cliOptions.format : defaultOptions.formatterName;
        options.quiet = cliOptions.quiet !== undefined ? cliOptions.quiet : defaultOptions.quiet;
        options.color = cliOptions.color !== undefined ? cliOptions.color : defaultOptions.color;
        options.textlintrc = cliOptions.textlintrc !== undefined ? cliOptions.textlintrc : defaultOptions.textlintrc;
        // --cache
        options.cache = cliOptions.cache !== undefined ? cliOptions.cache : defaultOptions.cache;
        // --cache-location="path/to/file"
        options.cacheLocation =
            cliOptions.cacheLocation !== undefined
                ? path.resolve(process.cwd(), cliOptions.cacheLocation)
                : defaultOptions.cacheLocation;
        // --rules-base-directory "other/node_modules"
        options.rulesBaseDirectory = cliOptions.rulesBaseDirectory || defaultOptions.rulesBaseDirectory;
        return this.initWithAutoLoading(options);
    }

    /* eslint-disable complexity */

    // load config and merge options.
    static initWithAutoLoading(options: any = {}) {
        // Base directory
        const rulesBaseDirectory = options.rulesBaseDirectory
            ? options.rulesBaseDirectory
            : defaultOptions.rulesBaseDirectory;
        // Create resolver
        const moduleResolver = new TextLintModuleResolver(this, rulesBaseDirectory);
        // => ConfigFile
        // configFile is optional
        // => load .textlintrc
        const loadedResult =
            typeof options.textlintrc === "undefined" || options.textlintrc
                ? loadConfig(options.configFile, {
                      moduleResolver,
                      configFileName: this.CONFIG_FILE_NAME
                  })
                : {
                      config: {},
                      filePath: undefined
                  };
        const configFileRaw = loadedResult.config;
        const configFilePath = loadedResult.filePath;
        // => Load options from .textlintrc
        const configRulesObject = separateAvailableOrDisable(configFileRaw.rules);
        const configFilterRulesObject = separateAvailableOrDisable(configFileRaw.filters);
        const configPresets = configRulesObject.presets;
        const configFilePlugins = getPluginNames(configFileRaw);
        const configFilePluginConfig = getPluginConfig(configFileRaw);
        const configFileRulesConfig = convertRulesConfigToFlatPath(configFileRaw.rules);
        const configFileFilterRulesConfig = convertRulesConfigToFlatPath(configFileRaw.filters);
        // => User specified Options
        const optionRules = options.rules || [];
        const optionFilterRules = options.filterRules || [];
        const optionDisabledRules = options.disabledRules || [];
        const optionDisabledFilterRules = options.disabledFilterRules || [];
        const optionRulesConfig = options.rulesConfig || {};
        const optionFilterRulesConfig = options.filterRulesConfig || {};
        const optionPlugins = options.plugins || [];
        const optionPresets = options.presets || [];
        const optionPluginsConfig = options.pluginsConfig || {};
        // => Merge options and configFileOptions
        // Priority options > configFile
        const rules = concat(optionRules, configRulesObject.available);
        const disabledRules = concat(optionDisabledRules, configRulesObject.disable);
        const filterRules = concat(optionFilterRules, configFilterRulesObject.available);
        const disabledFilterRules = concat(optionDisabledFilterRules, configFilterRulesObject.disable);
        const rulesConfig = objectAssign({}, configFileRulesConfig, optionRulesConfig);
        const filterRulesConfig = objectAssign({}, configFileFilterRulesConfig, optionFilterRulesConfig);
        const plugins = concat(optionPlugins, configFilePlugins);
        const pluginsConfig = objectAssign({}, configFilePluginConfig, optionPluginsConfig);
        const presets = concat(optionPresets, configPresets);
        const mergedOptions = objectAssign({}, options, {
            rules,
            disabledRules,
            rulesConfig,
            filterRules,
            disabledFilterRules,
            filterRulesConfig,
            plugins,
            pluginsConfig,
            presets,
            configFile: configFilePath
        });
        return new this(mergedOptions);
    }

    /**
     * Return hash string of the config and textlint version
     * @returns {string}
     */
    get hash() {
        const pkgConf = require("read-pkg-up");
        const version = pkgConf.sync({ cwd: __dirname }).pkg.version;
        const toString = JSON.stringify(this.toJSON());
        return md5(`${version}-${toString}`);
    }

    /**
     * initialize with options.
     * @param {TextlintConfig} options the option object is defined as TextlintConfig.
     * @returns {Config}
     * @constructor
     */
    constructor(options: Partial<Config> = {}) {
        /**
         * @type {string|undefined} absolute path to .textlintrc file.
         * - If using .textlintrc, return path to .textlintrc
         * - If using npm config module, return path to main file of the module
         * - If not using config file, return undefined
         */
        this.configFile = options.configFile;
        if (this.configFile) {
            assert(path.isAbsolute(this.configFile), `configFile should be absolute path: ${this.configFile}`);
        }
        this.rulesBaseDirectory = options.rulesBaseDirectory
            ? options.rulesBaseDirectory
            : defaultOptions.rulesBaseDirectory;
        // rule names that are defined in ,textlintrc
        const configConstructor: ConfigStatics = (this.constructor as any) as ConfigStatics;
        const moduleResolver = new TextLintModuleResolver(
            {
                CONFIG_PACKAGE_PREFIX: configConstructor.CONFIG_PACKAGE_PREFIX,
                FILTER_RULE_NAME_PREFIX: configConstructor.FILTER_RULE_NAME_PREFIX,
                RULE_NAME_PREFIX: configConstructor.RULE_NAME_PREFIX,
                RULE_PRESET_NAME_PREFIX: configConstructor.RULE_PRESET_NAME_PREFIX,
                PLUGIN_NAME_PREFIX: configConstructor.PLUGIN_NAME_PREFIX
            },
            this.rulesBaseDirectory
        );
        /**
         * @type {string[]} rule key list
         * but, plugins's rules are not contained in `rules`
         * plugins's rule are loaded in TextLintEngine
         */
        this.rules = options.rules ? options.rules : defaultOptions.rules;
        /**
         * @type {string[]} rule key list
         * These rule is set `false` to options
         */
        this.disabledRules = options.disabledRules ? options.disabledRules : defaultOptions.disabledRules;
        /**
         * @type {string[]} filter rule key list
         */
        this.filterRules = options.filterRules ? options.filterRules : defaultOptions.filterRules;
        /**
         * @type {string[]} rule key list
         * These rule is set `false` to options
         */
        this.disabledFilterRules = options.disabledFilterRules
            ? options.disabledFilterRules
            : defaultOptions.disabledFilterRules;
        /**
         * @type {string[]} preset key list
         */
        this.presets = options.presets ? options.presets : defaultOptions.presets;
        // => load plugins
        // this.rules has not contain plugin rules
        // =====================
        this.plugins = options.plugins ? options.plugins : defaultOptions.plugins;
        this.pluginsConfig = options.pluginsConfig ? options.pluginsConfig : defaultOptions.pluginsConfig;
        // rulesConfig
        const presetRulesConfig = loadRulesConfigFromPresets(this.presets, moduleResolver);
        this.rulesConfig = objectAssign({}, presetRulesConfig, options.rulesConfig);
        // filterRulesConfig
        this.filterRulesConfig = options.filterRulesConfig || defaultOptions.filterRulesConfig;
        /**
         * @type {string[]}
         */
        this.rulePaths = options.rulePaths ? options.rulePaths : defaultOptions.rulePaths;
        /**
         * @type {string}
         */
        this.formatterName = options.formatterName ? options.formatterName : defaultOptions.formatterName;
        /**
         * @type {boolean}
         */
        this.quiet = options.quiet !== undefined ? options.quiet : defaultOptions.quiet;
        /**
         * @type {boolean}
         */
        this.color = options.color !== undefined ? options.color : defaultOptions.color;
        /**
         * @type {boolean}
         */
        this.cache = options.cache !== undefined ? options.cache : defaultOptions.cache;
        /**
         * @type {string}
         */
        this.cacheLocation = options.cacheLocation !== undefined ? options.cacheLocation : defaultOptions.cacheLocation;
        this._assertCacheLocation(this.cacheLocation);
    }

    _assertCacheLocation(locationPath: string) {
        let fileStats;
        try {
            fileStats = fs.lstatSync(locationPath);
        } catch (ex) {
            fileStats = null;
        }
        if (!fileStats) {
            return;
        }
        // TODO: --cache-location not supported directory
        // We should defined what is default name.
        assert(!fileStats.isDirectory(), "--cache-location doesn't support directory");
    }

    /* eslint-enable complexity */

    toJSON() {
        const r = Object.create(null);
        Object.keys(this).forEach(key => {
            if (!this.hasOwnProperty(key)) {
                return;
            }
            const value = (this as any)[key];
            if (value == null) {
                return;
            }
            r[key] = typeof value.toJSON !== "undefined" ? value.toJSON() : value;
        });
        return r;
    }
}
