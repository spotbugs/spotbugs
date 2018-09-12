# -*- coding: utf-8 -*-

import os
import io

from babel.messages import pofile, mofile


def load_po(filename):
    """read po/pot file and return catalog object

    :param unicode filename: path to po/pot file
    :return: catalog object
    """
    # pre-read to get charset
    with io.open(filename, 'rb') as f:
        cat = pofile.read_po(f)
    charset = cat.charset or 'utf-8'

    # To decode lines by babel, read po file as binary mode and specify charset for
    # read_po function.
    with io.open(filename, 'rb') as f:  # FIXME: encoding VS charset
        return pofile.read_po(f, charset=charset)


def dump_po(filename, catalog):
    """write po/pot file from catalog object

    :param unicode filename: path to po file
    :param catalog: catalog object
    :return: None
    """
    dirname = os.path.dirname(filename)
    if not os.path.exists(dirname):
        os.makedirs(dirname)

    # Because babel automatically encode strings, file should be open as binary mode.
    with io.open(filename, 'wb') as f:
        pofile.write_po(f, catalog)


def write_mo(filename, catalog):
    """write mo file from catalog object

    :param unicode filename: path to mo file
    :param catalog: catalog object
    :return: None
    """
    dirname = os.path.dirname(filename)
    if not os.path.exists(dirname):
        os.makedirs(dirname)
    with io.open(filename, 'wb') as f:
        mofile.write_mo(f, catalog)


def translated_entries(catalog):
    return [m for m in catalog if m.id and m.string]


def fuzzy_entries(catalog):
    return [m for m in catalog if m.id and m.fuzzy]


def untranslated_entries(catalog):
    return [m for m in catalog if m.id and not m.string]


def update_with_fuzzy(catalog, catalog_source):
    """update catalog by template catalog with fuzzy flag.

    :param catalog: catalog object to be updated
    :param catalog_source: catalog object as a template to update 'catalog'
    :return: None
    """
    catalog.update(catalog_source)
