// LICENSE : MIT
"use strict";
const assert = require("assert");
const path = require("path");
const tryResolve = require("try-resolve");
const debug = require("debug")("textlint:module-resolver");
const validateConfigConstructor = (ConfigConstructor: any) => {
    assert(
        ConfigConstructor.CONFIG_PACKAGE_PREFIX &&
            ConfigConstructor.FILTER_RULE_NAME_PREFIX &&
            ConfigConstructor.RULE_NAME_PREFIX &&
            ConfigConstructor.RULE_PRESET_NAME_PREFIX &&
            ConfigConstructor.PLUGIN_NAME_PREFIX
    );
};

/**
 * Create full package name and return
 * @param {string} prefix
 * @param {string} name
 * @returns {string}
 */
export const createFullPackageName = (prefix: string, name: string): string => {
    if (name.charAt(0) === "@") {
        const scopedPackageNameRegex = new RegExp(`^${prefix}(-|$)`);
        // if @scope/<name> -> @scope/<prefix><name>
        if (!scopedPackageNameRegex.test(name.split("/")[1])) {
            /*
             * for scoped packages, insert the textlint-rule after the first / unless
             * the path is already @scope/<name> or @scope/textlint-rule-<name>
             */
            return name.replace(/^@([^/]+)\/(.*)$/, `@$1/${prefix}$2`);
        }
    }
    return `${prefix}${name}`;
};

export interface ConfigModulePrefix {
    CONFIG_PACKAGE_PREFIX: string;
    FILTER_RULE_NAME_PREFIX: string;
    RULE_NAME_PREFIX: string;
    RULE_PRESET_NAME_PREFIX: string;
    PLUGIN_NAME_PREFIX: string;
}

/**
 * This class aim to resolve textlint's package name and get the module path.
 *
 * Define
 *
 * - `package` is npm package
 * - `module` is package's main module
 *
 * ## Support
 *
 * - textlint-rule-*
 * - textlint-preset-*
 * - textlint-plugin-*
 * - textlint-config-*
 */
export class TextLintModuleResolver {
    private baseDirectory: string;
    private PLUGIN_NAME_PREFIX: string;
    private RULE_PRESET_NAME_PREFIX: string;
    private FILTER_RULE_NAME_PREFIX: string;
    private RULE_NAME_PREFIX: string;
    private CONFIG_PACKAGE_PREFIX: string;
    /**
     *
     * @param {Config|*} ConfigConstructor config constructor like object
     * It has static property like CONFIG_PACKAGE_PREFIX etc...
     * @param {string} [baseDirectory]
     * @constructor
     */
    constructor(ConfigConstructor: ConfigModulePrefix, baseDirectory: string = "") {
        validateConfigConstructor(ConfigConstructor);
        /**
         * @type {string} config package prefix
         */
        this.CONFIG_PACKAGE_PREFIX = ConfigConstructor.CONFIG_PACKAGE_PREFIX;
        /**
         * @type {string} rule package's name prefix
         */
        this.RULE_NAME_PREFIX = ConfigConstructor.RULE_NAME_PREFIX;
        /**
         * @type {string} filter rule package's name prefix
         */
        this.FILTER_RULE_NAME_PREFIX = ConfigConstructor.FILTER_RULE_NAME_PREFIX;
        /**
         * @type {string} rule preset package's name prefix
         */
        this.RULE_PRESET_NAME_PREFIX = ConfigConstructor.RULE_PRESET_NAME_PREFIX;
        /**
         * @type {string} plugins package's name prefix
         */
        this.PLUGIN_NAME_PREFIX = ConfigConstructor.PLUGIN_NAME_PREFIX;
        /**
         * @type {string} baseDirectory for resolving
         */
        this.baseDirectory = baseDirectory;
    }

    /**
     * Take package name, and return path to module.
     * @param {string} packageName
     * @returns {string} return path to module
     */
    resolveRulePackageName(packageName: string): string {
        const baseDir = this.baseDirectory;
        const PREFIX = this.RULE_NAME_PREFIX;
        const fullPackageName = createFullPackageName(PREFIX, packageName);
        // <rule-name> or textlint-rule-<rule-name>
        const pkgPath = tryResolve(path.join(baseDir, fullPackageName)) || tryResolve(path.join(baseDir, packageName));
        if (!pkgPath) {
            debug(`rule fullPackageName: ${fullPackageName}`);
            throw new ReferenceError(`Failed to load textlint's rule module: "${packageName}" is not found.
See FAQ: https://github.com/textlint/textlint/blob/master/docs/faq/failed-to-load-textlints-module.md
`);
        }
        return pkgPath;
    }

