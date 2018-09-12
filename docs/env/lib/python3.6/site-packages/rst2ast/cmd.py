# -*- coding: utf-8 -*-

from . import writer
from docutils.core import default_description, publish_cmdline

def run():
    publish_cmdline(writer=writer.ASTWriter())
