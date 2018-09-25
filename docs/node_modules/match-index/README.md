# match-index [![Build Status](https://travis-ci.org/azu/match-index.svg?branch=master)](https://travis-ci.org/azu/match-index)

Get index of each capture.

- [Get index of each capture in a JavaScript regex - Stack Overflow](http://stackoverflow.com/questions/15934353/get-index-of-each-capture-in-a-javascript-regex)
- [tc39/String.prototype.matchAll: ES Proposal, specs, tests, reference implementation, and polyfill/shim for String.prototype.matchAll](https://github.com/tc39/String.prototype.matchAll#rationale)

## Why?

You want to match a regex like `/(a).(b)(c.)d/` with "aabccde", and get the following information back:

    "a" at index = 0
    "b" at index = 2
    "cc" at index = 3

But, it is difficult to write.

`match-index` provide `matchCaptureGroupAll` function that easy to get this information!

```js
const text = "aabccde";
const regExp = /(a).(b)(c.)d/;
const captureGroups = matchCaptureGroupAll(text, regExp);
// array of `MatchCaptureGroup`
assert.equal(captureGroups.length, 3);
const [a, b, c]= captureGroups;
assert.equal(a.text, "a");
assert.equal(a.index, 1);
assert.equal(b.text, "b");
assert.equal(b.index, 2);
assert.equal(c.text, "cc");
assert.equal(c.index, 3);
```

## Installation

    npm install match-index

## Usage

`match-index` provide two functions

### `matchCaptureGroupAll(text, regExp): MatchCaptureGroup`

Retrieves the captured matches when matching a string against a regular expression.

Example of `matchCaptureGroupAll()`

```js
// get "ABC" and "EFC that are captured by ( and )
const captureGroups = matchCaptureGroupAll("ABC EFG", /(ABC).*?(EFG)/);
// captureGroups is array of MatchAllGroup
/**
 * @typedef {Object} MatchAllGroup
 * @property {Array} all
 * @property {string} input
 * @property {number} index
 * @property {MatchCaptureGroup[]} captureGroups
 */
assert(captureGroups.length, 2);
const [x, y] = captureGroups;
assert.equal(x.text, "ABC");
assert.equal(x.index, 0);
assert.equal(y.text, "EFG");
assert.equal(y.index, 4);
```

`matchCaptureGroupAll` use `matchAll` in internal.

### `matchAll(text, regExp): MatchAllGroup`

Retrieves the matches all when matching a string against a regular expression.

Example of `matchAll()`

```js
const text = 'test1test2';
const regexp = /t(e)(st(\d?))/g;
const captureGroups = matchAll(text, regexp);
// captureGroups is array of `MatchAllGroup`
/**
 * @typedef {Object} MatchAllGroup
 * @property {Array} all
 * @property {string} input
 * @property {number} index
 * @property {MatchCaptureGroup[]} captureGroups
 */

assert.equal(captureGroups.length, 2);
const [test1, test2] = captureGroups;
assert.equal(test1.index, 0);
assert.equal(test1.input, text);
assert.deepEqual(test1.all, ['test1', 'e', 'st1', '1']);
assert.deepEqual(test1.captureGroups, [
    {
        index: 1,
        text: 'e'
    }, {
        index: 2,
        text: 'st1'
    }, {
        index: -1,// Limitation of capture nest
        text: '1'
    }
]);
assert.equal(test2.index, 5);
assert.equal(test2.input, text);
assert.deepEqual(test2.all, ['test2', 'e', 'st2', '2']);
assert.deepEqual(test2.captureGroups, [
    {
        index: 6,
        text: 'e'
    }, {
        index: 7,
        text: 'st2'
    }, {
        index: -1, // Limitation
        text: '2'
    }
]);
```

## Notes

### Limitation :warning:

`matchAll` and `matchCaptureGroupAll` doesn't support nest capture.

e.g.) last captureGroups item's `index` is wrong result.

`(st(\d?))` is nest capture.

```js
const text = 'test1test2';
const regexp = /t(e)(st(\d?))/g;
const captureGroups = matchAll(text, regexp);
// captureGroups is array of `MatchAllGroup`
/**
 * @typedef {Object} MatchAllGroup
 * @property {Array} all
 * @property {string} input
 * @property {number} index
 * @property {MatchCaptureGroup[]} captureGroups
 */

assert.equal(captureGroups.length, 2);
const [test1, test2] = captureGroups;
assert.equal(test1.index, 0);
assert.equal(test1.input, text);
assert.deepEqual(test1.all, ['test1', 'e', 'st1', '1']);
assert.deepEqual(test1.captureGroups, [
    {
        index: 1,
        text: 'e'
    }, {
        index: 2,
        text: 'st1'
    }, {
        index: -1,// Limitation of capture nest
        text: '1'
    }
]);
```

Welcome to pull request to fix this limitation :)

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