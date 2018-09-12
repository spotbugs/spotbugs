# match-test-replace

Easy text pattern match and replace text.

This library does Match -> Test -> Replace pattern.

1. Match `pattern`
2. Does `test`
3. Does `replace`

## Install

Install with [npm](https://www.npmjs.com/):

    npm install match-test-replace

## Usage

```ts
export interface PatternMatchDictArgs {
    index: number;
    match: string;
    captures: string[];
    all: string;
}
export interface TestMatchReplaceReturnDict {
    pattern: RegExp;
    replace: (args: PatternMatchDictArgs) => string;
    test?: (args: PatternMatchDictArgs) => boolean;
    message?: (args: PatternMatchDictArgs) => string;
}
export interface TestMatchReplaceReturnResult {
    index: number;
    match: string;
    replace: string;
    message?: string;
}
export interface TestMatchReplaceReturn {
    ok: boolean;
    results: TestMatchReplaceReturnResult[];
}
/**
 * replace `text` with `results`.
 */
export declare const replaceAll: (
    text: string,
    results: TestMatchReplaceReturnResult[]
) => {
    ok: boolean;
    output: string;
};
/**
 * test `text`, match `text`, and replace `text.
 */
export declare const testMatchReplace: (text: string, dict: TestMatchReplaceReturnDict) => TestMatchReplaceReturn;
```


### Match -> Replace

```js
import { replaceAll, matchTestReplace } from "match-test-replace";
const text = "Hello";
const res = matchTestReplace(text, {
    pattern: /hello/i,
    replace: () => "Hello"
});
assert.ok(res.ok, "should be ok: true");
assert.strictEqual(res.results.length, 1, "1 replace");
/**
[ { index: 0, match: 'Hello', replace: 'Hello', message: undefined } ]
*/
```

### Match -> test -> Replace

match-test-replace not replace if `test` return `false`

```js
import { replaceAll, matchTestReplace } from "match-test-replace";
const text = "webkit is matched,but node-webkit is not match";
const res = matchTestReplace(text, {
    pattern: /(\S*?)webkit/g,
    replace: () => "WebKit",
    test: ({ captures }) => {
        return captures[0] !== "node-";
    }
});
assert.ok(res.ok === true, "should be ok: false");
assert.strictEqual(res.results.length, 1, "no replace");
assert.strictEqual(replaceAll(text, res.results).output, "WebKit is matched,but node-webkit is not match");
```

### Complex

```js
import * as assert from "assert";
import { replaceAll, matchTestReplace } from "match-test-replace";
import { PatternMatcher } from "nlcst-pattern-match";
import { EnglishParser } from "nlcst-parse-english";
const englishParser = new EnglishParser();
const matcher = new PatternMatcher({ parser: englishParser });
// https://developers.google.com/style/clause-order
// NG: Click Delete if you want to delete the entire document.
// OK: To delete the entire document, click Delete.
const text = 'Click Delete if you want to delete the entire document.';
const res = matchTestReplace(text, {
    pattern: /Click (\w+) if you want to (.+)./,
    replace: ({ captures }) => {
        console.log(captures);
        return `To ${captures[1]}, click ${captures[0]}.`
    },
    test: ({ all }) => {
        const pattern = matcher.tag`Click ${{
            type: "WordNode",
            data: {
                // Verb
                pos: /^VB/
            }
        }}`;
        return matcher.test(all, pattern);
    }
});
assert.ok(res.ok === true, "should be ok: true");
const output = replaceAll(text, res.results).output;
assert.strictEqual(output, "To delete the entire document, click Delete.");
```

## Changelog

See [Releases page](https://github.com/azu/nlp-pattern-match/releases).

## Running tests

Install devDependencies and Run `npm test`:

    npm i -d && npm test

## Contributing

Pull requests and stars are always welcome.

For bugs and feature requests, [please create an issue](https://github.com/azu/nlp-pattern-match/issues).

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :D

## Author

- [github/azu](https://github.com/azu)
- [twitter/azu_re](https://twitter.com/azu_re)

## License

MIT © azu
