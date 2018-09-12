# textlint-util-to-string

Convert `Paragraph` Node to plain text with SourceMap.

SourceMap mean that could revert `position` in plain text to `position` in Node.

This library is for [textlint](https://github.com/textlint/textlint "textlint") and [textstat](https://github.com/azu/textstat "textstat").

## Installation

    npm install textlint-util-to-string

## Terminology

The concepts `position` and `index` are the same as those explained in [Constellation/structured-source](https://github.com/Constellation/structured-source).

**Note**: the `column` property of `position` as it is **0-based** index.

## Usage

### `Constructor(rootNode): source`

Return instance of Source.

## `originalIndexFromIndex(generatedIndex): number`

Get original index from generated index value

## `originalPositionFromPosition(position): Position`

Get original position from generated position

## `originalIndexFromPosition(generatedPosition): number`

Get original index from generated position

## `originalPositionFromIndex(generatedIndex): Position`

Get original position from generated index

```js
import assert from "power-assert"
import {parse} from "markdown-to-ast";
import StringSource from "textlint-util-to-string";

let originalText = "This is [Example！？](http://example.com/)";
let AST = parse(originalText);
let source = new StringSource(AST);
let result = source.toString();

// StringSource#toString returns a plain text
assert.equal(result, "This is Example！？");

// "Example" is located at the index 8 in the plain text
//  ^
let index1 = result.indexOf("Example");
assert.equal(index1, 8);

// The same "E" is located at the index 9 in the original text
assert.equal(source.originalIndexFromIndex(index1), 9);
assert.deepEqual(source.originalPositionFromPosition({
    line: 1,
    column: 8
}), {
    line: 1,
    column: 9
);

// Another example with "！", which is located at 15 in the plain text
// and at 16 in the original text
let index2 = result.indexOf("！？");
assert.equal(index2, 15);
assert.equal(source.originalIndexFromIndex(index2), 16);
```

## Examples

- [azu/textlint-rule-first-sentence-length: textlint rule that limit maximum length of First sentence of the section.](https://github.com/azu/textlint-rule-first-sentence-length)
- [azu/textlint-rule-en-max-word-count: textlint rule that specify the maximum word count of a sentence.](https://github.com/azu/textlint-rule-en-max-word-count)
- [nodaguti/textlint-rule-spellchecker: textlint rule to check spellings with native spellchecker](https://github.com/nodaguti/textlint-rule-spellchecker)


## FAQ

### Why return relative position from rootNode?

```js
let AST = ....
let rootNode = AST.children[10];
let source = new StringSource(rootNode);
source.originalIndexFor(0); // should be 0
```

To return relative position easy to compute position(We think).

One space has a single absolute position, The other should be relative position.

## Tests

    npm test

## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :D

## License

MIT
