// LICENSE : MIT
"use strict";
import CoreTask from "./textlint-core-task";
import { createFreezedRuleContext } from "../core/rule-context";
import { createFreezedFilterRuleContext } from "../core/filter-rule-context";
import { TextlintKernelConstructorOptions } from "../textlint-kernel-interface";
import SourceCode from "../core/source-code";
import { TextlintFilterRuleDescriptors, TextlintRuleDescriptors } from "../descriptor";

const debug = require("debug")("textlint:TextLintCoreTask");

export interface TextLintCoreTaskArgs {
    config: TextlintKernelConstructorOptions;
    ruleDescriptors: TextlintRuleDescriptors;
    filterRuleDescriptors: TextlintFilterRuleDescriptors;
    sourceCode: SourceCode;
    configBaseDir?: string;
}

export default class TextLintCoreTask extends CoreTask {
    config: TextlintKernelConstructorOptions;
    ruleDescriptors: TextlintRuleDescriptors;
    filterRuleDescriptors: TextlintFilterRuleDescriptors;
    sourceCode: SourceCode;
    configBaseDir?: string;

    constructor({
        config,
        configBaseDir,
        ruleDescriptors,
        filterRuleDescriptors: filterRuleDescriptors,
        sourceCode
    }: TextLintCoreTaskArgs) {
        super();
        this.config = config;
        this.configBaseDir = configBaseDir;
        this.ruleDescriptors = ruleDescriptors;
        this.filterRuleDescriptors = filterRuleDescriptors;
        this.sourceCode = sourceCode;
        this._setupRules();
    }

    start() {
        this.startTraverser(this.sourceCode);
    }

    _setupRules() {
        // rule
        const sourceCode = this.sourceCode;
        const report = this.createReporter(sourceCode);
        const ignoreReport = this.createShouldIgnore();
        // setup "rules" field
        // filter duplicated rules for improving experience
        // see https://github.com/textlint/textlint/issues/219
        debug("rules", this.ruleDescriptors);
        this.ruleDescriptors.lintableDescriptors.forEach(ruleDescriptor => {
            const ruleOptions = ruleDescriptor.normalizedOptions;
            const ruleContext = createFreezedRuleContext({
                ruleId: ruleDescriptor.id,
                ruleOptions: ruleOptions,
                sourceCode,
                report,
                configBaseDir: this.configBaseDir
            });
            this.tryToAddListenRule(ruleDescriptor.linter, ruleContext, ruleOptions);
        });
        // setup "filters" field
        debug("filterRules", this.filterRuleDescriptors);
        this.filterRuleDescriptors.descriptors.forEach(filterDescriptor => {
            const ruleContext = createFreezedFilterRuleContext({
                ruleId: filterDescriptor.id,
                sourceCode,
                ignoreReport,
                configBaseDir: this.configBaseDir
            });
            this.tryToAddListenRule(filterDescriptor.filter, ruleContext, filterDescriptor.normalizedOptions);
        });
    }
}
