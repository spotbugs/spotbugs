import { SourceCode } from "./SourceCode";

export abstract class AbstractParser {
    abstract test(source: SourceCode): boolean;

    abstract seek(source: SourceCode): void;
}
