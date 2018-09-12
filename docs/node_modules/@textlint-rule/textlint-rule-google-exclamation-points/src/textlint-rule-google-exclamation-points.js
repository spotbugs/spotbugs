// MIT © 2017 azu
"use strict";
const noExclamationQuestionMark = require("textlint-rule-no-exclamation-question-mark");
const defaultOptions = {
    // allow to use !
    allowHalfWidthExclamation: false,
    // allow to use ！
    allowFullWidthExclamation: false,
    // allow to use ?
    allowHalfWidthQuestion: false,
    // allow to use ？
    allowFullWidthQuestion: false
};
const linter = (context, options = defaultOptions) => {
    const { report } = context;
    const overlayContext = Object.create(context);
    Object.defineProperty(overlayContext, "report", {
        value: (node, error) => {
            error.message += "\nhttps://developers.google.com/style/exclamation-points";
            report(node, error);
        },
        enumerable: true,
        configurable: true,
        writable: true
    });
    return noExclamationQuestionMark(overlayContext, options);
};
module.exports = linter;
