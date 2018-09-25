"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const lexicon = require("en-lexicon");
const list = [
    ["no", "nah", "nope", "n"],
    ["yes", "yeah", "yep", "yup", "yah", "aye", "yea", "ya"],
    ["seriously", "srlsy"],
    ["why", "y"],
    ["ok", "k", "okay", "o.k.", "oki", "okey-dokey", "okey-doke"],
    ["alright", "alrighty"],
    ["them", "'em", "dem"],
    ["you", "ya", "ye", "u"],
    ["your", "yo"],
    ["because", "cuz", "b/c"],
    ["please", "plz", "pwez"],
    ["this", "dis"],
    ["tomorrow", "2moro", "2mro", "2mr"],
    ["tonight", "2nite", "2nt"],
    ["today", "2day", "2dy"],
    ["great", "gr8"],
    ["later", "l8r"],
    ["thanks", "thx", "thks", "tx", "tnx", "tanx"],
    ["are", "'re", "r"],
    ["am", "'m", "m"],
    ["hello", "hi", "hey"],
    ["love", "<3"],
    ["babe", "bae"],
    ["what", "dafuq"],
    ["fuck", "fml"],
    ["ugh", "facepalm", "smh"],
    ["laughing", "lol", "lolz", "lulz", "lols", "lmao", "lmfao", "rofl", "roflmao", "roflol"],
    ["right", "ikr?", "ikr"],
];
function default_1(token) {
    token = token.toLowerCase();
    for (var i = 0; i < list.length; i++) {
        if (~list[i].indexOf(token)) {
            let nToken = list[i][0];
            let nEntry = lexicon.lexicon[nToken];
            lexicon.lexicon[token] = nEntry;
            return (nEntry || "").split("|")[0];
        }
    }
    return "";
}
exports.default = default_1;
;
