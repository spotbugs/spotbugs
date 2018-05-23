# -*- coding: utf-8 -*-
# A Sphinx extension to generate list of bug descriptions from findbugs.xml and messages.xml.
# It is necessary to generate localized list, because Sphinx i18n feature based on .po file
# does not support translating raw HTML block.

from __future__ import print_function, unicode_literals

from collections import namedtuple
from docutils.utils import column_width
import io
import os
import xml.etree.ElementTree as ET


DOCS_DIR = os.path.dirname(os.path.dirname(__file__))
ETC_DIR = os.path.normpath(os.path.join(DOCS_DIR, "..", "spotbugs", "etc"))


BugCategory = namedtuple("BugCategory", "name hidden description")
Bug = namedtuple("Bug", "name abbrev category deprecated description")
Detector = namedtuple("Detector", "java_class reports speed hidden disabled")


def parse_bool_attr(element, attr_name):
    return element.get(attr_name) == "true"  # missing attr = false


def parse_bug_patterns(app, path):
    document = ET.parse(path)

    bugs = {}
    categories = {}

    # Index all bug patterns
    for element in document.iterfind(".//BugPattern"):
        bug = Bug(
            name=element.get("type"),
            abbrev=element.get("abbrev"),
            category=element.get("category"),
            deprecated=element.get("deprecated"),
            description="")
        if bug.category not in categories:
            categories[bug.category] = BugCategory(name=bug.category, hidden=False, description="")
        bugs[bug.name] = bug

    # Update hidden categories
    for element in document.iterfind(".//BugCategory"):
        category_name = element.get("category")
        hidden = parse_bool_attr(element, "hidden")
        categories[category_name] = categories[category_name]._replace(hidden=hidden)

    # Index detectors
    detectors = []
    for element in document.iterfind(".//Detector"):
        klass = element.get("class")
        app.debug("Parsing detector: %s", klass)
        disabled = parse_bool_attr(element, "disabled")
        hidden = parse_bool_attr(element, "hidden")
        speed = element.get("speed")

        reports = ()
        for key in element.get("reports", "").split(","):
            if not key:
                continue
            if key not in bugs:
                app.warn("Detector %s claims to report %s but no such bug exists", klass, key)
            reports += (bugs[key],)

        detectors.append(Detector(klass, reports, speed, hidden, disabled))

    return categories, bugs, detectors


def parse_i18n_messages(language):
    filename = "messages.xml" if language == "en" else "messages_{}.xml".format(language)
    return ET.parse(os.path.join(ETC_DIR, filename))


def i18n_text(element, subtag):
    value = element.findtext("./" + subtag)
    # On Python2.7, ElementTree returns bytes if string is ASCII
    return value.decode('utf-8') if isinstance(value, bytes) else value


def generate_pattern_title(bug_pattern, message):
    return "%s: %s (%s)" % (
        bug_pattern.get("abbrev"),
        i18n_text(message, "ShortDescription"),
        bug_pattern.get("type"))


def generate_category(messages, category):
    msg_elem = messages.find(".//BugCategory[@category='%s']" % category.name)

    title = "{0}: {1}".format(i18n_text(msg_elem, "Description"), category.name)

    yield ".. _bug-category-{0}:".format(category.name.lower())
    yield ""
    yield title
    yield "-" * column_width(title)
    yield ""

    for line in i18n_text(msg_elem, "Details").splitlines():
        yield line.strip()

    yield ""
    yield ""


def generate_raw_section(html, prolog=None):
    yield ".. raw:: html"
    yield ""
    if prolog:
        yield "   " + prolog
        yield ""
    for line in html.strip().splitlines():
        yield "   " + line
    yield ""


def generate_bug(messages, bug):
    if bug.deprecated:
        return

    msg_elem = messages.find(".//BugPattern[@type='%s']" % bug.name)

    description = i18n_text(msg_elem, "ShortDescription")
    details = i18n_text(msg_elem, "Details")

    title = "{bug.abbrev}: {short_desc}".format(bug=bug, short_desc=description)

    # This creates a target for :ref:`my-bug-pattern` links
    yield ".. _{bug.name}:".format(bug=bug)
    yield ""
    yield title
    yield "^" * column_width(title)

    # This is needed because Sphinx turns FOO_BAR into foo-bar, but
    # we still want bugDescription.html#FOO_BAR to work
    anchor = '<p><em id="{bug.name}">{bug.name}</em></p>'.format(bug=bug)

    for line in generate_raw_section(details, prolog=anchor):
        yield line


def generate_bug_description(app, messages, categories, bugs):
    for category in sorted(categories.values(), key=lambda c: c.name):
        app.debug("Generating %s", category)
        if category.hidden:
            continue

        for line in generate_category(messages, category):
            yield line

        category_bugs = [bug for bug in bugs.values() if bug.category == category.name]
        category_bugs.sort(key=lambda b: b.name)

        for bug in category_bugs:
            app.debug("Generating %s", bug)
            for line in generate_bug(messages, bug):
                yield line

    yield ""


def generate_detector_description(app, messages, detectors, disabled):
    for detector in sorted(detectors, key=lambda d: d.java_class):
        if detector.hidden:
            continue

        if detector.disabled != disabled:
            continue

        app.debug("Generating %s", detector)

        msg_elem = messages.find(".//Detector[@class='%s']" % detector.java_class)
        short_name = detector.java_class.split(".")[-1]
        details = i18n_text(msg_elem, "Details")

        yield ".. _detector-{}:".format(short_name.lower())
        yield ""
        yield short_name
        yield "^" * column_width(short_name)
        yield ""

        for line in generate_raw_section(details):
            yield line
        yield ""

        for bug in sorted(detector.reports, key=lambda bug: bug.name):
            yield "* :ref:`{bug.name}`".format(bug=bug)
        yield ""


def write_bug_description(app):
    categories, bugs, detectors = parse_bug_patterns(app, os.path.join(ETC_DIR, "findbugs.xml"))
    messages = parse_i18n_messages(app.config.language)

    output_dir = os.path.join(DOCS_DIR, "generated")

    with io.open(os.path.join(output_dir, "bugDescriptionList.inc"), mode="w", encoding="utf-8") as output:
        for line in generate_bug_description(app, messages, categories, bugs):
            print(line, file=output)

    with io.open(os.path.join(output_dir, "detectorListEnabled.inc"), mode="w", encoding="utf-8") as output:
        for line in generate_detector_description(app, messages, detectors, disabled=False):
            print(line, file=output)

    with io.open(os.path.join(output_dir, "detectorListDisabled.inc"), mode="w", encoding="utf-8") as output:
        for line in generate_detector_description(app, messages, detectors, disabled=True):
            print(line, file=output)

def setup(app):
    app.connect('builder-inited', write_bug_description)
