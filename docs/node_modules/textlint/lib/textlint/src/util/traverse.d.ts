declare const fs: any;
declare const path: any;
declare const debug: any;
/**
 * Walks a path recursively calling the callback on each file.
 * @param {string} name The file or directory path.
 * @param {string[]} extensions The file extensions that should cause the callback
 *      to be called.
 * @param {Function} [exclude] The function to check if file/path should be excluded.
 * @param {Function} callback The function to call on each file.
 * @returns {void}
 * @private
 */
declare function walk(name: string, extensions: string[], exclude: Function, callback: Function): void;
