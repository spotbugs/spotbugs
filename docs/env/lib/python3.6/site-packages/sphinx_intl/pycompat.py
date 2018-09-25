# -*- coding: utf-8 -*-
"""
Python compatibility functions.
"""

import sys
import os
from six import PY3, exec_, b

FS_ENCODING = sys.getfilesystemencoding() or sys.getdefaultencoding()


if sys.version_info >= (2, 6):
    # Python >= 2.6
    relpath = os.path.relpath
else:
    from os.path import curdir

    def relpath(path, start=curdir):
        """Return a relative version of a path"""
        from os.path import sep, abspath, commonprefix, join, pardir

        if not path:
            raise ValueError("no path specified")

        start_list = abspath(start).split(sep)
        path_list = abspath(path).split(sep)

        # Work out how much of the filepath is shared by start and path.
        i = len(commonprefix([start_list, path_list]))

        rel_list = [pardir] * (len(start_list)-i) + path_list[i:]
        if not rel_list:
            return start
        return join(*rel_list)
    del curdir

if PY3:
    def convert_with_2to3(filepath):
        from lib2to3.refactor import RefactoringTool, get_fixers_from_package
        from lib2to3.pgen2.parse import ParseError
        fixers = get_fixers_from_package('lib2to3.fixes')
        refactoring_tool = RefactoringTool(fixers)
        source = refactoring_tool._read_python_source(filepath)[0]
        try:
            tree = refactoring_tool.refactor_string(source, 'conf.py')
        except ParseError:
            typ, err, tb = sys.exc_info()
            # do not propagate lib2to3 exceptions
            lineno, offset = err.context[1]
            # try to match ParseError details with SyntaxError details
            raise SyntaxError(err.msg, (filepath, lineno, offset, err.value))
        return str(tree)
else:
    convert_with_2to3 = None


def execfile_(filepath, _globals):
    # get config source -- 'b' is a no-op under 2.x, while 'U' is
    # ignored under 3.x (but 3.x compile() accepts \r\n newlines)
    f = open(filepath, 'rbU')
    try:
        source = f.read()
    finally:
        f.close()

    # py25,py26,py31 accept only LF eol instead of CRLF
    if sys.version_info[:2] in ((2, 5), (2, 6), (3, 1)):
        source = source.replace(b('\r\n'), b('\n'))

    # compile to a code object, handle syntax errors
    filepath_enc = filepath.encode(FS_ENCODING)
    try:
        code = compile(source, filepath_enc, 'exec')
    except SyntaxError:
        if convert_with_2to3:
            # maybe the file uses 2.x syntax; try to refactor to
            # 3.x syntax using 2to3
            source = convert_with_2to3(filepath)
            code = compile(source, filepath_enc, 'exec')
        else:
            raise

    exec_(code, _globals)
