import { SourceCode } from "./SourceCode";
export declare abstract class AbstractParser {
    abstract test(source: SourceCode): boolean;
    abstract seek(source: SourceCode): void;
}