    /**
     * Take package name, and return path to module.
     * @param {string} packageName
     * @returns {string} return path to module
     */
    resolveFilterRulePackageName(packageName: string): string {
        const baseDir = this.baseDirectory;
        const PREFIX = this.FILTER_RULE_NAME_PREFIX;
        const fullPackageName = createFullPackageName(PREFIX, packageName);
        // <rule-name> or textlint-filter-rule-<rule-name> or @scope/<rule-name>
        const pkgPath = tryResolve(path.join(baseDir, fullPackageName)) || tryResolve(path.join(baseDir, packageName));
        if (!pkgPath) {
            debug(`filter rule fullPackageName: ${fullPackageName}`);
            throw new ReferenceError(`Failed to load textlint's filter rule module: "${packageName}" is not found.
See FAQ: https://github.com/textlint/textlint/blob/master/docs/faq/failed-to-load-textlints-module.md
`);
        }
        return pkgPath;
    }

    /**
     * Take package name, and return path to module.
     * @param {string} packageName
     * @returns {string} return path to module
     */
    resolvePluginPackageName(packageName: string): string {
        const baseDir = this.baseDirectory;
        const PREFIX = this.PLUGIN_NAME_PREFIX;
        const fullPackageName = createFullPackageName(PREFIX, packageName);
        // <plugin-name> or textlint-plugin-<rule-name>
        const pkgPath = tryResolve(path.join(baseDir, fullPackageName)) || tryResolve(path.join(baseDir, packageName));
        if (!pkgPath) {
            debug(`plugin fullPackageName: ${fullPackageName}`);
            throw new ReferenceError(`Failed to load textlint's plugin module: "${packageName}" is not found.
See FAQ: https://github.com/textlint/textlint/blob/master/docs/faq/failed-to-load-textlints-module.md
`);
        }
        return pkgPath;
    }

    /**
     * Take package name, and return path to module.
     * @param {string} packageName
     * The user must specify preset- prefix to these `packageName`.
     * @returns {string} return path to module
     */
    resolvePresetPackageName(packageName: string): string {
        const baseDir = this.baseDirectory;
        const PREFIX = this.RULE_PRESET_NAME_PREFIX;
        /* Implementation Note
    
        preset name is defined in config file:
        In the case, `packageName` is "preset-gizmo"
        TextLintModuleResolver resolve "preset-gizmo" to "textlint-rule-preset-gizmo"
        {
            "rules": {
                "preset-gizmo": {
                    "ruleA": false
                }
            }
        }
         */
        // preset-<name> or textlint-rule-preset-<name>
        // @scope/preset-<name> or @scope/textlint-rule-preset-<name>
        const packageNameWithoutPreset = packageName
            .replace(/^preset-/, "")
            .replace(/^@([^/]+)\/preset-(.*)$/, `@$1/$2`);
        const fullPackageName = createFullPackageName(PREFIX, packageNameWithoutPreset);
        const fullFullPackageName = `${PREFIX}${packageNameWithoutPreset}`;
        // preset-<preset-name> or textlint-rule-preset-<preset-name>
        const pkgPath =
            tryResolve(path.join(baseDir, fullFullPackageName)) ||
            tryResolve(path.join(baseDir, packageNameWithoutPreset)) ||
            // <preset-name> or textlint-rule-preset-<rule-name>
            tryResolve(path.join(baseDir, fullPackageName)) ||
            tryResolve(path.join(baseDir, packageName));
        if (!pkgPath) {
            debug(`preset fullPackageName: ${fullPackageName}`);
            debug(`preset fullFullPackageName: ${fullFullPackageName}`);
            throw new ReferenceError(`Failed to load textlint's preset module: "${packageName}" is not found.
See FAQ: https://github.com/textlint/textlint/blob/master/docs/faq/failed-to-load-textlints-module.md
`);
        }
        return pkgPath;
    }

    /**
     * Take Config package name, and return path to module.
     * @param {string} packageName
     * @returns {string} return path to module
     */
    resolveConfigPackageName(packageName: string): string {
        const baseDir = this.baseDirectory;
        const PREFIX = this.CONFIG_PACKAGE_PREFIX;
        const fullPackageName = createFullPackageName(PREFIX, packageName);
        // <plugin-name> or textlint-config-<rule-name>
        const pkgPath = tryResolve(path.join(baseDir, fullPackageName)) || tryResolve(path.join(baseDir, packageName));
        if (!pkgPath) {
            throw new ReferenceError(`Failed to load textlint's config module: "${packageName}" is not found.
See FAQ: https://github.com/textlint/textlint/blob/master/docs/faq/failed-to-load-textlints-module.md
`);
        }
        return pkgPath;
    }
}
