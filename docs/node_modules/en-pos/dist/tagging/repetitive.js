"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const lexicon = require("en-lexicon");
const slang_1 = require("./slang");
function tryToken(token) {
    if (!countRepetitions(token))
        return "";
    token = token.toLowerCase();
    token = removeRepetition(token);
    if (lexicon.lexicon[token])
        return lexicon.lexicon[token].split("|")[0];
    if (slang_1.default(token))
        return slang_1.default(token);
    return tryToken(token);
}
function countRepetitions(token) {
    return token.split("").filter((a) => token.indexOf(a) !== token.lastIndexOf(a)).length;
}
function removeRepetition(token) {
    return token.replace(/(.)(?=\1)/, "");
}
exports.default = tryToken;
