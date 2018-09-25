"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const lexicon = require("en-lexicon");
const rules = [
    {
        regexp: /(CD-CD(s)?)$/,
        pos: "CD"
    },
    {
        regexp: /(NNP-NNP|JJ-NNP|NNP-CD(s)?|1UC|1UC-CD)$/,
        pos: "NNP"
    },
    {
        regexp: /(NNS|NN-VBZ)$/,
        pos: "NNS"
    },
    {
        regexp: /(VB-IN|NN-NN|NN-VBG)$/,
        pos: "NN"
    },
];
function default_1(token) {
    token = token
        .split("-")
        .map((part) => {
        if (lexicon.lexicon[part])
            return lexicon.lexicon[part].split("|")[0];
        if (lexicon.lexicon[part.toLowerCase()])
            return lexicon.lexicon[part.toLowerCase()].split("|")[0];
        if (!isNaN(parseInt(part)))
            return "CD";
        if (!isNaN(parseInt(part.substr(0, part.length - 1))))
            return "CD";
        if (part === part.charAt(0).toUpperCase() + part.substr(1))
            return "1UC";
        return "UNKNOWN";
    })
        .join("-");
    for (var i = 0; i < rules.length; i++) {
        if (rules[i].regexp.test(token))
            return rules[i].pos;
    }
    return "JJ";
}
exports.default = default_1;
;
