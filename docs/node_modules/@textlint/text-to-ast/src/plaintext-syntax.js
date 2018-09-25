// LICENSE : MIT
"use strict";
const { ASTNodeTypes } = require("@textlint/ast-node-types");
const exports = {
    Document: ASTNodeTypes.Document, // must
    Paragraph: ASTNodeTypes.Paragraph,
    // inline
    Str: ASTNodeTypes.Str, // must
    Break: ASTNodeTypes.Break // must
};
module.exports = exports;
