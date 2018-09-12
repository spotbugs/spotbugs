// LICENSE : MIT
"use strict";
import {RuleHelper} from "textlint-rule-helper";
import {matchCaptureGroupAll} from "match-index"
const defaultOptions = {
    // allow to use !
    "allowHalfWidthExclamation": false,
    // allow to use ！
    "allowFullWidthExclamation": false,
    // allow to use ?
    "allowHalfWidthQuestion": false,
    // allow to use ？
    "allowFullWidthQuestion": false
};
const Mark = {
    HalfWidthExclamation: /(!)/,
    FullWidthExclamation: /(！)/,
    HalfWidthQuestion: /(\?)/,
    FullWidthQuestion: /(？)/
};

module.exports = function (context, options = defaultOptions) {
    const {Syntax, RuleError, report, getSource} = context;
    const helper = new RuleHelper(context);
    const allowHalfWidthExclamation = options.allowHalfWidthExclamation || defaultOptions.allowHalfWidthExclamation;
    const allowFullWidthExclamation = options.allowFullWidthExclamation || defaultOptions.allowFullWidthExclamation;
    const allowHalfWidthQuestion = options.allowHalfWidthQuestion || defaultOptions.allowHalfWidthQuestion;
    const allowFullWidthQuestion = options.allowFullWidthQuestion || defaultOptions.allowFullWidthQuestion;

    return {
        [Syntax.Str](node){
            if (helper.isChildNode(node, [Syntax.Link, Syntax.Image, Syntax.BlockQuote, Syntax.Emphasis])) {
                return;
            }
            const text = getSource(node);
            /**
             * report if match the markRegExp
             * @param {string} text
             * @param {RegExp} markRegExp
             */
            const reportIfIncludeMark = (text, markRegExp) => {
                matchCaptureGroupAll(text, markRegExp).forEach(({text, index}) => {
                    report(node, new RuleError(`Disallow to use "${text}".`, {
                        index
                    }));
                });
            };
            // Check
            if (!allowHalfWidthExclamation) {
                reportIfIncludeMark(text, Mark.HalfWidthExclamation);
            }
            if (!allowHalfWidthQuestion) {
                reportIfIncludeMark(text, Mark.HalfWidthQuestion);
            }
            if (!allowFullWidthExclamation) {
                reportIfIncludeMark(text, Mark.FullWidthExclamation);
            }
            if (!allowFullWidthQuestion) {
                reportIfIncludeMark(text, Mark.FullWidthQuestion);
            }
        }
    }
};