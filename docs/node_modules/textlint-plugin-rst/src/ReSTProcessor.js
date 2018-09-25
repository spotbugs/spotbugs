"use strict";

import { parse } from "./rst-to-ast";

export default class ReSTProcessor {
    constructor(config) {
        this.config = config;
    }

    static availableExtensions() {
        return [
            ".rst",
            ".rest"
        ];
    }

    processor(ext) {
        return {
            preProcess(text, filePath) {
                return parse(text);
            },
            postProcess(messages, filePath) {
                return {
                    messages,
                    filePath: filePath ? filePath : "<rst>"
                };
            }
        };
    }
}
