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
export class AnyValueParser implements AbstractParser {
    private parsers: AbstractParser[];
    private markers: AbstractMarker[];

    /**
     * Eat any value without `parsers.test`
     */
    constructor(options: AnyValueParserOptions) {
        this.parsers = options.parsers;
        this.markers = options.markers;
    }

    test(sourceCode: SourceCode) {
        if (sourceCode.hasEnd) {
            return false;
        }
        return this.parsers.every(parser => !parser.test(sourceCode));
    }

    seek(sourceCode: SourceCode) {
        const currentNode = sourceCode.readNode();
        if (!currentNode) {
            // Text mode
            while (this.test(sourceCode)) {
                this.markers.forEach(marker => marker.mark(sourceCode));
                sourceCode.peek();
            }
            return;
        }
        // node - should not over next node
        const isInCurrentNode = () => {
            const currentOffset = sourceCode.offset;
            return currentNode.range[0] <= currentOffset && currentOffset < currentNode.range[1];
        };
        while (isInCurrentNode() && this.test(sourceCode)) {
            this.markers.forEach(marker => marker.mark(sourceCode));
            sourceCode.peek();
        }
    }
}
