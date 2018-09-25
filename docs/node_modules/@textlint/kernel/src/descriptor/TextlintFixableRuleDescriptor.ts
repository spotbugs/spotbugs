// LICENSE : MIT
"use strict";
import { getFixer } from "./rule-creator-helper";
import { TextlintRuleReporter } from "../textlint-kernel-interface";
import { TextlintLintableRuleDescriptor } from "./TextlintLintableRuleDescriptor";

/**
 * Textlint Fixable Rule Descriptor.
 * It is inherit **Rule** Descriptor and add fixer() method.
 * It handle RuleCreator and RuleOption.
 */
export class TextlintFixableRuleDescriptor extends TextlintLintableRuleDescriptor {
    /**
     * Return fixer function
     * You should check hasFixer before call this.
     */
    get fixer(): TextlintRuleReporter {
        return getFixer(this.rule);
    }
}
