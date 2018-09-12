import { SourceCode } from "./SourceCode";
import { AbstractParser } from "./AbstractParser";
/**
 * Space parser
 */
export declare class SpaceParser implements AbstractParser {
    test(sourceCode: SourceCode): boolean;
    seek(sourceCode: SourceCode): void;
}
