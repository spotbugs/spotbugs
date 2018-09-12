// LICENSE : MIT
"use strict";
const assert = require("assert");
import StructureSource from "structured-source";

const defaultOptions = {
    // charRegExp is deprecated
    charRegExp: /[\.。\?\!？！]/,
    // separator char list
    separatorChars: [".", "。", "?", "!", "？", "！"],
    newLineCharacters: "\n",
    whiteSpaceCharacters: [" ", "　"]
};
export const Syntax = {
    WhiteSpace: "WhiteSpace",
    Sentence: "Sentence"
};

/**
 * @param {string} text
 * @param {{
 *      charRegExp: ?Object,
 *      separatorChars: ?string[],
 *      newLineCharacters: ?String,
 *      whiteSpaceCharacters: ?string[]
 *  }} options
 * @returns {Array}
 */
export function split(text, options = {}) {
    const charRegExp = options.charRegExp;
    const separatorChars = options.separatorChars || defaultOptions.separatorChars;
    const whiteSpaceCharacters = options.whiteSpaceCharacters || defaultOptions.whiteSpaceCharacters;
    assert(
        !(options.charRegExp && options.separatorChars),
        "should use either one `charRegExp` or `separatorChars`.\n" + "`charRegExp` is deprecated."
    );
    /**
     * Is the `char` separator symbol?
     * @param {string} char
     * @returns {boolean}
     */
    const testCharIsSeparator = char => {
        if (charRegExp) {
            return charRegExp.test(char);
        }
        return separatorChars.indexOf(char) !== -1;
    };
    const newLineCharacters = options.newLineCharacters || defaultOptions.newLineCharacters;
    const src = new StructureSource(text);
    const createNode = (type, start, end) => {
        let range = [start, end];
        let location = src.rangeToLocation(range);
        let slicedText = text.slice(start, end);
        let node;
        if (type === Syntax.WhiteSpace) {
            node = createWhiteSpaceNode(slicedText, location, range);
        } else if (type === Syntax.Sentence) {
            node = createSentenceNode(slicedText, location, range);
        }
        return node;
    };
    let results = [];
    let startPoint = 0;
    let currentIndex = 0;
    let isSplitPoint = false;
    let isInSentence = false;
    const newLineCharactersLength = newLineCharacters.length;
    for (; currentIndex < text.length; currentIndex++) {
        let char = text[currentIndex];
        let whiteTarget = text.slice(currentIndex, currentIndex + newLineCharactersLength);
        if (whiteTarget === newLineCharacters) {
            // (string)\n
            if (startPoint !== currentIndex) {
                results.push(createNode(Syntax.Sentence, startPoint, currentIndex));
            }
            for (let i = 0; i < newLineCharactersLength; i++) {
                // string(\n)
                let startIndex = currentIndex + i;
                results.push(createNode(Syntax.WhiteSpace, startIndex, startIndex + 1));
            }
            // string\n|
            startPoint = currentIndex + newLineCharactersLength;
            isSplitPoint = false;
        } else if (testCharIsSeparator(char)) {
            isSplitPoint = true;
        } else {
            // why `else`
            // it for support 。。。 pattern
            if (isSplitPoint) {
                results.push(createNode(Syntax.Sentence, startPoint, currentIndex));
                // reset stat
                startPoint = currentIndex;
                isSplitPoint = false;
                isInSentence = false;
            }
            // Sentence<WhiteSpace>*Sentence
            if (isInSentence === false && whiteSpaceCharacters.indexOf(char) !== -1) {
                // Add WhiteSpace
                results.push(createNode(Syntax.WhiteSpace, startPoint, currentIndex + 1));
                startPoint++;
            } else {
                // New sentence start
                isInSentence = true;
            }
        }
    }

    if (startPoint !== currentIndex) {
        results.push(createNode(Syntax.Sentence, startPoint, currentIndex));
    }
    return results;
}

/**
 * @param {string} text
 * @param {Object} loc
 * @param {number[]} range
 * @returns {{type: string, raw: string, value: string, loc: Object, range: number[]}}
 */
export function createWhiteSpaceNode(text, loc, range) {
    return {
        type: "WhiteSpace",
        raw: text,
        value: text,
        loc: loc,
        range: range
    };
}

/**
 * @param {string} text
 * @param {Object} loc
 * @param {number[]} range
 * @returns {{type: string, raw: string, value: string, loc: Object, range: number[]}}
 */
export function createSentenceNode(text, loc, range) {
    return {
        type: "Sentence",
        raw: text,
        value: text,
        loc: loc,
        range: range
    };
}
