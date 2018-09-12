# sentence-splitter

Split {Japanese, English} text into sentences.

## Installation

    npm install sentence-splitter

### CLI

    $ npm install -g sentence-splitter
    $ echo "This is a pen.But This is not pen" | sentence-splitter
    This is a pen.
    But This is not pen


## Usage

- `splitSentences(text, [options])`: `Node[]`

```js
import {split, Syntax} from "sentence-splitter";
let sentences = split("text.\n\ntext");
console.log(JSON.stringify(sentences, null, 4));
/*
[
    {
        "type": "Sentence",
        "raw": "text.",
        "value": "text.",
        "loc": {
            "start": {
                "line": 1,
                "column": 0
            },
            "end": {
                "line": 1,
                "column": 5
            }
        },
        "range": [
            0,
            5
        ]
    },
    {
        "type": "WhiteSpace",
        "raw": "\n",
        "value": "\n",
        "loc": {
            "start": {
                "line": 1,
                "column": 5
            },
            "end": {
                "line": 2,
                "column": 0
            }
        },
        "range": [
            5,
            6
        ]
    },
    {
        "type": "WhiteSpace",
        "raw": "\n",
        "value": "\n",
        "loc": {
            "start": {
                "line": 2,
                "column": 0
            },
            "end": {
                "line": 3,
                "column": 0
            }
        },
        "range": [
            6,
            7
        ]
    },
    {
        "type": "Sentence",
        "raw": "text",
        "value": "text",
        "loc": {
            "start": {
                "line": 3,
                "column": 0
            },
            "end": {
                "line": 3,
                "column": 4
            }
        },
        "range": [
            7,
            11
        ]
    }
]
*/

// with splitting char options
let sentences = split("text¶text", {
    separatorChars: ["¶"]
});
sentences.length; // 2
```

- `line`: start with **1**
- `column`: start with **0**

See more detail on [Why do `line` of location in JavaScript AST(ESTree) start with 1 and not 0?](https://gist.github.com/azu/8866b2cb9b7a933e01fe "Why do `line` of location in JavaScript AST(ESTree) start with 1 and not 0?")

### Options

- `separatorChars`
    - default: `[".", "。", "?", "!", "？", "！"]`
    - separator chars of sentences.
- `charRegExp` (**Deprecated**)
    - default: `/[\.。\?\!？！]/`
    - separator of sentences.
- `newLineCharacters`
    - default: `"\n"`
    - line break mark
    - if you treat Markdown text, set `newLineCharacters: "\n\n"` to this option

### Node's type

- `Sentence`: Sentence Node contain punctuation.
- `WhiteSpace`: WhiteSpace Node has `\n`.

Get these `Syntax` constants value from the module:

```js
import {Syntax} from "sentence-splitter";
console.log(Syntax.Sentence);// "Sentence"
````

### Treat Markdown break line

td:lr: set `newLineCharacters: "\n\n"` to option.

```js
let sentences = splitSentences(text, {
    newLineCharacters: "\n\n" // `\n\n` as a separator
});
```

`sentence-splitter` split text into `Sentence` and `WhiteSpace`

`sentence-splitter` following text to **3** Sentence and **3** WhiteSpace.

Some markdown parser take cognizance 1 Sentence + 1 WhiteSpace + 1Sentence as 1 Sentence.

```markdown
TextA
TextB

TextC
```

Output: 

```json
[
    {
        "type": "Sentence",
        "raw": "TextA",
    },
    {
        "type": "WhiteSpace",
        "raw": "\n",
    },
    {
        "type": "Sentence",
        "raw": "TextB",
    },
    {
        "type": "WhiteSpace",
        "raw": "\n",
    },
    {
        "type": "WhiteSpace",
        "raw": "\n",
    },
    {
        "type": "Sentence",
        "raw": "TextC",
    }
]
```


If you want to treat `\n\n` as a separator of sentences, can use `newLineCharacters` options.

```js
let text = `TextA
TextB
           
TextC`;
let sentences = split(text, {
    newLineCharacters: "\n\n" // `\n\n` as a separator
});
console.log(JSON.stringify(sentences, null, 4))
```

Output: 

```json
[
    {
        "type": "Sentence",
        "raw": "TextA\nTextB",
    },
    {
        "type": "WhiteSpace",
        "raw": "\n",
    },
    {
        "type": "WhiteSpace",
        "raw": "\n",
    },
    {
        "type": "Sentence",
        "raw": "TextC",
    }
]
```


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
