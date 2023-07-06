# SPDX-FileCopyrightText: 2023 Jeremias Eppler <jeremias.eppler@mercedes-benz.com>
# SPDX-License-Identifier: LGPL-2.1-or-later

import xml.etree.ElementTree as ET
import json
import argparse
import os

def main(xml_file):
    if os.path.isfile(xml_file) is False:
        print(f"CWE XML file {xml_file} does not exist.")
        os.exit(1)

    # overwrite namespace
    # see: https://docs.python.org/3/library/xml.etree.elementtree.html#parsing-xml-with-namespaces
    namespaces = { 'cwe': 'http://cwe.mitre.org/cwe-6' }

    tree = ET.parse(xml_file)
    root = tree.getroot()
    weaknesses = root[0]

    data = {}
    data['name'] = root.get('Name')
    data['version'] = root.get('Version')
    data['date'] = root.get('Date')
    data['weaknesses'] = []

    for weakness in weaknesses:
        cweid = weakness.get('ID')
        name = weakness.get('Name')

        description = ''
        severity = ''

        descriptionElement = weakness.find('cwe:Description', namespaces)
        description = descriptionElement.text

        severityElement = weakness.find('cwe:Likelihood_Of_Exploit', namespaces)

        if severityElement is None:
            severity = 'none'
        else:
            severity = severityElement.text.lower()

        weakness_element = {}
        weakness_element["cweid"] = cweid
        weakness_element["name"] = name
        weakness_element["description"] = description
        weakness_element["severity"] = severity
        data['weaknesses'].append(weakness_element)

    file_name = data['name'] + "_" + data['version'] + ".json"

    with open(file_name, 'w') as json_file:
        json.dump(data, json_file, indent=4)

    print(f"Converted file and wrote it to {file_name}")

if __name__ == "__main__":
    parser = parser = argparse.ArgumentParser(description='Convert CWE XML to JSON.')
    parser.add_argument('xml_file', metavar='File', help='The CWE Weaknesse Catalog in XML format.')

    args = parser.parse_args()
    main(args.xml_file)

