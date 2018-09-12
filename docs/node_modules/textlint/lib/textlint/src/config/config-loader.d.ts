import { TextLintModuleResolver } from "../engine/textlint-module-resolver";
/**
 * @param {string} configFilePath
 * @param {string} configFileName
 * @param {TextLintModuleResolver} moduleResolver
 * @returns {{ config: Object, filePath:string}}
 */
export declare function loadConfig(configFilePath: string, {configFileName, moduleResolver}: {
    configFileName: string;
    moduleResolver: TextLintModuleResolver;
}): {
    config: any;
    filePath: any;
};
