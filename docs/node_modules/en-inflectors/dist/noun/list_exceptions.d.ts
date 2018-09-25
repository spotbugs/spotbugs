export interface NounConversionObject {
    [key: string]: Array<string>;
}
declare const singular2plural: NounConversionObject;
declare const plural2singular: NounConversionObject;
export { plural2singular, singular2plural };
