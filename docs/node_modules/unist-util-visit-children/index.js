'use strict'

/* Expose. */
module.exports = visitorFactory

/* Turns `callback` into a child-visitor accepting a parent. */
function visitorFactory(callback) {
  return visitor

  /* Visit `parent`, invoking `callback` for each child. */
  function visitor(parent) {
    var index = -1
    var children = parent && parent.children

    if (!children) {
      throw new Error('Missing children in `parent` for `visitor`')
    }

    while (++index in children) {
      callback(children[index], index, parent)
    }
  }
}
