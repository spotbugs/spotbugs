# unist-types

Unist for TypeScript.

## Install

Install with [npm](https://www.npmjs.com/):

    npm install unist-types

## Usage

```ts

import * as Unist from "unist-types";
const AST: Unist.Parent = {
    type: "Root",
    children: [
        {
            type: "String",
            data: {
                myData: "string"
            }
        },
        {
            type: "String-position",
            position: {
                start: {
                    line: 1,
                    column: 1,
                    offset: 0
                },
                end: {
                    line: 1,
                    column: 2,
                    offset: 1
                },
                index: 1
            }
        }
    ]
};

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
