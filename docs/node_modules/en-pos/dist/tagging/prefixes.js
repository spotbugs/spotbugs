"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const lexicon = require("en-lexicon");
const prefixes = [
    "counter",
    "electro",
    "across",
    "circum",
    "contra",
    "crypto",
    "franco",
    "hetero",
    "thermo",
    "paleo",
    "intro",
    "supra",
    "super",
    "astro",
    "ortho",
    "socio",
    "macro",
    "infra",
    "retro",
    "intra",
    "quasi",
    "under",
    "multi",
    "ultra",
    "hyper",
    "hydro",
    "micro",
    "photo",
    "extra",
    "anglo",
    "trans",
    "inter",
    "after",
    "anti",
    "tele",
    "ambi",
    "hind",
    "homo",
    "hypo",
    "ideo",
    "idio",
    "afro",
    "arch",
    "maxi",
    "mega",
    "meta",
    "auto",
    "down",
    "mono",
    "back",
    "ante",
    "cryo",
    "omni",
    "hemi",
    "over",
    "euro",
    "para",
    "xeno",
    "fore",
    "peri",
    "pleo",
    "poly",
    "post",
    "demi",
    "vice",
    "pyro",
    "gyro",
    "self",
    "semi",
    "step",
    "cis",
    "mis",
    "sub",
    "bio",
    "pro",
    "pre",
    "per",
    "ped",
    "pan",
    "out",
    "off",
    "non",
    "dia",
    "uni",
    "mid",
    "iso",
    "geo",
    "tri",
    "epi",
    "ana",
    "com",
    "sur",
    "eco",
    "dis",
    "apo",
    "neo",
    "re",
    "co",
    "by",
    "ex",
    "en",
    "em",
    "di",
    "bi",
    "un",
    "de"
];
function default_1(token) {
    for (let i = prefixes.length - 1; i >= 0; i--) {
        let prefix = prefixes[i];
        let prefixUsed = token.indexOf(prefix) === 0;
        let prefixWithHyphen = token.indexOf(prefix + "-") === 0;
        if (!(prefixWithHyphen || prefixUsed))
            continue;
        let fragment = "";
        if (prefixWithHyphen)
            fragment = token.split(prefix + "-")[1];
        else
            fragment = token.split(prefix)[1];
        let lexiconEntry = lexicon.lexicon[fragment];
        if (!lexiconEntry)
            continue;
        lexicon.lexicon[token] = lexiconEntry;
        return lexiconEntry.split("|")[0];
    }
    return "";
}
exports.default = default_1;
;
