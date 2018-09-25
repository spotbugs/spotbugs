import { SourceCode } from "./SourceCode";
import { AbstractParser } from "./AbstractParser";
import { AbstractMarker } from "./AbstractMarker";
export interface AnyValueParserOptions {
    parsers: AbstractParser[];
    markers: AbstractMarker[];
}
/**
 * Any value without `parsers`
 */
export declare class AnyValueParser implements AbstractParser {
    private parsers;
    private markers;
    /**
     * Eat any value without `parsers.test`
     */
    constructor(options: AnyValueParserOptions);
    test(sourceCode: SourceCode): boolean;
    seek(sourceCode: SourceCode): void;
}
