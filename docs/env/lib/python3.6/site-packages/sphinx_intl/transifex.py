# -*- coding: utf-8 -*-

import os
import re
import textwrap

import click

from .pycompat import relpath
from .catalog import load_po


# ==================================
# settings

# To avoid using invalid resource name, append underscore to such names.
# As a limitation, append `_` doesn't care about collision to other resources.
# e.g. 'glossary' and 'glossary_' are pushed as a 'glossary_'.
IGNORED_RESOURCE_NAMES = (
    'glossary',  # transifex reject this name
)

TRANSIFEXRC_TEMPLATE = """\
[https://www.transifex.com]
hostname = https://www.transifex.com
password = %(transifex_password)s
username = %(transifex_username)s
token =
"""

TXCONFIG_TEMPLATE = """\
[main]
host = https://www.transifex.com
"""


# ==================================
# utility functions

def get_tx_root():
    import txclib.utils
    tx_root = txclib.utils.find_dot_tx()
    if tx_root is None:
        msg = "'.tx/config' not found. You need 'create-txconfig' first."
        raise click.BadParameter(msg)
    return tx_root


def normalize_resource_name(name):
    # replace path separator with '--'
    name = re.sub(r'[\\/]', '--', name)

    # replace unusable characters (not: -, _ ascii, digit) with '_'
    name = re.sub(r'[^\-\w]', '_', name)

    # append `_` for ignored resource names
    while name in IGNORED_RESOURCE_NAMES:
        name += '_'

    return name


# ==================================
# commands

def create_transifexrc(transifex_username, transifex_password):
    """
    Create `$HOME/.transifexrc`
    """
    target = os.path.normpath(os.path.expanduser('~/.transifexrc'))

    if os.path.exists(target):
        click.echo('{0} already exists, skipped.'.format(target))
        return

    if not transifex_username or not 'transifex_password':
        msg = textwrap.dedent("""\
        You need transifex username/password by command option or environment.
        command option: --transifex-username, --transifex-password
        """)
        raise click.BadParameter(msg, param_hint='transifex_username,transifex_password')

    with open(target, 'wt') as rc:
        rc.write(TRANSIFEXRC_TEMPLATE % locals())
    click.echo('Create: {0}'.format(target))


def create_txconfig():
    """
    Create `./.tx/config`
    """
    target = os.path.normpath('.tx/config')
    if os.path.exists(target):
        click.echo('{0} already exists, skipped.'.format(target))
        return

    if not os.path.exists('.tx'):
        os.mkdir('.tx')

    with open(target, 'wt') as f:
        f.write(TXCONFIG_TEMPLATE)

    click.echo('Create: {0}'.format(target))


def update_txconfig_resources(transifex_project_name, locale_dir, pot_dir):
    """
    Update resource sections of `./.tx/config`.
    """
    try:
        import txclib
        import txclib.utils
    except ImportError:
        msg = textwrap.dedent("""\
            Could not import 'txclib.utils'.
            You need install transifex_client external library.
            Please install below command if you want to this action:

                $ pip install sphinx-intl[transifex]
            """)
        raise click.BadParameter(msg)

    tx_root = get_tx_root()

    tx_version = getattr(txclib, '__version__', '0.0')
    if tx_version < '0.13':
        args_tmpl = (
            '--auto-local', '-r', '%(transifex_project_name)s.%(resource_name)s',
            '%(locale_dir)s/<lang>/LC_MESSAGES/%(resource_path)s.po',
            '--source-lang', 'en',
            '--source-file', '%(pot_dir)s/%(resource_path)s.pot',
            '--execute'
        )
    else:
        args_tmpl = (
            'mapping',
            '-r', '%(transifex_project_name)s.%(resource_name)s',
            '-t', 'PO',
            '-s', 'en',
            '-f', '%(pot_dir)s/%(resource_path)s.pot',
            '--execute',
            '%(locale_dir)s/<lang>/LC_MESSAGES/%(resource_path)s.po',
        )

    # convert transifex_project_name to internal name
    transifex_project_name = transifex_project_name.replace(' ', '-')
    transifex_project_name = re.sub(r'[^\-_\w]', '', transifex_project_name)

    for dirpath, dirnames, filenames in os.walk(pot_dir):
        for filename in filenames:
            pot_file = os.path.join(dirpath, filename)
            base, ext = os.path.splitext(pot_file)
            if ext != ".pot":
                continue
            resource_path = relpath(base, pot_dir)
            pot = load_po(pot_file)
            if len(pot):
                resource_name = normalize_resource_name(resource_path)
                lv = locals()
                args = [arg % lv for arg in args_tmpl]
                # print('set', args, tx_root)
                txclib.utils.exec_command('set', args, tx_root)
            else:
                click.echo('{0} is empty, skipped'.format(pot_file))
