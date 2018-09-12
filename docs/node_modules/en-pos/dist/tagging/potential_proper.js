"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const inflectors = require("en-inflectors");
function default_1(token) {
    const regex = /^([A-Z])(('[A-Z])?)[A-Za-z0-9.]+$/;
    if (!regex.test(token))
        return "";
    if (new inflectors.Inflectors(token).isPlural())
        return "NNPS";
    else
        return "NNP";
}
exports.default = default_1;
;
