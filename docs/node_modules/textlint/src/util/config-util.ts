// LICENSE : MIT
"use strict";

export function isPluginRuleKey(key: string) {
    // @<owner>/<plugin><>rule>
    if (key[0] === "@" && key.indexOf("/textlint-plugin") !== -1) {
        return true;
    }
    // not contain @, but contain /
    // <plugin>/<rule>
    return key[0] !== "@" && key.indexOf("/") !== -1;
}

export function isPresetRuleKey(key: string) {
    if (/^preset-/.test(key)) {
        return true;
    }
    // scoped module: @textlint/textlint-rule-preset-foo
    return key[0] === "@" && (key.indexOf("/textlint-rule-preset-") !== -1 || key.indexOf("/preset-") !== -1);
}
