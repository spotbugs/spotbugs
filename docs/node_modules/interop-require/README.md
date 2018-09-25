# interop-require

Require babel's ES6 modules from node.

With the release of [babel]`@6` the way generated modules work now does NOT set the `module.exports` property; instead it sets `exports.default`. Of course this basically breaks everything trying to `require()` it. So this tiny module is a replacement for `require()` for node-based runtimes that imports the default if an ES6 module is detected. A necessary evil it seems, given this behavior is not changing: https://github.com/babel/babel/issues/2212.

[babel]: https://github.com/babel/babel
