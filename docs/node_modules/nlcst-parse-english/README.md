# nlcst-parse-english

Parse english with Part-of-speech(POS) tagged and output [NLCST](https://github.com/syntax-tree/nlcst).

## Install

Install with [npm](https://www.npmjs.com/):

    npm install nlcst-parse-english

## Part-of-speech(POS)

`WordNode` has `{ data: { pos: Annotation } }`.

This library use [parse-english](https://github.com/wooorm/parse-english "parse-english") + [FinNLP/en-pos](https://github.com/finnlp/en-pos "FinNLP/en-pos: ⚙️ [Processor] A better English POS tagger written in JavaScript").

Annotation | Name | Example
--- | --- | ---
**`NN`** | Noun | `dog` `man`
**`NNS`** | Plural noun | `dogs` `men`
**`NNP`** | Proper noun | `London` `Alex`
**`NNPS`** | Plural proper noun | `Smiths`
**`VB`** | Base form verb | `be`
**`VBP`** | Present form verb | `throw`
**`VBZ`** | Present form (3rd person) | `throws`
**`VBG`** | Gerund form verb | `throwing`
**`VBD`** | Past tense verb | `threw`
**`VBN`** | Past participle verb | `thrown`
**`MD`** | Modal verb | `can` `shall` `will` `may` `must` `ought`
**`JJ`** | Adjective | `big` `fast`
**`JJR`** | Comparative adjective | `bigger`
**`JJS`** | Superlative adjective | `biggest`
**`RB`** | Adverb | `not` `quickly` `closely`
**`RBR`** | Comparative adverb | `less-closely` `faster`
**`RBS`** | Superlative adverb | `fastest`
**`DT`** | Determiner | `the` `a` `some` `both`
**`PDT`** | Predeterminer | `all` `quite`
**`PRP`** | Personal Pronoun | `I` `you` `he` `she`
**`PRP$`** | Possessive Pronoun | `I` `you` `he` `she`
**`POS`** | Possessive ending | `'s`
**`IN`** | Preposition | `of` `by` `in`
**`PR`** | Particle | `up` `off`
**`TO`** | *to* | `to`
**`WDT`** | Wh-determiner | `which` `that` `whatever` `whichever`
**`WP`** | Wh-pronoun | `who` `whoever` `whom` `what`
**`WP$`** | Wh-possessive | `whose`
**`WRB`** | Wh-adverb | `how` `where` 
**`EX`** | Expletive there | `there`
**`CC`** | Coordinating conjugation | `&` `and` `nor` `or`
**`CD`** | Cardinal Numbers | `1` `7` `77` `one`
**`LS`** | List item marker | `1` `B` `C` `One`
**`UH`** | Interjection | `ah` `oh` `oops`
**`FW`** | Foreign Words | `viva` `mon` `toujours`
**`,`** | Comma | `,`
**`:`** |Mid-sent punct | `:` `;` `...`
**`.`** | Sent-final punct. | `.` `!` `?`
**`(`** | Left parenthesis | `)` `}` `]`
**`)`** | Right parenthesis | `(` `{` `[`
**`#`** | Pound sign | `#`
**`$`** | Currency symbols | `$` `€` `£` `¥`
**`SYM`** | Other symbols | `+` `*` `/` `<` `>`
**`EM`** | Emojis & emoticons | `:)` `❤`

## Usage

```ts
const parser = new EnglishParser();
const CST = parser.parse("Mr. Henry Brown: A hapless but friendly City of London worker.");
assert.deepEqual(CST, {
    "type": "RootNode",
    "children": [
        {
            "type": "ParagraphNode",
            "children": [
                {
                    "type": "SentenceNode",
                    "children": [
                        {
                            "type": "WordNode",
                            "children": [
                                {
                                    "type": "TextNode",
                                    "value": "Mr",
                                    "position": {
                                        "start": {
                                            "line": 1,
                                            "column": 1,
                                            "offset": 0
                                        },
                                        "end": {
                                            "line": 1,
                                            "column": 3,
                                            "offset": 2
                                        }
                                    }
                                },
                                {
                                    "type": "PunctuationNode",
                                    "value": ".",
                                    "position": {
                                        "start": {
                                            "line": 1,
                                            "column": 3,
                                            "offset": 2
                                        },
                                        "end": {
                                            "line": 1,
                                            "column": 4,
                                            "offset": 3
                                        }
                                    }
                                }
                            ],
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 1,
                                    "offset": 0
                                },
                                "end": {
                                    "line": 1,
                                    "column": 4,
                                    "offset": 3
                                }
                            },
                            "data": {
                                "pos": "NNP"
                            }
                        },
                        {
                            "type": "WhiteSpaceNode",
                            "value": " ",
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 4,
                                    "offset": 3
                                },
                                "end": {
                                    "line": 1,
                                    "column": 5,
                                    "offset": 4
                                }
                            }
                        },
                        {
                            "type": "WordNode",
                            "children": [
                                {
                                    "type": "TextNode",
                                    "value": "Henry",
                                    "position": {
                                        "start": {
                                            "line": 1,
                                            "column": 5,
                                            "offset": 4
                                        },
                                        "end": {
                                            "line": 1,
                                            "column": 10,
                                            "offset": 9
                                        }
                                    }
                                }
                            ],
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 5,
                                    "offset": 4
                                },
                                "end": {
                                    "line": 1,
                                    "column": 10,
                                    "offset": 9
                                }
                            },
                            "data": {
                                "pos": "NNP"
                            }
                        },
                        {
                            "type": "WhiteSpaceNode",
                            "value": " ",
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 10,
                                    "offset": 9
                                },
                                "end": {
                                    "line": 1,
                                    "column": 11,
                                    "offset": 10
                                }
                            }
                        },
                        {
                            "type": "WordNode",
                            "children": [
                                {
                                    "type": "TextNode",
                                    "value": "Brown",
                                    "position": {
                                        "start": {
                                            "line": 1,
                                            "column": 11,
                                            "offset": 10
                                        },
                                        "end": {
                                            "line": 1,
                                            "column": 16,
                                            "offset": 15
                                        }
                                    }
                                }
                            ],
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 11,
                                    "offset": 10
                                },
                                "end": {
                                    "line": 1,
                                    "column": 16,
                                    "offset": 15
                                }
                            },
                            "data": {
                                "pos": "NNP"
                            }
                        },
                        {
                            "type": "PunctuationNode",
                            "value": ":",
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 16,
                                    "offset": 15
                                },
                                "end": {
                                    "line": 1,
                                    "column": 17,
                                    "offset": 16
                                }
                            },
                            "data": {
                                "pos": ":"
                            }
                        },
                        {
                            "type": "WhiteSpaceNode",
                            "value": " ",
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 17,
                                    "offset": 16
                                },
                                "end": {
                                    "line": 1,
                                    "column": 18,
                                    "offset": 17
                                }
                            }
                        },
                        {
                            "type": "WordNode",
                            "children": [
                                {
                                    "type": "TextNode",
                                    "value": "A",
                                    "position": {
                                        "start": {
                                            "line": 1,
                                            "column": 18,
                                            "offset": 17
                                        },
                                        "end": {
                                            "line": 1,
                                            "column": 19,
                                            "offset": 18
                                        }
                                    }
                                }
                            ],
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 18,
                                    "offset": 17
                                },
                                "end": {
                                    "line": 1,
                                    "column": 19,
                                    "offset": 18
                                }
                            },
                            "data": {
                                "pos": "DT"
                            }
                        },
                        {
                            "type": "WhiteSpaceNode",
                            "value": " ",
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 19,
                                    "offset": 18
                                },
                                "end": {
                                    "line": 1,
                                    "column": 20,
                                    "offset": 19
                                }
                            }
                        },
                        {
                            "type": "WordNode",
                            "children": [
                                {
                                    "type": "TextNode",
                                    "value": "hapless",
                                    "position": {
                                        "start": {
                                            "line": 1,
                                            "column": 20,
                                            "offset": 19
                                        },
                                        "end": {
                                            "line": 1,
                                            "column": 27,
                                            "offset": 26
                                        }
                                    }
                                }
                            ],
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 20,
                                    "offset": 19
                                },
                                "end": {
                                    "line": 1,
                                    "column": 27,
                                    "offset": 26
                                }
                            },
                            "data": {
                                "pos": "JJ"
                            }
                        },
                        {
                            "type": "WhiteSpaceNode",
                            "value": " ",
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 27,
                                    "offset": 26
                                },
                                "end": {
                                    "line": 1,
                                    "column": 28,
                                    "offset": 27
                                }
                            }
                        },
                        {
                            "type": "WordNode",
                            "children": [
                                {
                                    "type": "TextNode",
                                    "value": "but",
                                    "position": {
                                        "start": {
                                            "line": 1,
                                            "column": 28,
                                            "offset": 27
                                        },
                                        "end": {
                                            "line": 1,
                                            "column": 31,
                                            "offset": 30
                                        }
                                    }
                                }
                            ],
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 28,
                                    "offset": 27
                                },
                                "end": {
                                    "line": 1,
                                    "column": 31,
                                    "offset": 30
                                }
                            },
                            "data": {
                                "pos": "CC"
                            }
                        },
                        {
                            "type": "WhiteSpaceNode",
                            "value": " ",
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 31,
                                    "offset": 30
                                },
                                "end": {
                                    "line": 1,
                                    "column": 32,
                                    "offset": 31
                                }
                            }
                        },
                        {
                            "type": "WordNode",
                            "children": [
                                {
                                    "type": "TextNode",
                                    "value": "friendly",
                                    "position": {
                                        "start": {
                                            "line": 1,
                                            "column": 32,
                                            "offset": 31
                                        },
                                        "end": {
                                            "line": 1,
                                            "column": 40,
                                            "offset": 39
                                        }
                                    }
                                }
                            ],
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 32,
                                    "offset": 31
                                },
                                "end": {
                                    "line": 1,
                                    "column": 40,
                                    "offset": 39
                                }
                            },
                            "data": {
                                "pos": "JJ"
                            }
                        },
                        {
                            "type": "WhiteSpaceNode",
                            "value": " ",
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 40,
                                    "offset": 39
                                },
                                "end": {
                                    "line": 1,
                                    "column": 41,
                                    "offset": 40
                                }
                            }
                        },
                        {
                            "type": "WordNode",
                            "children": [
                                {
                                    "type": "TextNode",
                                    "value": "City",
                                    "position": {
                                        "start": {
                                            "line": 1,
                                            "column": 41,
                                            "offset": 40
                                        },
                                        "end": {
                                            "line": 1,
                                            "column": 45,
                                            "offset": 44
                                        }
                                    }
                                }
                            ],
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 41,
                                    "offset": 40
                                },
                                "end": {
                                    "line": 1,
                                    "column": 45,
                                    "offset": 44
                                }
                            },
                            "data": {
                                "pos": "NNP"
                            }
                        },
                        {
                            "type": "WhiteSpaceNode",
                            "value": " ",
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 45,
                                    "offset": 44
                                },
                                "end": {
                                    "line": 1,
                                    "column": 46,
                                    "offset": 45
                                }
                            }
                        },
                        {
                            "type": "WordNode",
                            "children": [
                                {
                                    "type": "TextNode",
                                    "value": "of",
                                    "position": {
                                        "start": {
                                            "line": 1,
                                            "column": 46,
                                            "offset": 45
                                        },
                                        "end": {
                                            "line": 1,
                                            "column": 48,
                                            "offset": 47
                                        }
                                    }
                                }
                            ],
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 46,
                                    "offset": 45
                                },
                                "end": {
                                    "line": 1,
                                    "column": 48,
                                    "offset": 47
                                }
                            },
                            "data": {
                                "pos": "IN"
                            }
                        },
                        {
                            "type": "WhiteSpaceNode",
                            "value": " ",
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 48,
                                    "offset": 47
                                },
                                "end": {
                                    "line": 1,
                                    "column": 49,
                                    "offset": 48
                                }
                            }
                        },
                        {
                            "type": "WordNode",
                            "children": [
                                {
                                    "type": "TextNode",
                                    "value": "London",
                                    "position": {
                                        "start": {
                                            "line": 1,
                                            "column": 49,
                                            "offset": 48
                                        },
                                        "end": {
                                            "line": 1,
                                            "column": 55,
                                            "offset": 54
                                        }
                                    }
                                }
                            ],
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 49,
                                    "offset": 48
                                },
                                "end": {
                                    "line": 1,
                                    "column": 55,
                                    "offset": 54
                                }
                            },
                            "data": {
                                "pos": "NNP"
                            }
                        },
                        {
                            "type": "WhiteSpaceNode",
                            "value": " ",
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 55,
                                    "offset": 54
                                },
                                "end": {
                                    "line": 1,
                                    "column": 56,
                                    "offset": 55
                                }
                            }
                        },
                        {
                            "type": "WordNode",
                            "children": [
                                {
                                    "type": "TextNode",
                                    "value": "worker",
                                    "position": {
                                        "start": {
                                            "line": 1,
                                            "column": 56,
                                            "offset": 55
                                        },
                                        "end": {
                                            "line": 1,
                                            "column": 62,
                                            "offset": 61
                                        }
                                    }
                                }
                            ],
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 56,
                                    "offset": 55
                                },
                                "end": {
                                    "line": 1,
                                    "column": 62,
                                    "offset": 61
                                }
                            },
                            "data": {
                                "pos": "NN"
                            }
                        },
                        {
                            "type": "PunctuationNode",
                            "value": ".",
                            "position": {
                                "start": {
                                    "line": 1,
                                    "column": 62,
                                    "offset": 61
                                },
                                "end": {
                                    "line": 1,
                                    "column": 63,
                                    "offset": 62
                                }
                            },
                            "data": {
                                "pos": "."
                            }
                        }
                    ],
                    "position": {
                        "start": {
                            "line": 1,
                            "column": 1,
                            "offset": 0
                        },
                        "end": {
                            "line": 1,
                            "column": 63,
                            "offset": 62
                        }
                    }
                }
            ],
            "position": {
                "start": {
                    "line": 1,
                    "column": 1,
                    "offset": 0
                },
                "end": {
                    "line": 1,
                    "column": 63,
                    "offset": 62
                }
            }
        }
    ],
    "position": {
        "start": {
            "line": 1,
            "column": 1,
            "offset": 0
        },
        "end": {
            "line": 1,
            "column": 63,
            "offset": 62
        }
    }
});

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
