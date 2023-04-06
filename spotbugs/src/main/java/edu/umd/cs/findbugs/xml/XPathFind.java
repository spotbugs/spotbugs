/*
 * Evaluate XPath expressions on an XML file
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

package edu.umd.cs.findbugs.xml;

import java.util.Iterator;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;


/**
 * Find nodes in a dom4j tree that match a particular XPath expression. The
 * main() driver prints out information about matching nodes in an XML document.
 *
 * <p>
 * For example, to find the list of non-disabled detectors in a FindBugs plugin
 * descriptor, you can use the expression <blockquote>
 * <code>/FindbugsPlugin/Detector[boolean(@disabled)=false()]/@class</code>
 * </blockquote>
 *
 * @author David Hovemeyer
 */
public abstract class XPathFind {
    private final Document document;

    public XPathFind(Document document) {
        this.document = document;
    }

    public void find(String xpath) {
        List<Node> nodes = XMLUtil.selectNodes(document, xpath);
        for (Node node : nodes) {
            match(node);
        }
    }

    protected abstract void match(Node node);

    public static void main(String[] argv) throws Exception {
        if (argv.length != 2) {
            System.err.println("Usage: " + XPathFind.class.getName() + ": <filename> <xpath expression>");
            System.exit(1);
        }

        String fileName = argv[0];
        String xpath = argv[1];

        SAXReader reader = XMLUtil.buildSAXReader();
        Document document = reader.read(fileName);

        XPathFind finder = new XPathFind(document) {
            @Override
            protected void match(Node node) {
                // System.out.println(node.toString());
                if (node instanceof Element) {
                    Element element = (Element) node;
                    System.out.println("Element: " + element.getQualifiedName());
                    System.out.println("\tText: " + element.getText());
                    System.out.println("\tAttributes:");
                    for (Iterator<?> i = element.attributeIterator(); i.hasNext();) {
                        Attribute attribute = (Attribute) i.next();
                        System.out.println("\t\t" + attribute.getName() + "=" + attribute.getValue());
                    }
                } else if (node instanceof Attribute) {
                    Attribute attribute = (Attribute) node;
                    System.out.println("Attribute: " + attribute.getName() + "=" + attribute.getValue());
                }
            }
        };

        finder.find(xpath);
    }
}
