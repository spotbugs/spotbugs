/**
 * Encapsulates all CLI behavior for eslint. Makes it easier to test as well as
 * for other Node.js programs to effectively run the CLI.
 */
export declare const cli: {
    execute(args: string | object | any[], text: string): Promise<number>;
    executeWithOptions(cliOptions: any, files: string[], text: string, stdinFilename: string): Promise<number>;
};
