import { SourceCode } from "./SourceCode";
import { AbstractParser } from "./AbstractParser";
/**
 * Separator parser
 */
export declare class SeparatorParser implements AbstractParser {
    test(sourceCode: SourceCode): boolean;
    seek(sourceCode: SourceCode): void;
}
