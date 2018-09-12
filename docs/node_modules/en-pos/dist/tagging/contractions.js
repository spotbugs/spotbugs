"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function default_1(token) {
    if (token.length > 3 || token.charAt(0) !== "'")
        return "";
    token = token.toLowerCase();
    if (token === "'re")
        return "VBP";
    if (token === "'m")
        return "VBP";
    if (token === "'ve")
        return "VBP";
    if (token === "'ll")
        return "MD";
    if (token === "'d")
        return "MD";
    return "";
}
exports.default = default_1;
;
