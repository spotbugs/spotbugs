// LICENSE : MIT
"use strict";

import { BaseRuleContext } from "./BaseRuleContext";

const assert = require("assert");
import { TxtNode, ASTNodeTypes } from "@textlint/ast-node-types";
import RuleFixer, { IntermediateFixCommand } from "../fixer/rule-fixer";
import RuleError from "./rule-error";
import SeverityLevel from "../shared/type/SeverityLevel";
import { getSeverity } from "../shared/rule-severity";
import SourceCode from "./source-code";
import { TextlintRuleOptions } from "../textlint-kernel-interface";
import { ReportFunction } from "../task/textlint-core-task";
// instance for rule context
const ruleFixer = new RuleFixer();

/**
 * This callback is displayed as a global member.
 * @callback ReportCallback
 * @param {ReportMessage} message
 */
/**
 * Rule context object is passed to each rule as `context`
 * @param {string} ruleId
 * @param {SourceCode} sourceCode
 * @param {ReportCallback} report
 * @param {Object|boolean|undefined} ruleOptions
 * @param {string} [configBaseDir]
 * @constructor
 */
export interface RuleContextArgs {
    ruleId: string;
    sourceCode: SourceCode;
    report: ReportFunction;
    ruleOptions?: TextlintRuleOptions;
    configBaseDir?: string;
}

/**
 * Object version of RuleError
 * It is un-document way
 *
 * report(node, {
 *   message: ""
 * })
 */
export interface RuleReportedObject {
    line?: number;
    column?: number;
    index?: number;
    fix?: IntermediateFixCommand;
    message: string;
    severity?: number;

    [index: string]: any;
}

export const createFreezedRuleContext = (args: RuleContextArgs) => {
    return Object.freeze(new RuleContext(args));
};

export default class RuleContext implements BaseRuleContext {
    private _ruleId: string;
    private _sourceCode: SourceCode;
    private _report: ReportFunction;
    private _ruleOptions?: TextlintRuleOptions;
    private _configBaseDir?: string;
    private _severity: number;

    constructor(args: RuleContextArgs) {
        this._ruleId = args.ruleId;
        this._sourceCode = args.sourceCode;
        this._report = args.report;
        this._ruleOptions = args.ruleOptions;
        this._configBaseDir = args.configBaseDir;
        this._severity = getSeverity(this._ruleOptions);
    }

    /**
     * Rule id
     * @returns {string}
     */
    get id() {
        return this._ruleId;
    }

    get severity() {
        return this._severity;
    }

    /**
     * Node's type values
     * @type {ASTNodeTypes}
     */
    get Syntax(): typeof ASTNodeTypes {
        return this._sourceCode.getSyntax();
    }

    /**
     * CustomError object
     * @type {RuleError}
     */
    get RuleError() {
        return RuleError;
    }

    /**
     * Rule fixer command object
     * @type {RuleFixer}
     */
    get fixer() {
        return ruleFixer;
    }

    /**
     * report function that is called in a rule
     */
    report = (node: TxtNode, ruleError: RuleError | RuleReportedObject, _shouldNotUsed?: any) => {
        assert(!(node instanceof RuleError), "1st argument should be node. Usage: `report(node, ruleError);`");
        assert(_shouldNotUsed === undefined, "3rd argument should not be used. Usage: `report(node, ruleError);`");
        if (ruleError instanceof RuleError) {
            // severity come from `.textlintrc` option like `{ "<rule-name>" : { serverity: "warning" } } `
            this._report({ ruleId: this._ruleId, node, severity: this._severity, ruleError });
        } else {
            const ruleReportedObject: RuleReportedObject = ruleError;
            // severity come from report arguments like `report(node, { severity: 1 })`
            const level = ruleReportedObject.severity || SeverityLevel.error;
            this._report({ ruleId: this._ruleId, node, severity: level, ruleError: ruleReportedObject });
        }
    };

    /**
     * get file path current processing.
     */
    getFilePath = () => {
        return this._sourceCode.getFilePath();
    };

    /**
     * Gets the source code for the given node.
     * @param {TxtNode=} node The AST node to get the text for.
     * @param {int=} beforeCount The number of characters before the node to retrieve.
     * @param {int=} afterCount The number of characters after the node to retrieve.
     * @returns {string} The text representing the AST node.
     */
    getSource = (node?: TxtNode, beforeCount?: number, afterCount?: number): string => {
        return this._sourceCode.getSource(node, beforeCount, afterCount);
    };

    /**
     * get config base directory path
     * config base directory path often is the place of .textlintrc
     *
     * e.g.) /path/to/dir/.textlintrc
     * `getConfigBaseDir()` return `"/path/to/dir/"`.
     *
     * When using textlint as module, it is specified by `configBaseDir`
     * If not found the value, return undefined.
     *
     * You can use it for resolving relative path from config dir.
     * @returns {string|undefined}
     */
    getConfigBaseDir = () => {
        return this._configBaseDir;
    };
}
