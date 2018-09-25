import { SourceCode } from "./SourceCode";
import { AbstractParser } from "./AbstractParser";

const separatorPattern = /[.．。?!？！]/;

/**
 * Separator parser
 */
export class SeparatorParser implements AbstractParser {
    test(sourceCode: SourceCode) {
        if (sourceCode.isInContext()) {
            return false;
        }
        if (sourceCode.isInContextRange()) {
            return false;
        }
        const firstChar = sourceCode.read();
        const nextChar = sourceCode.read(1);
        if (!firstChar) {
            return false;
        }
        if (!separatorPattern.test(firstChar)) {
            return false;
        }
        // Need space after period
        // Example: This is a pen. This it not a pen.
        // It will avoid false-position like `1.23`
        if (firstChar === ".") {
            if (nextChar) {
                return /[\s\t\r\n]/.test(nextChar);
            } else {
                return true;
            }
        }
        return true;
    }

    seek(sourceCode: SourceCode): void {
        while (this.test(sourceCode)) {
            sourceCode.peek();
        }
    }
}
