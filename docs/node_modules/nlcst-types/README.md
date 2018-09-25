# nlcst-types

[NLCST](https://github.com/syntax-tree/nlcst) types for TypeScript.

## Install

Install with [npm](https://www.npmjs.com/):

    npm install nlcst-types

## Usage

```ts
import {
    Root,
    Paragraph,
    Sentence,
    Word,
    Text,
    Symbol,
    Punctuation,
    WhiteSpace,
    Source,
    TextNode
} from "nlcst-types";
// Type Guard
// https://basarat.gitbooks.io/typescript/docs/types/typeGuard.html
import {
    isRoot,
    isParagraph,
    isSentence,
    isWord,
    isText,
    isSymbol,
    isPunctuation,
    isWhiteSpace,
    isSource,
    isTextNode
} from "nlcst-types";
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
