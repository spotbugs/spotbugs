"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const names = require("humannames");
const capitalize_1 = require("./capitalize");
function default_1(token, sensitive) {
    if (!sensitive)
        token = capitalize_1.default(token);
    if (names[token])
        return "NNP";
    return "";
}
exports.default = default_1;
;
