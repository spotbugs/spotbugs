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
 * @author David Hovemeyer
 */
public class SAXBugCollectionHandler extends DefaultHandler {
	private BugCollection bugCollection;
	private Project project;

	private ArrayList<String> elementStack;
	private StringBuffer textBuffer;
	private BugInstance bugInstance;
	private MethodAnnotation methodAnnotation;

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
						project.setProjectFileName(filename);
				} else if (qName.equals("BugInstance")) {
					// BugInstance element - get required type and priority attributes
					String type = getRequiredAttribute(attributes, "type", qName);
					String priority = getRequiredAttribute(attributes, "priority", qName);

					try {
						int prio = Integer.parseInt(priority);
						bugInstance = new BugInstance(type, prio);
					} catch (NumberFormatException e) {
						throw new SAXException("BugInstance with invalid priority value \"" +
							priority + "\"", e);
					}
				}
			} else if (outerElement.equals("BugInstance")) {
				// Parsing an attribute of a BugInstance
				BugAnnotation bugAnnotation = null;
				if (qName.equals("Class")) {
					String className = getRequiredAttribute(attributes, "classname", qName);
					bugAnnotation = new ClassAnnotation(className);
				} else if (qName.equals("Method") || qName.equals("Field")) {
					String classname = getRequiredAttribute(attributes, "classname", qName);
					String fieldOrMethodName = getRequiredAttribute(attributes, "name", qName);
					String signature = getRequiredAttribute(attributes, "signature", qName);
					if (qName.equals("Method")) {
						// Save in field in case of nested SourceLine elements.
						methodAnnotation =
							new MethodAnnotation(classname, fieldOrMethodName, signature);
						bugAnnotation = methodAnnotation;
					} else {
						String isStatic = getRequiredAttribute(attributes, "isStatic", qName);
						bugAnnotation = new FieldAnnotation(classname, fieldOrMethodName, signature,
							Boolean.valueOf(isStatic).booleanValue());
					}
				} else if (qName.equals("SourceLine")) {
					bugAnnotation = createSourceLineAnnotation(qName, attributes);
				} else if (qName.equals("Int")) {
					try {
						String value = getRequiredAttribute(attributes, "value", qName);
						String role = attributes.getValue("role");
						bugAnnotation = new IntAnnotation(Integer.parseInt(value));
						if (role != null)
							bugAnnotation.setDescription(role);
					} catch (NumberFormatException e) {
						throw new SAXException("Bad integer value in Int");
					}
				}

				if (bugAnnotation != null)
					bugInstance.add(bugAnnotation);
			} else if (outerElement.equals("Method")) {
				if (qName.equals("SourceLine")) {
					// Method elements can contain nested SourceLine elements.
					methodAnnotation.setSourceLines(createSourceLineAnnotation(qName, attributes));
				}
			}
		}

		textBuffer.delete(0, textBuffer.length());
		elementStack.add(qName);
	}

	private SourceLineAnnotation createSourceLineAnnotation(String qName, Attributes attributes)
			throws SAXException {
		String classname = getRequiredAttribute(attributes, "classname", qName);
		String sourceFile = attributes.getValue("sourcefile");
		if (sourceFile == null)
			sourceFile = SourceLineAnnotation.UNKNOWN_SOURCE_FILE;
		String startLine = getRequiredAttribute(attributes, "start", qName);
		String endLine = getRequiredAttribute(attributes, "end", qName);
		String startBytecode = attributes.getValue("startBytecode");
		String endBytecode = attributes.getValue("endBytecode");

		try {
			int sl = Integer.parseInt(startLine);
			int el = Integer.parseInt(endLine);
			int sb = startBytecode != null ? Integer.parseInt(startBytecode) : -1;
			int eb = endBytecode != null ? Integer.parseInt(endBytecode) : -1;

			return new SourceLineAnnotation(classname, sourceFile, sl, el, sb, eb);
		} catch (NumberFormatException e) {
			throw new SAXException("Bad integer value in SourceLine element", e);
		}
	}


	public void endElement(String uri, String name, String qName) throws SAXException {
		// URI should always be empty.
		// So, qName is the name of the element.

		if (elementStack.size() > 1) {
			String outerElement = elementStack.get(elementStack.size() - 2);

			if (outerElement.equals("BugCollection")) {
				if (qName.equals("SummaryHTML"))
					bugCollection.setSummaryHTML(textBuffer.toString());
				else if (qName.equals("BugInstance"))
					bugCollection.add(bugInstance);
				else if (qName.equals(BugCollection.ANALYSIS_ERROR_ELEMENT_NAME))
					bugCollection.addError(textBuffer.toString());
				else if (qName.equals(BugCollection.MISSING_CLASS_ELEMENT_NAME))
					bugCollection.addMissingClass(textBuffer.toString());
			} else if (outerElement.equals("Project")) {
				//System.out.println("Adding project element " + qName + ": " + textBuffer.toString());
				if (qName.equals("Jar"))
					project.addFile(textBuffer.toString());
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

	private static String getRequiredAttribute(Attributes attributes, String attrName, String elementName)
		throws SAXException {
		String value = attributes.getValue(attrName);
		if (value == null)
			throw new SAXException(elementName + " element missing " + attrName + " attribute");
		return value;
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
