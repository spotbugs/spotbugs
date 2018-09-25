import { SourceCode } from "./SourceCode";
import { AbstractParser } from "./AbstractParser";

/**
 * Space parser
 */
export class SpaceParser implements AbstractParser {
    test(sourceCode: SourceCode) {
        const string = sourceCode.read();
        if (!string) {
            return false;
        }
        return /\s/.test(string);
    }

    seek(sourceCode: SourceCode): void {
        while (this.test(sourceCode)) {
            sourceCode.peek();
        }
    }
}
