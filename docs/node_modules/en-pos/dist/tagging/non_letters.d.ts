export interface NonLetterDetector {
    regex: RegExp;
    pos: string;
}
export default function (token: string): string;
