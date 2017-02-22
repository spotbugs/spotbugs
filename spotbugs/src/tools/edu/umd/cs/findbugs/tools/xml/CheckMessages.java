/*
 * Check FindBugs XML message files
 * Copyright (C) 2004, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.tools.xml;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import edu.umd.cs.findbugs.xml.XMLUtil;

/**
 * Ensure that the XML messages files in a FindBugs plugin are valid and
 * complete.
 */
public class CheckMessages {

    private static class CheckMessagesException extends DocumentException {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        public CheckMessagesException(String msg, XMLFile xmlFile, Node node) {
            super("In " + xmlFile.getFilename() + ", " + node.toString() + ": " + msg);
        }

        public CheckMessagesException(String msg, XMLFile xmlFile) {
            super("In " + xmlFile.getFilename() + ": " + msg);
        }
    }

    private static class XMLFile {
        private final String filename;

        private final Document document;

        public XMLFile(String filename) throws DocumentException {
            this.filename = filename;

            File file = new File(filename);
            SAXReader saxReader = new SAXReader();
            this.document = saxReader.read(file);
        }

        public String getFilename() {
            return filename;
        }

        public Document getDocument() {
            return document;
        }

        /**
         * Get iterator over Nodes selected by given XPath expression.
         */
        @SuppressWarnings("unchecked")
        public Iterator<Node> xpathIterator(String xpath) {
            List<Node> nodes = XMLUtil.selectNodes(document, xpath);
            return nodes.iterator();
        }

        /**
         * Build collection of the values of given attribute in all nodes
         * matching given XPath expression.
         */
        public Set<String> collectAttributes(String xpath, String attrName) throws DocumentException {
            Set<String> result = new HashSet<String>();

            for (Iterator<Node> i = xpathIterator(xpath); i.hasNext();) {
                Node node = i.next();
                String value = checkAttribute(node, attrName).getValue();
                result.add(value);
            }

            return result;
        }

        public Attribute checkAttribute(Node node, String attrName) throws DocumentException {
            if (!(node instanceof Element)) {
                throw new CheckMessagesException("Node is not an element", this, node);
            }
            Element element = (Element) node;
            Attribute attr = element.attribute(attrName);
            if (attr == null) {
                throw new CheckMessagesException("Missing " + attrName + " attribute", this, node);
            }
            return attr;
        }

        public Element checkElement(Node node, String elementName) throws DocumentException {
            if (!(node instanceof Element)) {
                throw new CheckMessagesException("Node is not an element", this, node);
            }
            Element element = (Element) node;
            Element child = element.element(elementName);
            if (child == null) {
                throw new CheckMessagesException("Missing " + elementName + " element", this, node);
            }
            return child;
        }

        public String checkNonEmptyText(Node node) throws DocumentException {
            if (!(node instanceof Element)) {
                throw new CheckMessagesException("Node is not an element", this, node);
            }
            Element element = (Element) node;
            String text = element.getText();
            if ("".equals(text)) {
                throw new CheckMessagesException("Empty text in element", this, node);
            }
            return text;
        }
    }

    private final Set<String> declaredDetectorsSet;

    private final Set<String> declaredAbbrevsSet;

    public CheckMessages(String pluginDescriptorFilename) throws DocumentException {

        XMLFile pluginDescriptorDoc = new XMLFile(pluginDescriptorFilename);

        declaredDetectorsSet = pluginDescriptorDoc.collectAttributes("/FindbugsPlugin/Detector", "class");

        declaredAbbrevsSet = pluginDescriptorDoc.collectAttributes("/FindbugsPlugin/BugPattern", "abbrev");
    }

    /**
     * Check given messages file for validity.
     *
     * @throws DocumentException
     *             if the messages file is invalid
     */
    public void checkMessages(XMLFile messagesDoc) throws DocumentException {
        // Detector elements must all have a class attribute
        // and details child element.
        for (Iterator<Node> i = messagesDoc.xpathIterator("/MessageCollection/Detector"); i.hasNext();) {
            Node node = i.next();
            messagesDoc.checkAttribute(node, "class");
            messagesDoc.checkElement(node, "Details");
        }

        // BugPattern elements must all have type attribute
        // and ShortDescription, LongDescription, and Details
        // child elements.
        for (Iterator<Node> i = messagesDoc.xpathIterator("/MessageCollection/BugPattern"); i.hasNext();) {
            Node node = i.next();
            messagesDoc.checkAttribute(node, "type");
            messagesDoc.checkElement(node, "ShortDescription");
            messagesDoc.checkElement(node, "LongDescription");
            messagesDoc.checkElement(node, "Details");
        }

        // BugCode elements must contain abbrev attribute
        // and have non-empty text
        for (Iterator<Node> i = messagesDoc.xpathIterator("/MessageCollection/BugCode"); i.hasNext();) {
            Node node = i.next();
            messagesDoc.checkAttribute(node, "abbrev");
            messagesDoc.checkNonEmptyText(node);
        }

        // Check that all Detectors are described
        Set<String> describedDetectorsSet = messagesDoc.collectAttributes("/MessageCollection/Detector", "class");
        checkDescribed("Bug detectors not described by Detector elements", messagesDoc, declaredDetectorsSet,
                describedDetectorsSet);

        // Check that all BugCodes are described
        Set<String> describedAbbrevsSet = messagesDoc.collectAttributes("/MessageCollection/BugCode", "abbrev");
        checkDescribed("Abbreviations not described by BugCode elements", messagesDoc, declaredAbbrevsSet, describedAbbrevsSet);
    }

    public void checkDescribed(String description, XMLFile xmlFile, Set<String> declared, Set<String> described)
            throws DocumentException {

        Set<String> notDescribed = new HashSet<String>();
        notDescribed.addAll(declared);
        notDescribed.removeAll(described);

        if (!notDescribed.isEmpty()) {
            throw new CheckMessagesException(description + ": " + notDescribed.toString(), xmlFile);
        }
    }

    public static void main(String[] argv) throws Exception {
        if (argv.length < 2) {
            System.err.println("Usage: " + CheckMessages.class.getName()
                    + " <plugin descriptor xml> <bug description xml> [<bug description xml>...]");
            System.exit(1);
        }

        String pluginDescriptor = argv[0];

        try {
            CheckMessages checkMessages = new CheckMessages(pluginDescriptor);
            for (int i = 1; i < argv.length; ++i) {
                String messagesFile = argv[i];
                System.out.println("Checking messages file " + messagesFile);
                checkMessages.checkMessages(new XMLFile(messagesFile));
            }
        } catch (DocumentException e) {
            System.err.println("Could not verify messages files: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Messages files look OK!");
    }
}

