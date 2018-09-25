import { SourceCode } from "./SourceCode";
import { Language } from "./lang/LanguageInterface";
import { English } from "./lang/English";
import { AbstractMarker } from "./AbstractMarker";

const isCapitalized = (text: string) => {
    if (!text || text.length === 0) {
        return false;
    }
    return /^[A-Z]/.test(text);
};

const compareNoCaseSensitive = (a: string, b: string): boolean => {
    return a.toLowerCase() === b.toLowerCase();
};

/**
 * abbreviation marker
 */
export class AbbrMarker implements AbstractMarker {
    private lang: Language;

    constructor(lang: Language = English) {
        this.lang = lang;
    }

    /**
     * Get Word
     * word should have left space and right space,
     * @param {SourceCode} sourceCode
     * @param {number} startIndex
     * @returns {string}
     */
    private getWord(sourceCode: SourceCode, startIndex: number = 0): string {
        const whiteSpace = /\s/;
        const prevChar = sourceCode.read(-1);
        if (prevChar && !whiteSpace.test(prevChar)) {
            return "";
        }
        let word = "";
        let count = startIndex;
        let char: boolean | string = "";
        while ((char = sourceCode.read(count))) {
            if (whiteSpace.test(char)) {
                break;
            }
            word += char;
            count++;
        }
        return word;
    }

    private getPrevWord(sourceCode: SourceCode): string {
        const whiteSpace = /\s/;
        let count = -1;
        let char: boolean | string = "";
        while ((char = sourceCode.read(count))) {
            if (!whiteSpace.test(char)) {
                break;
            }
            count--;
        }
        while ((char = sourceCode.read(count))) {
            if (whiteSpace.test(char)) {
                break;
            }
            count--;
        }
        return this.getWord(sourceCode, count + 1);
    }

    mark(sourceCode: SourceCode) {
        if (sourceCode.isInContextRange()) {
            return;
        }
        const currentWord = this.getWord(sourceCode);
        if (currentWord.length === 0) {
            return;
        }
        // Allow: Multi-period abbr
        // Example: U.S.A
        if (/^([a-zA-Z]\.){3,}$/.test(currentWord)) {
            return sourceCode.markContextRange([sourceCode.offset, sourceCode.offset + currentWord.length]);
        }
        // EXCALAMATION_WORDS
        // Example: Yahoo!
        const isMatchedEXCALAMATION_WORDS = this.lang.EXCALAMATION_WORDS.some(abbr => {
            return compareNoCaseSensitive(abbr, currentWord);
        });
        if (isMatchedEXCALAMATION_WORDS) {
            return sourceCode.markContextRange([sourceCode.offset, sourceCode.offset + currentWord.length]);
        }
        // PREPOSITIVE_ABBREVIATIONS
        // Example: Mr. Fuji
        const isMatchedPREPOSITIVE_ABBREVIATIONS = this.lang.PREPOSITIVE_ABBREVIATIONS.some(abbr => {
            return compareNoCaseSensitive(abbr, currentWord);
        });
        if (isMatchedPREPOSITIVE_ABBREVIATIONS) {
            return sourceCode.markContextRange([sourceCode.offset, sourceCode.offset + currentWord.length]);
        }
        // ABBREVIATIONS
        const isMatched = this.lang.ABBREVIATIONS.some(abbr => {
            return compareNoCaseSensitive(abbr, currentWord);
        });
        const prevWord = this.getPrevWord(sourceCode);
        const nextWord = this.getWord(sourceCode, currentWord.length + 1);
        // console.log("prevWord", prevWord);
        // console.log("currentWord", currentWord);
        // console.log("nextWord", nextWord);
        // Special case: Capital <ABBR>. Capital
        // Example: `I` as a sentence boundary and `I` as an abbreviation
        // > We make a good team, you and I. Did you see Albert I. Jones yesterday?
        if (isCapitalized(prevWord) && /[A-Z]\./.test(currentWord) && isCapitalized(nextWord)) {
            sourceCode.markContextRange([sourceCode.offset, sourceCode.offset + currentWord.length]);
        } else if (isMatched && !isCapitalized(nextWord)) {
            // Exception. This allow to write Capitalized word at next word
            // A.M. is store.
            sourceCode.markContextRange([sourceCode.offset, sourceCode.offset + currentWord.length]);
        }
    }
}
