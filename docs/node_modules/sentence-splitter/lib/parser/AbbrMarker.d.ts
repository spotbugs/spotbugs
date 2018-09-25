import { SourceCode } from "./SourceCode";
import { Language } from "./lang/LanguageInterface";
import { AbstractMarker } from "./AbstractMarker";
/**
 * abbreviation marker
 */
export declare class AbbrMarker implements AbstractMarker {
    private lang;
    constructor(lang?: Language);
    /**
     * Get Word
     * word should have left space and right space,
     * @param {SourceCode} sourceCode
     * @param {number} startIndex
     * @returns {string}
     */
    private getWord;
    private getPrevWord;
    mark(sourceCode: SourceCode): void;
}
