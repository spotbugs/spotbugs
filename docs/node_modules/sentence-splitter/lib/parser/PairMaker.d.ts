import { SourceCode } from "./SourceCode";
import { AbstractMarker } from "./AbstractMarker";
/**
 * Mark pair character
 * PairMarker aim to mark pair string as a single sentence.
 *
 * For example, Following sentence has two period(。). but it should treat a single sentence
 *
 * > I hear "I'm back to home." from radio.
 *
 */
export declare class PairMaker implements AbstractMarker {
    private pairs;
    private pairKeys;
    private pairValues;
    mark(sourceCode: SourceCode): void;
}
