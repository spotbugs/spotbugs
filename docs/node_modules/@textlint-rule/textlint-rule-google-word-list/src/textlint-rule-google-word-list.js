// MIT © 2017 azu
"use strict";
import { paragraphReporter, getPosFromSingleWord } from "@textlint-rule/textlint-report-helper-for-google-preset";

const report = context => {
    // Politeness and use of "please"
    // https://developers.google.com/style/tone#politeness-and-use-of-please
    const dictionaries = [
        {
            word: "abort",
            message: "Don't use. Instead, use words like stop, exit, cancel, or end."
        },
        {
            word: "access",
            test: ({ all }) => {
                return /^VB/.test(getPosFromSingleWord(all)); // Verb
            },
            message:
                "Access(verb) – Avoid when you can, in favor of friendlier words like see, edit, find,\nuse, or view."
        },
        {
            word: "account name",
            replace: () => "username",
            message: "Don't use. Instead, use username."
        },
        {
            word: "actionable",
            message:
                'Avoid unless it\'s the clearest and simplest phrasing for your audience. If\nnot, then consider options such as leaving it out or replacing it with a phrase\nlike "useful" or "that you can act on." Don\'t use it in the legal sense without\nconsulting a lawyer.'
        },
        {
            word: "action bar",
            replace: () => "app bar",
            message: "Don't use. Instead, use app bar."
        },
        {
            word: "administrator",
            replace: () => "admin",
            message: 'Don\'t use. Instead, use "admin."'
        },
        // {
        //     "word": "& (ampersand)",
        //     "message": "In headings or text, don't use instead of \"and\"; however, it's OK to use\nin tables and UIs. And of course it's fine to use & for technical\npurposes in code."
        // },
        {
            word: "and so on",
            message: 'Avoid using "and so on" whenever possible. For more information, see etc.'
        },
        {
            word: "application",
            replace: () => "app",
            message: 'Don\'t use. Instead, use "app".'
        },
        {
            word: "authN",
            message: 'Don\'t use. Instead, use "authentication" or "authorization."'
        },
        {
            word: "authZ",
            message: 'Don\'t use. Instead, use "authentication" or "authorization."'
        },
        {
            word: "autoupdate",
            replace: () => "automatically update",
            message: 'Don\'t use. Instead, use "automatically update."'
        },
        {
            word: "cell phone",
            message:
                'Don\'t use. Instead, use "mobile" or "mobile phone" or (if you\'re talking\nabout more than just phones) "mobile device." Using "phone" (without "mobile")\nis fine when the context is clear.'
        },
        {
            word: "cellular data",
            replace: () => "mobile data",
            message: 'Don\'t use. Instead, use "mobile data."'
        },
        {
            word: "cellular network",
            replace: () => "mobile network",
            message: 'Don\'t use. Instead, use "mobile network."'
        },
        {
            word: /check (.*)/,
            test: ({ all }) => {
                return /checkbox/i.test(all);
            },
            replace: ({ captures }) => `select ${captures[0]}`,
            message: 'Don\'t use to refer to marking a checkbox. Instead, use "select."'
        },
        {
            word: "click here",
            message:
                "Don't use. For details and alternatives, see Link text. https://developers.google.com/style/link-text"
        },
        {
            word: "content type",
            replace: () => "media type",
            message: 'Don\'t use when referring to types such as "application/json"; instead,\nuse "media type."'
        },
        {
            word: "deselect",
            message: 'Don\'t use to refer to clearing a check mark from a checkbox. Instead, use\n"clear."'
        },
        {
            word: "Developers Console",
            message: 'Don\'t use. Instead, use "Google API Console" or "API Console."'
        },
        {
            word: "disable",
            message: 'Don\'t use. Instead, use "turn off" or "off."'
        },
        {
            word: "disabled",
            message: 'Don\'t use. Instead, use "turn off" or "off."'
        },
        {
            word: "easy",
            message: ""
        },
        {
            word: "e.g.",
            message:
                'Don\'t use. Instead, use phrases like "for example" or "for instance." Too\nmany people mix up "e.g." and "i.e."'
        },
        {
            word: "enable",
            message: 'Don\'t use. Instead, use "turn on" or "on."'
        },
        {
            word: "enabled",
            message: 'Don\'t use. Instead, use "turn on" or "on."'
        },
        // {
        //     "word": "gender-neutral he, him, or his (or she or\nher)",
        //     "message": "Don't use. Instead, use the singular \"they\" (see Jane Austen and other\nfamous authors violate what everyone learned in their English class). If\nyou can't stand that, then use \"he or she,\" or rewrite to avoid singular\ngendered pronouns. For example, using plurals can often help. (For more\nsuggestions, if you have access to the Chicago Manual of Style,\n16th edition, then see section 5.225, \"Nine techniques for achieving gender\nneutrality.\") Don't use \"he/she\" or \"(s)he\" or other such punctuational\napproaches."
        // },
        {
            word: "Googling",
            message: 'Don\'t use as a verb or gerund. Instead, use "search with Google."'
        },
        {
            word: "Google Developers Console",
            message: 'Don\'t use. Instead, use "Google API Console" or "API Console."'
        },
        {
            word: "grayed-out",
            message: 'Don\'t use. Instead, use "unavailable."'
        },
        {
            word: "hit",
            message: 'Don\'t use as a synonym for "click."'
        },
        {
            word: "i.e.",
            message: 'Don\'t use. Instead, use phrases like "that is." Too many people mix up\n"e.g." and "i.e."'
        },
        {
            word: "in order to",
            message:
                'If at all possible, don\'t use "in order to"; instead, use "to." Very\noccasionally, "in order to" does clarify meaning or make something easier to\nread.'
        },
        {
            word: "kill",
            message: 'Don\'t use. Instead, use words like "stop," "exit," "cancel," or "end."'
        },
        {
            word: "learnings",
            message: "Don't use."
        },
        // {
        //     word: 'let\'s (as a contraction of "let us")',
        //     message: "Don't use if at all possible."
        // },
        {
            word: "omnibox",
            replace: () => "address bar",
            message: 'Don\'t use. Instead, use "address bar."'
        },
        {
            word: "overview screen",
            replace: () => "recents screen",
            message: 'Don\'t use. Instead, use "recents screen."'
        },
        // {
        //     "word": "please: see tone",
        //     "message": ""
        // },
        {
            word: "Representational State Transfer",
            message:
                "Don't use. To people unfamiliar with REST, this acronym expansion is\nmeaningless; better to just refer to it as REST and don't bother trying to\nexplain what it theoretically stands for."
        },
        {
            word: "should",
            message:
                "Generally avoid." +
                'When telling the reader what to do, "should" implies recommended but optional, which leaves the reader unsure of what to do. Better to use "must" or just leave out the word "should."'
        },
        {
            word: "sign-on",
            message: 'Don\'t use either form on its own. Use the hyphenated version as part of\n"single sign-on."'
        },
        {
            word: "sign on",
            message: 'Don\'t use either form on its own. Use the hyphenated version as part of\n"single sign-on."'
        },
        {
            word: "simple",
            message: ""
        },
        {
            word: "smartphone",
            message: 'Don\'t use. Instead, use "mobile phone" or\n"phone."'
        },
        {
            word: "ssh'ing",
            message: 'Use alternatives to "ssh\'ing" unless there is just no way around it.'
        },
        {
            word: "tap & hold",
            replace: () => "touch & hold",
            message: 'Use "touch & hold" (not "touch and hold") instead. (Note the "&". It\'s OK\nto use in this case.)'
        },
        {
            word: "tap and hold",
            replace: () => "touch & hold",
            message: 'Use "touch & hold" (not "touch and hold") instead. (Note the "&". It\'s OK\nto use in this case.)'
        },
        {
            word: "terminate",
            message: "Don't use. Instead, use words like stop, exit, cancel, or end."
        },
        {
            word: /touch (.*?)/,
            test: ({ all }) => {
                if (/touch & hold/.test(all)) {
                    return false;
                }
                return true;
            },
            replace: ({ captures }) => `tap ${captures[0]}`,
            message: 'Don\'t use. Instead, use "tap." However, "touch & hold" is OK to use.'
        },
        {
            word: "uncheck",
            replace: () => "clear",
            message: 'Don\'t use to refer to clearing a check mark from a checkbox. Instead, use\n"clear."'
        },
        {
            word: "unselect",
            message: "Don't use."
        },
        {
            word: "vs.",
            replace: () => "versus.",
            message: 'Don\'t use "vs." as an abbreviation for "versus"; instead, use the unabbreviated "versus."'
        },
        {
            word: "voila",
            message: "Don't use."
        },
        {
            word: "World Wide Web",
            replace: () => "web",
            message: 'Don\'t use. Instead, use "web."'
        },
        {
            word: "zippy",
            message:
                "Don't use to refer to expander arrows,\nunless you're specifically referring to the Zippy\nwidget in Closure."
        }
    ].map(preDict => {
        return {
            pattern: typeof preDict.word === "string" ? new RegExp("\\b" + preDict.word + "\\b") : preDict.word,
            test: preDict.test ? preDict.test : undefined,
            replace: preDict.replace ? preDict.replace : undefined,
            message: () => preDict.message
        };
    });

    const { Syntax, RuleError, getSource, fixer, report } = context;
    return {
        [Syntax.Paragraph](node) {
            paragraphReporter({
                Syntax,
                node,
                dictionaries,
                report,
                getSource,
                RuleError,
                fixer
            });
        }
    };
};
module.exports = {
    linter: report,
    fixer: report
};
