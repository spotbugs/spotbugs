// LICENSE : MIT
"use strict";
const fs = require("fs");
const Promise = require("bluebird");
export function readFile<T = any>(filePath: string): T {
    return new Promise((resolve: any, reject: any) => {
        fs.readFile(filePath, "utf-8", (error: any | undefined, result: any) => {
            if (error) {
                return reject(error);
            }
            resolve(result);
        });
    });
}
