# -*- coding: utf-8 -*-
# A Sphinx extension to generate list of bug descriptions from findbugs.xml and messages.xml.
# It is necessary to generate localized list, because Sphinx i18n feature based on .po file
# does not support translating raw HTML block.

from xml.etree.ElementTree import *
import codecs

def generate_category_title(bug_category):
    return "%s (%s)" % (bug_category.findtext('.//Description'), bug_category.get('category'))

def generate_pattern_title(bug_pattern, message):
    return "%s: %s (%s)" % (bug_pattern.get('abbrev'), message.findtext('.//ShortDescription'), bug_pattern.get('type'))

def generate_bug_description(language):
    print("Generating bug description page for %s..." % language)
    findbugs = parse('../spotbugs/etc/findbugs.xml')
    if language == 'ja':
        messages = parse('../spotbugs/etc/messages_ja.xml')
    else:
        messages = parse('../spotbugs/etc/messages.xml')

    with codecs.open('generated/bugDescriptionList.rst', 'w', encoding='UTF-8') as bug_description_page:
        for bug_category in sorted(messages.getiterator('BugCategory'), key=lambda element: element.get('category')):
            category = bug_category.get('category')
            category_title = generate_category_title(bug_category)
            bug_description_page.write(category_title)
            bug_description_page.write('\n')
            bug_description_page.write('-' * len(category_title))
            bug_description_page.write('\n')
            for line in bug_category.findtext('.//Details').splitlines():
                bug_description_page.write(line.strip())
                bug_description_page.write('\n')
            bug_description_page.write('\n\n')

            for bug_pattern in findbugs.findall(".//BugPattern[@category='%s']" % category):
                type = bug_pattern.get('type')
                message = messages.find(".//BugPattern[@type='%s']" % type)
                pattern_title = generate_pattern_title(bug_pattern, message)
                bug_description_page.write("%s\n%s\n\n" % (pattern_title, '^' * len(pattern_title)))
                details = message.findtext('.//Details')
                bug_description_page.write('.. raw:: html\n')
                for line in details.splitlines():
                    bug_description_page.write('  ')
                    bug_description_page.write(line)
                    bug_description_page.write('\n')
                bug_description_page.write('\n')

def setup(app):
    app.connect('builder-inited', lambda app: generate_bug_description(app.config.language))
