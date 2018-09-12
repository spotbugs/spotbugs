// LICENSE : MIT
"use strict";
const TraverseController = require("@textlint/ast-traverse").Controller;
const traverseController = new TraverseController();
const debug = require("debug")("textlint:core-task");
import { PromiseEventEmitter } from "./promise-event-emitter";
import RuleError from "../core/rule-error";
import SourceLocation from "../core/source-location";
import timing from "../util/timing";
import MessageType from "../shared/type/MessageType";
import { EventEmitter } from "events";
import * as assert from "assert";
import SourceCode from "../core/source-code";
import { TxtNode } from "@textlint/ast-node-types";
import {
    TextlintRuleReporter,
    TextlintFilterRuleReporter,
    TextlintFilterRuleOptions,
    TextlintFixCommand,
    TextlintRuleOptions
} from "../textlint-kernel-interface";
import { default as RuleContext, RuleReportedObject } from "../core/rule-context";
import FilterRuleContext from "../core/filter-rule-context";
import Bluebird = require("bluebird");

class RuleTypeEmitter extends PromiseEventEmitter {}

/**
 * Ignoring Report function
 */
/**
 * Message of ignoring
 * @typedef {Object} ReportIgnoreMessage
 * @property {string} ruleId
 * @property {number[]} range
 * @property {string} ignoringRuleId to ignore ruleId
 * "*" is special case, it match all ruleId(work as wildcard).
 */
export interface ShouldIgnoreArgs {
    ruleId: string;
    range: [number, number];
    optional: {
        ruleId?: string;
    };
}

export interface IgnoreReportedMessage {
    ruleId: string;
    type: typeof MessageType.ignore;
    // location info
    // TODO: compatible with TextLintMessage
    // line: number; // start with 1
    // column: number;// start with 1
    // // indexed-location
    // index: number;// start with 0
    range: [number, number];

    ignoringRuleId: string;
}

export type ShouldIgnoreFunction = (args: ShouldIgnoreArgs) => void;

/**
 * context.report function
 */
export interface ReportArgs {
    ruleId: string;
    node: TxtNode;
    severity: number;
    ruleError: RuleError | RuleReportedObject;
}

export type ReportFunction = (args: ReportArgs) => void;

export interface LintReportedMessage {
    type: typeof MessageType.lint;
    ruleId: string;
    message: string;
    index: number;
    // See https://github.com/textlint/textlint/blob/master/typing/textlint.d.ts
    line: number; // start with 1(1-based line number)
    column: number; // start with 1(1-based column number)
    severity: number; // it's for compatible ESLint formatter
    fix?: TextlintFixCommand;
}

/**
 * CoreTask receive AST and prepare, traverse AST, emit nodeType event!
 * You can observe task and receive "message" event that is TextLintMessage.
 */
export default abstract class TextLintCoreTask extends EventEmitter {
    private ruleTypeEmitter: RuleTypeEmitter;

    static get events() {
        return {
            // receive start event
            start: "start",
            // receive message from each rules
            message: "message",
            // receive complete event
            complete: "complete",
            // receive error event
            error: "error"
        };
    }

    constructor() {
        super();
        this.ruleTypeEmitter = new RuleTypeEmitter();
    }

    abstract start(): void;

    createShouldIgnore(): ShouldIgnoreFunction {
        const shouldIgnore = (args: ShouldIgnoreArgs) => {
            const { ruleId, range, optional } = args;
            assert(
                typeof range[0] !== "undefined" && typeof range[1] !== "undefined" && range[0] >= 0 && range[1] >= 0,
                "ignoreRange should have actual range: " + range
            );
            // FIXME: should have index, loc
            // should be compatible with LintReportedMessage?
            const message: IgnoreReportedMessage = {
                type: MessageType.ignore,
                ruleId: ruleId,
                range: range,
                // ignoring target ruleId - default: filter all messages
                ignoringRuleId: optional.ruleId || "*"
            };
            this.emit(TextLintCoreTask.events.message, message);
        };
        return shouldIgnore;
    }

