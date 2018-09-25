# @textlint-rule/textlint-rule-google-dashes

Reference:

- [Dashes  |  Google Developer Documentation Style Guide  |  Google Developers](https://developers.google.com/style/dashes "Dashes  |  Google Developer Documentation Style Guide  |  Google Developers")
- [Ranges of numbers](https://developers.google.com/style/numbers "Ranges of numbers")
- [punctuation - Em-dash vs colon: "Remind me: what's your name again?" or "Remind me—what's your name again?" - English Language & Usage Stack Exchange](https://english.stackexchange.com/questions/151971/em-dash-vs-colon-remind-me-whats-your-name-again-or-remind-me-whats-your "punctuation - Em-dash vs colon: &#34;Remind me: what&#39;s your name again?&#34; or &#34;Remind me—what&#39;s your name again?&#34; - English Language &amp; Usage Stack Exchange")
- [Dash - Wikipedia](https://en.wikipedia.org/wiki/Dash#Em_dash "Dash - Wikipedia")

## Install

Install with [npm](https://www.npmjs.com/):

    npm install @textlint-rule/textlint-rule-google-dashes

## Usage

Via `.textlintrc`(Recommended)

```json
{
    "rules": {
        "@textlint-rule/google-dashes": true
    }
}
```

Via CLI

```
textlint --rule @textlint-rule/google-dashes README.md
```

## TODO

- [ ] use colon instead of dash or hyphen
  - Currently, support partially


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
