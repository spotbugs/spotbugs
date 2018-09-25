import { SourceCode } from "./SourceCode";

export abstract class AbstractMarker {
    abstract mark(source: SourceCode): void;
}