    createReporter(sourceCode: SourceCode): ReportFunction {
        const sourceLocation = new SourceLocation(sourceCode);
        /**
         * push new RuleError to results
         * @param {ReportMessage} reportArgs
         */
        const reportFunction = (reportArgs: ReportArgs) => {
            const { ruleId, severity, ruleError } = reportArgs;
            debug("%s pushReport %s", ruleId, ruleError);
            const { line, column, fix } = sourceLocation.adjust(reportArgs);
            const index = sourceCode.positionToIndex({ line, column });
            // add TextLintMessage
            const message: LintReportedMessage = {
                type: MessageType.lint,
                ruleId: ruleId,
                message: ruleError.message,
                index,
                // See https://github.com/textlint/textlint/blob/master/typing/textlint.d.ts
                line: line, // start with 1(1-based line number)
                column: column + 1, // start with 1(1-based column number)
                severity: severity, // it's for compatible ESLint formatter
                fix: fix !== undefined ? fix : undefined
            };
            if (!(ruleError instanceof RuleError)) {
                // FIXME: RuleReportedObject should be removed
                // `error` is a any data.
                const data = ruleError;
                (message as any).data = data;
            }
            this.emit(TextLintCoreTask.events.message, message);
        };
        return reportFunction;
    }

    /**
     * start process and emitting events.
     * You can listen message by `task.on("message", message => {})`
     * @param {SourceCode} sourceCode
     */
    startTraverser(sourceCode: SourceCode) {
        this.emit(TextLintCoreTask.events.start);
        const promiseQueue: Array<Bluebird<Array<void>>> = [];
        const ruleTypeEmitter = this.ruleTypeEmitter;
        traverseController.traverse(sourceCode.ast, {
            enter(node: TxtNode, parent?: TxtNode) {
                const type = node.type;
                Object.defineProperty(node, "parent", { value: parent });
                if (ruleTypeEmitter.listenerCount(type) > 0) {
                    const promise = ruleTypeEmitter.emit(type, node);
                    promiseQueue.push(promise);
                }
            },
            leave(node: TxtNode) {
                const type = `${node.type}:exit`;
                if (ruleTypeEmitter.listenerCount(type) > 0) {
                    const promise = ruleTypeEmitter.emit(type, node);
                    promiseQueue.push(promise);
                }
            }
        });
        Bluebird.all(promiseQueue)
            .then(() => {
                this.emit(TextLintCoreTask.events.complete);
            })
            .catch(error => {
                this.emit(TextLintCoreTask.events.error, error);
            });
    }

    /**
     * try to get rule object
     */
    tryToGetRuleObject(
        ruleCreator: TextlintRuleReporter,
        ruleContext: Readonly<RuleContext>,
        ruleOptions?: TextlintRuleOptions
    ) {
        try {
            return ruleCreator(ruleContext, ruleOptions);
        } catch (error) {
            error.message = `Error while loading rule '${ruleContext.id}': ${error.message}`;
            throw error;
        }
    }

    /**
     * try to get filter rule object
     */
    tryToGetFilterRuleObject(
        ruleCreator: TextlintFilterRuleReporter,
        ruleContext: Readonly<FilterRuleContext>,
        ruleOptions?: TextlintFilterRuleOptions
    ) {
        try {
            return ruleCreator(ruleContext, ruleOptions);
        } catch (error) {
            error.message = `Error while loading filter rule '${ruleContext.id}': ${error.message}`;
            throw error;
        }
    }

    /**
     * add all the node types as listeners of the rule
     * @param {Function} ruleCreator
     * @param {Readonly<RuleContext>|Readonly<FilterRuleContext>} ruleContext
     * @param {Object|boolean|undefined} ruleOptions
     * @returns {Object}
     */
    tryToAddListenRule(
        ruleCreator: TextlintRuleReporter | TextlintFilterRuleReporter,
        ruleContext: Readonly<RuleContext> | Readonly<FilterRuleContext>,
        ruleOptions?: TextlintRuleOptions | TextlintFilterRuleOptions
    ): void {
        const ruleObject =
            ruleContext instanceof RuleContext
                ? this.tryToGetRuleObject(
                      ruleCreator as TextlintRuleReporter,
                      ruleContext as Readonly<RuleContext>,
                      ruleOptions
                  )
                : this.tryToGetFilterRuleObject(
                      ruleCreator as TextlintFilterRuleReporter,
                      ruleContext as Readonly<FilterRuleContext>,
                      ruleOptions
                  );
        const types = Object.keys(ruleObject) as (keyof typeof ruleObject)[];
        types.forEach((nodeType: keyof typeof ruleObject) => {
            this.ruleTypeEmitter.on(
                nodeType,
                timing.enabled ? timing.time(ruleContext.id, ruleObject[nodeType] as Function) : ruleObject[nodeType]!
            );
        });
    }
}
