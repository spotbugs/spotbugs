// MIT © 2017 azu
"use strict";
const { Tag } = require("en-pos");
export const getPosFromSingleWord = word => {
    const tags = new Tag([word])
        .initial() // initial dictionary and pattern based tagging
        .smooth().tags; // further context based smoothing
    return tags[0];
};
export const isCapitalized = string => {
    return /^[A-Z]/.test(string);
};
/**
 * to upper
 * @param {string} string
 * @returns {string}
 */
export const upperFirstCharacter = string => {
    return string.charAt(0).toUpperCase() + string.slice(1);
};
/**
 * @param {string} string
 * @returns {string}
 */
export const lowerFirstCharacter = string => {
    return string.charAt(0).toLocaleLowerCase() + string.slice(1);
};
