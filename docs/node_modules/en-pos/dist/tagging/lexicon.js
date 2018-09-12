"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const lexicon = require("en-lexicon");
function default_1(word, sensitive) {
    if (!sensitive)
        word = word.toLowerCase();
    const entry = lexicon.lexicon[word];
    return entry ? entry.split("|")[0] : "";
}
exports.default = default_1;
;
