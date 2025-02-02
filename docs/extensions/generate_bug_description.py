# -*- coding: utf-8 -*-
# A Sphinx extension to generate list of bug descriptions from findbugs.xml and messages.xml.
# It is necessary to generate localized list, because Sphinx i18n feature based on .po file
# does not support translating raw HTML block.

from xml.etree.ElementTree import *
import codecs

def parse_bool_attr(element, attr_name):
    return element.get(attr_name) == "true"

def generate_category_title(bug_category):
    return "%s (%s)" % (bug_category.findtext('.//Description'), bug_category.get('category'))

def generate_pattern_title(bug_pattern, message):
    return "%s: %s (%s)" % (bug_pattern.get('abbrev'), message.findtext('.//ShortDescription'), bug_pattern.get('type'))

def generate_bug_description(language):
    print("Generating bug description page for %s..." % language)
    findbugs = parse('../spotbugs/etc/findbugs.xml')
    fallback = None
    if language == 'ja':
        messages = parse('../spotbugs/etc/messages_ja.xml')
        fallback = parse('../spotbugs/etc/messages.xml')
    else:
        messages = parse('../spotbugs/etc/messages.xml')

    with codecs.open('generated/bugDescriptionList.inc', 'w', encoding='UTF-8') as bug_description_page:
        for bug_category in sorted(messages.iter('BugCategory'), key=lambda element: element.get('category')):
            category = bug_category.get('category')
            category_title = generate_category_title(bug_category)
            bug_description_page.write(category_title)
            bug_description_page.write('\n')
            bug_description_page.write('-' * len(category_title))
            bug_description_page.write('\n\n')
            for line in bug_category.findtext('.//Details').splitlines():
                bug_description_page.write(line.strip())
                bug_description_page.write('\n')
            bug_description_page.write('\n\n')

            for bug_pattern in findbugs.findall(".//BugPattern[@category='%s']" % category):
                if (bug_pattern.get('deprecated') == 'true'):
                    continue
                type = bug_pattern.get('type')
                bug_description_page.write(".. _%s:" % type)
                bug_description_page.write('\n\n')
                message = findMessage(".//BugPattern[@type='%s']" % type, messages, fallback)
                pattern_title = generate_pattern_title(bug_pattern, message)
                bug_description_page.write("%s\n%s\n\n" % (pattern_title, '^' * len(pattern_title)))
                details = message.findtext('.//Details')
                bug_description_page.write('.. raw:: html\n')
                for line in details.splitlines():
                    bug_description_page.write('  ')
                    bug_description_page.write(line)
                    bug_description_page.write('\n')
                bug_description_page.write('\n')

    with codecs.open('generated/detectorListEnabled.inc', 'w', encoding='UTF-8') as enabled_detector_page:
        with codecs.open('generated/detectorListDisabled.inc', 'w', encoding='UTF-8') as disabled_detector_page:
            for element in findbugs.iterfind(".//Detector"):
                hidden = parse_bool_attr(element, "hidden")
                if hidden:
                    continue
                klass = element.get("class")
                disabled = parse_bool_attr(element, "disabled")
                reports = element.get("reports", "").split(",")

                page = disabled_detector_page if disabled else enabled_detector_page
                message = findMessage(".//Detector[@class='%s']" % klass, messages, fallback)
                short_name = klass.split(".")[-1]
                details = message.findtext('.//Details')

                page.write("%s\n%s\n\n" % (short_name, '^' * len(short_name)))
                page.write('.. raw:: html\n')
                for line in details.splitlines():
                    page.write('  ')
                    page.write(line)
                    page.write('\n')
                page.write('\n')

                for type in sorted(reports):
                    page.write("* :ref:`%s`" % type)
                    page.write('\n')
                page.write('\n')

def findMessage(pattern, messages, fallback):
    message = messages.find(pattern)
    if message is None and fallback is not None:
        print("Using fallback for %s" % pattern)
        message = fallback.find(pattern)
    if message is None:
        print("Pattern %s was not found", pattern)
    return message

def setup(app):
    app.connect('builder-inited', lambda app: generate_bug_description(app.config.language))
