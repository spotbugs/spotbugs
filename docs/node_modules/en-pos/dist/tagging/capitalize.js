"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function default_1(token) {
    return token.split(/\W/).map((x) => x.charAt(0).toUpperCase() + x.substr(1).toLowerCase()).join("-");
}
exports.default = default_1;
;
