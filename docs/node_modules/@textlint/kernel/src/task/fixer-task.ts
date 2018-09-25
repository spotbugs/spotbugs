// LICENSE : MIT
"use strict";
import CoreTask from "./textlint-core-task";
import { createFreezedRuleContext } from "../core/rule-context";
import { createFreezedFilterRuleContext } from "../core/filter-rule-context";
import { TextlintKernelConstructorOptions } from "../textlint-kernel-interface";
import SourceCode from "../core/source-code";
import { TextlintFilterRuleDescriptors, TextlintFixableRuleDescriptor } from "../descriptor";

const debug = require("debug")("textlint:TextLintCoreTask");

export interface TextLintCoreTaskArgs {
    config: TextlintKernelConstructorOptions;
    fixableRuleDescriptor: TextlintFixableRuleDescriptor;
    filterRuleDescriptors: TextlintFilterRuleDescriptors;
    sourceCode: SourceCode;
    configBaseDir?: string;
}

export default class TextLintCoreTask extends CoreTask {
    config: TextlintKernelConstructorOptions;
    fixableRuleDescriptor: TextlintFixableRuleDescriptor;
    filterRuleDescriptors: TextlintFilterRuleDescriptors;
    sourceCode: SourceCode;
    configBaseDir?: string;

    constructor({
        config,
        configBaseDir,
        fixableRuleDescriptor,
        filterRuleDescriptors,
        sourceCode
    }: TextLintCoreTaskArgs) {
        super();
        this.config = config;
        this.configBaseDir = configBaseDir;
        this.fixableRuleDescriptor = fixableRuleDescriptor;
        this.filterRuleDescriptors = filterRuleDescriptors;
        this.sourceCode = sourceCode;
        this._setupRules();
    }

    start() {
        this.startTraverser(this.sourceCode);
    }

    private _setupRules() {
        // rule
        const sourceCode = this.sourceCode;
        const report = this.createReporter(sourceCode);
        const ignoreReport = this.createShouldIgnore();
        // setup "rules" field by using a single fixerRule
        debug("fixerRule", this.fixableRuleDescriptor);
        const ruleContext = createFreezedRuleContext({
            ruleId: this.fixableRuleDescriptor.id,
            ruleOptions: this.fixableRuleDescriptor.normalizedOptions,
            sourceCode,
            report,
            configBaseDir: this.configBaseDir
        });
        this.tryToAddListenRule(
            this.fixableRuleDescriptor.fixer,
            ruleContext,
            this.fixableRuleDescriptor.normalizedOptions
        );
        // setup "filters" field
        debug("filterRules", this.filterRuleDescriptors);
        this.filterRuleDescriptors.descriptors.forEach(filterRuleDescriptor => {
            const ruleContext = createFreezedFilterRuleContext({
                ruleId: filterRuleDescriptor.id,
                sourceCode,
                ignoreReport
            });
            this.tryToAddListenRule(filterRuleDescriptor.filter, ruleContext, filterRuleDescriptor.normalizedOptions);
        });
    }
}
