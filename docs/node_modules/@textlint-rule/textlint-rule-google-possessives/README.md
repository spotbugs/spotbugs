# @textlint-rule/textlint-rule-google-possessives

Reference [Possessives  |  Google Developer Documentation Style Guide  |  Google Developers](https://developers.google.com/style/possessives "Possessives  |  Google Developer Documentation Style Guide  |  Google Developers")

## Notes

- NG: singular noun + `'`
- NG: plural word + `'s`
- OK: plural word without ended "s" + `'s`

## Install

Install with [npm](https://www.npmjs.com/):

    npm install @textlint-rule/textlint-rule-google-possessives

## Usage

Via `.textlintrc`(Recommended)

```json
{
    "rules": {
        "@textlint-rule/google-possessives": true
    }
}
```

Via CLI

```
textlint --rule @textlint-rule/google-possessives README.md
```


## Changelog

See [Releases page](https://github.com/textlint-rule/textlint-rule-preset-google/releases).

## Running tests

Install devDependencies and Run `npm test`:

    npm i -d && npm test

## Contributing

Pull requests and stars are always welcome.

For bugs and feature requests, [please create an issue](https://github.com/textlint-rule/textlint-rule-preset-google/issues).

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
