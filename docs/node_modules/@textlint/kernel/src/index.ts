// Kernel
export { TextlintKernel } from "./textlint-kernel";
// Kernel Descriptor
export * from "./descriptor/index";
// Types
export {
    TextlintResult,
    TextlintFixResult,
    TextlintFixCommand,
    TextlintMessage,
    // Kernel rule/filter/plugin format
    TextlintKernelRule,
    TextlintKernelFilterRule,
    TextlintKernelPlugin,
    // Notes: Following interface will be separated module in the future.
    // textlint rule interface
    TextlintRuleReporter,
    TextlintRuleModule,
    TextlintRuleOptions,
    // textlint filter rule interface
    TextlintFilterRuleReporter,
    TextlintFilterRuleOptions,
    // textlint plugin interface
    TextlintPluginCreator,
    TextlintPluginOptions,
    TextlintPluginProcessor,
    TextlintPluginProcessorConstructor
} from "./textlint-kernel-interface";
