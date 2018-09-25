// LICENSE : MIT
"use strict";
import { TextlintFixResult } from "@textlint/kernel";
export default function(results: TextlintFixResult[]) {
    return JSON.stringify(results);
}
