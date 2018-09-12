export interface Rule {
    from: string;
    to: string;
    type: number;
    c1: string;
    c2: string;
    c3: string;
    cr: RegExp;
    secondRun: boolean;
    verify?: boolean;
}
export declare const rules: Rule[];
