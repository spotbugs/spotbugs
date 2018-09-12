// MIT © 2017 azu

export interface PatternMatchDictArgs {
    // index of match string first character
    index: number;
    // whole of match string
    match: string;
    // capture strings
    captures: string[];
    // all string
    all: string;
}

export interface TestMatchReplaceReturnDict {
    // match pattern
    // if the result of match `pattern` is falsy, return { ok: false }
    pattern: RegExp;
    // test the result of match
    // if the result of `test` is true, allow to replace if needed
    test?: (args: PatternMatchDictArgs) => boolean;
    // replace the result of match
    replace?: (args: PatternMatchDictArgs) => string;
    // Return the message if needed
    message?: (args: PatternMatchDictArgs) => string;
}

export interface MatchTestReplaceReturnResult {
    index: number;
    match: string;
    replace: string | undefined;
    message?: string;
}

export interface MatchTestReplaceReturn {
    ok: boolean;
    results: MatchTestReplaceReturnResult[];
}

/**
 * Apply ReturnResult order by last.
 */
const applyFixes = (text: string, messages: MatchTestReplaceReturnResult[]) => {
    // As as result, show diff
    const remainingMessages: MatchTestReplaceReturnResult[] = [];
    const cloneMessages = messages.slice();
    const fixes: MatchTestReplaceReturnResult[] = cloneMessages;
    let lastFixPos = text.length + 1;
    let prefix = "";
    if (fixes.length) {
        // reverse: index larger is last
        fixes.sort((a: MatchTestReplaceReturnResult, b: MatchTestReplaceReturnResult) => {
            if (a.index + a.match.length <= b.index) {
                return 1;
            } else {
                return -1;
            }
        });

        // split into array of characters for easier manipulation
        const chars = text.split("");

        fixes.forEach(problem => {
            // pickup fix range
            let start = problem.index;
            const end = start + problem.match.length;
            // no replace
            if (problem.replace === undefined) {
                return;
            }
            let insertionText = problem.replace;
            if (end <= lastFixPos) {
                // modify
                chars.splice(start, end - start, insertionText);
                lastFixPos = start;
            } else {
                remainingMessages.push(problem);
            }
        });

        return {
            fixed: true,
            messages: cloneMessages, // have order
            remainingMessages: remainingMessages, // have not order
            output: prefix + chars.join("")
        };
    } else {
        return {
            fixed: false,
            messages: cloneMessages,
            remainingMessages,
            output: prefix + text
        };
    }
};
/**
 * replace `text` with `results`.
 */
export const replaceAll = (text: string, results: MatchTestReplaceReturnResult[]): { ok: boolean; output: string } => {
    const fixes = applyFixes(text, results);
    return {
        ok: fixes.fixed,
        output: fixes.output
    };
};

/**
 * test `text`, match `text`, and replace `text.
 */
export const matchTestReplace = (text: string, dict: TestMatchReplaceReturnDict): MatchTestReplaceReturn => {
    // No match
    if (!dict.pattern.test(text)) {
        return {
            ok: false,
            results: []
        };
    }

    const results: MatchTestReplaceReturnResult[] = [];
    text.replace(dict.pattern, function replacer() {
        let isReplaceOK = true;
        // ...rest operator can do following
        // (match: string, ...captures: any[], offset: number, all: string) => string
        const match: string = arguments[0];
        const index: number = arguments[arguments.length - 2];
        const all: string = arguments[arguments.length - 1];
        const captures: string[] = Array.prototype.slice.call(arguments, 1, -2);
        const dictArgs: PatternMatchDictArgs = {
            index,
            match,
            captures,
            all
        };
        if (typeof dict.test === "function") {
            isReplaceOK = dict.test(dictArgs);
        }
        if (isReplaceOK) {
            const replace = typeof dict.replace === "function" ? dict.replace(dictArgs) : undefined;
            const message = dict.message ? dict.message(dictArgs) : undefined;
            results.push({
                index,
                match: match,
                replace: replace,
                message: message
            });
            // no replace
            if (replace === undefined) {
                return all;
            }
            return replace;
        } else {
            // No replace
            return all;
        }
    });
    return {
        ok: true,
        results
    };
};
