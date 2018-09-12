// rule config
import { TxtNode, TxtNodeType } from "@textlint/ast-node-types";
import { SeverityLevelTypes } from "./shared/type/SeverityLevel";
import RuleContext from "./core/rule-context";
import FilterRuleContext from "./core/filter-rule-context";
/**
 * Rule reporter function
 */
export type TextlintRuleReporter = (
    context: Readonly<RuleContext>,
    options?: TextlintRuleOptions
) => { [P in TxtNodeType]?: (node: TxtNode) => void | Promise<any> };
export type TextlintFixableRuleModule = { linter: TextlintRuleReporter; fixer: TextlintRuleReporter };
/**
 * module.export = reporter | { linter, fixer }
 */
export type TextlintRuleModule = TextlintRuleReporter | TextlintFixableRuleModule;
/**
 * Filter rule reporter function
 */
export type TextlintFilterRuleReporter = (
    context: Readonly<FilterRuleContext>,
    options?: TextlintFilterRuleOptions
) => { [P in TxtNodeType]?: (node: TxtNode) => void | Promise<any> };

/**
 * textlint rule option values is object or boolean.
 * if this option value is false, disable the rule.
 */
export type TextlintRuleOptions = {
    [index: string]: any;

    severity?: SeverityLevelTypes;
};

/**
 * textlint filter rule option values is object or boolean.
 * if this option value is false, disable the filter rule.
 */
export type TextlintFilterRuleOptions = {
    [index: string]: any;
};

/**
 * textlint plugin option values is object or boolean.
 * if this option value is false, disable the plugin.
 */
export type TextlintPluginOptions = {
    [index: string]: any;
};

export interface TextlintKernelConstructorOptions {
    /**
     * Suppress messages of severity:warning and severity:info
     */
    quiet?: boolean;
}

// config
export interface TextlintConfigObject {
    // rule directories path
    rulePaths?: string[];
    // filter by file extensions
    extensions?: string[];
    // formatter file name
    // e.g.) stylish.js => set "stylish"
    formatterName?: string;
    // plugin package names
    plugins?: string[];
    // base directory for loading {rule, config, plugin} modules
    rulesBaseDirectory?: string;
    // ".textlint" file path
    configFile?: string;
    // disabled rule package names
    // always should start with empty
    disabledRules?: string[];
    // preset package names
    // e.g.) ["preset-foo"]
    presets?: string[];
    // rules config object
    rulesConfig?: Object;
    /**
     * quite options
     */
    quiet?: boolean;
}

// Plugin
export interface TextlintPluginProcessorConstructor extends Function {
    new (options?: TextlintPluginOptions): TextlintPluginProcessor;

    /**
     * Should defined `availableExtensions()` as instance method instead of static method.
     * @deprecated textlint@11+
     * @see https://github.com/textlint/textlint/issues/531
     */
    availableExtensions?(): Array<string>;
}

export declare class TextlintPluginProcessor {
    constructor(options?: TextlintPluginOptions);

    /**
     * Return available extensions for this plugin.
     * This extension should start with `.`(dot).
     * Example is [".md", ".mkd"]
     */
    availableExtensions(): Array<string>;

    processor(
        extension: string
    ): {
        preProcess(text: string, filePath?: string): TxtNode;
        postProcess(messages: Array<any>, filePath?: string): { messages: Array<any>; filePath: string };
    };
}

// textlint plugin module should export this interface
export interface TextlintPluginCreator {
    Processor: TextlintPluginProcessorConstructor;
}

export interface TextlintKernelPlugin {
    // plugin name as key
    pluginId: string;
    // plugin module
    // For example, `plugin: require("@textlint/textlint-plugin-markdown")`
    plugin: TextlintPluginCreator;
    // plugin options
    options?: TextlintPluginOptions | boolean;
}

export interface TextlintKernelRule {
    // rule name as key
    ruleId: string;
    // rule module
    // For example, `rule: require("textlint-rule-example")`
    rule: TextlintRuleModule;
    // rule options
    // Often rule option is written in .textlintrc
    options?: TextlintRuleOptions | boolean;
}

export interface TextlintKernelFilterRule {
    // filter rule name as key
    ruleId: string;
    // filter rule module instance
    rule: TextlintFilterRuleReporter;
    // filter rule options
    // Often rule option is written in .textlintrc
    options?: TextlintFilterRuleOptions | boolean;
}

export interface TextlintKernelOptions {
    // file type
    // For example) .md
    ext: string;
    // file path
    filePath?: string;
    // plugins
    plugins?: TextlintKernelPlugin[];
    // rules
    rules?: TextlintKernelRule[];
    // filterRules
    filterRules?: TextlintKernelFilterRule[];
    // config base directory
    // It is a value of context.getConfigBaseDir.
    configBaseDir?: string;
}

// "range" will be replaced by "text"
export interface TextlintFixCommand {
    text: string;
    range: [number, number];
}

export interface TextlintMessage {
    // See src/shared/type/MessageType.js
    // Message Type
    type: string;
    // Rule Id
    ruleId: string;
    message: string;
    // optional data
    data?: any;
    // FixCommand
    fix?: TextlintFixCommand;
    // location info
    // Text -> AST TxtNode(0-based columns) -> textlint -> TextlintMessage(**1-based columns**)
    line: number; // start with 1
    column: number; // start with 1
    // indexed-location
    index: number; // start with 0
    // Severity Level
    // See src/shared/type/SeverityLevel.js
    severity: number;
}

// Linting result
export interface TextlintResult {
    filePath: string;
    messages: TextlintMessage[];
}

// Fixing result
export interface TextlintFixResult {
    filePath: string;
    // fixed content
    output: string;
    // all messages = pre-applyingMessages + remainingMessages
    // it is same with one of `TextlintResult`
    messages: TextlintMessage[];
    // applied fixable messages
    applyingMessages: TextlintMessage[];
    // original means original for applyingMessages and remainingMessages
    // pre-applyingMessages + remainingMessages
    remainingMessages: TextlintMessage[];
}
