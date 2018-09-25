var format = require("./");

console.log(format('{blue} hello {green} world'));
console.log(format('{bold}{red} span {yellow} eggs {reset}'));
console.log(format('{undefined} {cyan} lorem ipsum {reset}'));
