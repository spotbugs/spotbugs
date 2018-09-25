# @textlint-rule/textlint-rule-google-sentence-spacing

Reference: [Spaces between sentences  |  Google Developer Documentation Style Guide  |  Google Developers](https://developers.google.com/style/sentence-spacing "Spaces between sentences  |  Google Developer Documentation Style Guide  |  Google Developers")

## Example

**OK**:

```
sentence is 1. sentence 2. sentence 3.

> sentence is 1.     sentence 2.

This is ` obj.code   =    1;  `.

[   .   .   . link is ok    .     .     . ](http://example.com)

# Allow first and last spaces 

  sentence is 1. sentence 2. sentence 3.
sentence is 1. sentence 2. sentence 3.  
  sentence is 1. sentence 2. sentence 3.  
```

**NG**:

```
There are two sentence.  But have two space.
sentence is 1.  sentence 2.  sentence 3.
```



## Install

Install with [npm](https://www.npmjs.com/):

    npm install @textlint-rule/textlint-rule-google-sentence-spacing

## Usage

Via `.textlintrc`(Recommended)

```json
{
    "rules": {
        "@textlint-rule/google-sentence-spacing": true
    }
}
```

Via CLI

```
textlint --rule @textlint-rule/google-sentence-spacing README.md
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
