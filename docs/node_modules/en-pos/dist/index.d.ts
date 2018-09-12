export interface MetaObject {
    pos?: any;
    tag?: any;
    yearlyRange?: any;
    time?: any;
    ratio?: any;
    timeIndicator?: any;
    order?: any;
    hyphenedOrder?: any;
    number?: any;
    abbrev?: any;
    properNoun?: any;
    acronym?: any;
    meta?: any;
}
export interface Result {
    tokens: Array<string>;
    tags: Array<string>;
    confidence: Array<number>;
    smooth: () => Result;
    initial: () => Result;
}
declare class Tag {
    tokens: Array<string>;
    tags: Array<string>;
    confidence: Array<number>;
    meta: Array<MetaObject>;
    blocked: Array<boolean>;
    constructor(tokens: Array<string>, meta?: Array<MetaObject>);
    initial: () => Result;
    smooth: () => Result;
    private _PreBrill;
    private _applyBrillRule;
    private _Brill;
    private _PostBrill;
}
export { Tag };
