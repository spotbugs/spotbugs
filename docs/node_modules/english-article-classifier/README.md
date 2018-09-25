# english-article-classifier [![Build Status](https://travis-ci.org/azu/english-article-classifier.svg?branch=master)](https://travis-ci.org/azu/english-article-classifier)

Classifier english article(a, an).

Based on:

- [rigoneri/indefinite-article.js: JavaScript function that returns the indefinite articles "a" or "an" based on a given word or phrase.](https://github.com/rigoneri/indefinite-article.js)
- [gitpan/Lingua-EN-Inflexion: Read-only release history for Lingua-EN-Inflexion](https://github.com/gitpan/Lingua-EN-Inflexion)
- [rossmeissl/indefinite_article: Indefinite article identification for Ruby strings](https://github.com/rossmeissl/indefinite_article)

## Feature

This library Classifier english word to 

- `a`
- `an`
- `unknown`

### Why "unknown"

Whether to use "a" or "an" depends on the pronunciation of the word that follows it.
Use "a" before any consonant sound; use "an" before any vowel sound.

So, We can not define `a` or `an` for a new word.

We want to avoid false-positive.

If you want to defined new word, please specify `option`.

Or

Please pull request to [a.ts](./src/a.ts) or [an.ts](./src/an.ts).

## Install

Install with [npm](https://www.npmjs.com/):

    npm install english-article-classifier

## Usage

### API

```ts
export interface ReturnClassifyArticle {
    type: "a" | "an" | "unknown";
    reason: string;
}
export interface classifyArticleOptions {
    forceA?: string[];
    forceAn?: string[];
}
export declare function classifyArticle(phrase: string, options?: classifyArticleOptions): ReturnClassifyArticle;
```

`classifyArticle` return an object that has `type` and `reason`. 
 
### Example

```js
"use strict";
const {classifyArticle} = require("./lib/english-article-classifier.js");
console.log(classifyArticle("hour"));
/*
{ type: 'an',
  reason: 'Specific start of words that should be proceeded by \'an\'' }
 */
console.log(classifyArticle("union"));
/*
{ type: 'a',
  reason: 'Special cases where a word that begins with a vowel should be proceeded by \'a\'' }
 */
console.log(classifyArticle("word"));
/*
{ type: 'a',
  reason: 'Other words that begins with a vowel should be proceeded by \'a\'' }
 */
console.log(classifyArticle("ZXCVBNM", {
    forceA: ["ZXCVBNM"]
}));
/*
{ type: 'a',
  reason: 'User defined words that should be proceeded by \'a\'' }
 */
console.log(classifyArticle("ZXCVBNM", {
    forceAn: ["ZXCVBNM"]
}));
/*
{ type: 'an',
  reason: 'User defined words that should be proceeded by \'an\'' }
 */
```

## Changelog

See [Releases page](https://github.com/azu/english-article-classifier/releases).

## Running tests

Install devDependencies and Run `npm test`:

    npm i -d && npm test

## Contributing

Pull requests and stars are always welcome.

For bugs and feature requests, [please create an issue](https://github.com/azu/english-article-classifier/issues).

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
