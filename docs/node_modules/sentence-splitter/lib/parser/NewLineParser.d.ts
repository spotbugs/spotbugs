import { SourceCode } from "./SourceCode";
import { AbstractParser } from "./AbstractParser";
/**
 * New Line Parser
 */
export declare class NewLineParser implements AbstractParser {
    test(sourceCode: SourceCode): boolean;
    seek(sourceCode: SourceCode): void;
}
