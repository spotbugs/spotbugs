export function debugLog(...args: any[]) {
    if (process.env.DEBUG !== "sentence-splitter") {
        return;
    }
    console.log("sentence-splitter: ", ...args);
}
