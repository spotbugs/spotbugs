# -*- coding: utf-8 -*-

import os
from glob import glob

import click

from . import catalog as c
from .pycompat import relpath


# ==================================
# utility functions

def get_lang_dirs(path):
    dirs = [relpath(d, path)
            for d in glob(path+'/[a-z]*')
            if os.path.isdir(d) and not d.endswith('pot')]
    return (tuple(dirs),)


# ==================================
# commands

def update(locale_dir, pot_dir, languages):
    """
    Update specified language's po files from pot.

    :param unicode locale_dir: path for locale directory
    :param unicode pot_dir: path for pot directory
    :param tuple languages: languages to update po files
    :return: {'create': 0, 'update': 0, 'notchanged': 0}
    :rtype: dict
    """
    status = {
        'create': 0,
        'update': 0,
        'notchanged': 0,
    }

    for dirpath, dirnames, filenames in os.walk(pot_dir):
        for filename in filenames:
            pot_file = os.path.join(dirpath, filename)
            base, ext = os.path.splitext(pot_file)
            if ext != ".pot":
                continue
            basename = relpath(base, pot_dir)
            for lang in languages:
                po_dir = os.path.join(locale_dir, lang, 'LC_MESSAGES')
                po_file = os.path.join(po_dir, basename + ".po")
                cat_pot = c.load_po(pot_file)
                if os.path.exists(po_file):
                    cat = c.load_po(po_file)
                    msgids = set([m.id for m in cat if m.id])
                    c.update_with_fuzzy(cat, cat_pot)
                    new_msgids = set([m.id for m in cat if m.id])
                    if msgids != new_msgids:
                        added = new_msgids - msgids
                        deleted = msgids - new_msgids
                        status['update'] += 1
                        click.echo('Update: {0} +{1}, -{2}'.format(
                            po_file, len(added), len(deleted)))
                        c.dump_po(po_file, cat)
                    else:
                        status['notchanged'] += 1
                        click.echo('Not Changed: {0}'.format(po_file))
                else:  # new po file
                    status['create'] += 1
                    click.echo('Create: {0}'.format(po_file))
                    c.dump_po(po_file, cat_pot)

    return status


def build(locale_dir, output_dir, languages):
    """
    Update specified language's po files from pot.

    :param unicode locale_dir: path for locale directory
    :param unicode output_dir: path for mo output directory
    :param tuple languages: languages to update po files
    :return: None
    """
    for lang in languages:
        lang_dir = os.path.join(locale_dir, lang)
        for dirpath, dirnames, filenames in os.walk(lang_dir):
            dirpath_output = os.path.join(output_dir, os.path.relpath(dirpath, locale_dir))

            for filename in filenames:
                base, ext = os.path.splitext(filename)
                if ext != ".po":
                    continue

                mo_file = os.path.join(dirpath_output, base + ".mo")
                po_file = os.path.join(dirpath, filename)

                if (os.path.exists(mo_file) and
                   os.path.getmtime(mo_file) > os.path.getmtime(po_file)):
                    continue
                click.echo('Build: {0}'.format(mo_file))
                cat = c.load_po(po_file)
                c.write_mo(mo_file, cat)


def stat(locale_dir, languages):
    """
    Print statistics for all po files.

    :param unicode locale_dir: path for locale directory
    :param tuple languages: languages to update po files
    :return: {'FILENAME': {'translated': 0, 'fuzzy': 0, 'untranslated': 0}, ...}
    :rtype: dict
    """
    result = {}

    for lang in languages:
        lang_dir = os.path.join(locale_dir, lang)
        for dirpath, dirnames, filenames in os.walk(lang_dir):
            for filename in filenames:
                po_file = os.path.join(dirpath, filename)
                base, ext = os.path.splitext(po_file)
                if ext != ".po":
                    continue

                cat = c.load_po(po_file)
                r = result[po_file.replace('\\', '/')] = {
                    'translated': len(c.translated_entries(cat)),
                    'fuzzy': len(c.fuzzy_entries(cat)),
                    'untranslated': len(c.untranslated_entries(cat)),
                }
                click.echo(
                    '{0}: {1} translated, {2} fuzzy, {3} untranslated.'.format(
                        po_file,
                        r['translated'],
                        r['fuzzy'],
                        r['untranslated'],
                    )
                )

    return result
