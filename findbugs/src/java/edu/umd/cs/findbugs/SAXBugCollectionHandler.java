/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs;

import java.io.FileReader;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Build a BugCollection based on SAX events.
 * This is intended to replace the old DOM-based parsing
 * of XML bug result files, which was very slow.
 *
 * <p> Note: this class is not complete yet.
 *
 * @author David Hovemeyer
 */
public class SAXBugCollectionHandler extends DefaultHandler {
	private BugCollection bugCollection;
	private Project project;

	private ArrayList<String> elementStack;
	private StringBuffer textBuffer;
	private BugInstance bugInstance;

	public SAXBugCollectionHandler(BugCollection bugCollection, Project project) {
		this.bugCollection = bugCollection;
		this.project = project;

		this.elementStack = new ArrayList<String>();
		this.textBuffer = new StringBuffer();
	}

	public void startDocument() {
	}

	public void endDocument() {
	}

	public void startElement(String uri, String name, String qName, Attributes attributes)
		throws SAXException {
		// URI should always be empty.
		// So, qName is the name of the element.

		if (!elementStack.isEmpty()) {
			String outerElement = elementStack.get(elementStack.size() - 1);

			if (outerElement.equals("BugCollection")) {
				// Parsing a top-level element of the BugCollection
				if (qName.equals("Project")) {
					// Project element
					String filename = attributes.getValue("filename");
					if (filename != null)
						project.setFileName(filename);
				} else if (qName.equals("BugInstance")) {
					// BugInstance element - get required type and priority attributes
					String type = attributes.getValue("type");
					String priority = attributes.getValue("priority");

					if (type == null)
						throw new SAXException("BugInstance missing type attribute");
					if (priority == null)
						throw new SAXException("BugInstance missing priority attribute");

					try {
						int prio = Integer.parseInt(priority);
						bugInstance = new BugInstance(type, prio);
					} catch (NumberFormatException e) {
						throw new SAXException("BugInstance with invalid priority value \"" +
							priority + "\"");
					}
				}
			} else if (outerElement.equals("BugInstance")) {
				// Parsing an attribute of a BugInstance
				if (qName.equals("Class")) {
				} else if (qName.equals("Method")) {
				} else if (qName.equals("Field")) {
				} else if (qName.equals("SourceLine")) {
				} else if (qName.equals("Int")) {
				}
			}
		}

		textBuffer.delete(0, textBuffer.length());
		elementStack.add(qName);
	}

	public void endElement(String uri, String name, String qName) throws SAXException {
		// URI should always be empty.
		// So, qName is the name of the element.

		if (elementStack.size() > 1) {
			String outerElement = elementStack.get(elementStack.size() - 2);

			if (outerElement.equals("Project")) {
				//System.out.println("Adding project element " + qName + ": " + textBuffer.toString());
				if (qName.equals("Jar"))
					project.addJar(textBuffer.toString());
				else if (qName.equals("SrcDir"))
					project.addSourceDir(textBuffer.toString());
				else if (qName.equals("AuxClasspathEntry"))
					project.addAuxClasspathEntry(textBuffer.toString());
			} else if (outerElement.equals("BugInstance")) {
				if (qName.equals("UserAnnotation")) {
					bugInstance.setAnnotationText(textBuffer.toString());
				}
			}
		}

		elementStack.remove(elementStack.size() - 1);
	}

	public void characters(char[] ch, int start, int length) {
		textBuffer.append(ch, start, length);
	}

	// Just a test driver
	public static void main(String[] argv) throws Exception {
		XMLReader xr = XMLReaderFactory.createXMLReader();

		BugCollection bugCollection = new SortedBugCollection();
		Project project = new Project();

		SAXBugCollectionHandler handler = new SAXBugCollectionHandler(bugCollection, project);
		xr.setContentHandler(handler);
		xr.setErrorHandler(handler);

		// Parse each file provided on the
		// command line.
		for (int i = 0; i < argv.length; i++) {
			FileReader r = new FileReader(argv[i]);
			xr.parse(new InputSource(r));
		}
	}
}

// vim:ts=4
