/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

import java.io.*;
import java.util.*;

import org.dom4j.Element;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.OutputFormat;

/**
 * Abstract base class for collections of BugInstance objects.
 * Supports reading and writing XML files.
 * @see BugInstance
 * @author David Hovemeyer
 */
public abstract class BugCollection {

	static {
		// Make sure BugInstance and all of the annotation classes
		// are loaded, to ensure that their XMLTranslators are registered.
		Class c;
		c = BugInstance.class;
		c = ClassAnnotation.class;
		c = FieldAnnotation.class;
		c = MethodAnnotation.class;
		c = SourceLineAnnotation.class;
		c = IntAnnotation.class;
	}

	public abstract void add(BugInstance bugInstance);
	public abstract Iterator<BugInstance> iterator();
	public abstract Collection<BugInstance> getCollection();

	private static final String ROOT_ELEMENT_NAME = "BugCollection";
	private static final String SRCMAP_ELEMENT_NAME= "SrcMap";
	private static final String PROJECT_ELEMENT_NAME = "Project";

	public void readXML(String fileName, Project project, Map<String, String> classToSourceFileMap)
		throws IOException, DocumentException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName));
		readXML(in, project, classToSourceFileMap);
	}

	public void readXML(File file, Project project, Map<String, String> classToSourceFileMap)
		throws IOException, DocumentException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
		readXML(in, project, classToSourceFileMap);
	}

	public void readXML(InputStream in, Project project, Map<String, String> classToSourceFileMap)
		throws IOException, DocumentException {

		SAXReader reader = new SAXReader();
		Document document = reader.read(in);

		Iterator i = document.getRootElement().elements().iterator();
		while (i.hasNext()) {
			Element element = (Element) i.next();
			String elementName = element.getName();

			if (elementName.equals(SRCMAP_ELEMENT_NAME)) {
				classToSourceFileMap.put(element.attributeValue("classname"), element.attributeValue("srcfile"));
			} else if (elementName.equals(PROJECT_ELEMENT_NAME)) {
				project.readElement(element);
			} else {
				XMLTranslator translator = XMLTranslatorRegistry.instance().getTranslator(elementName);
				if (translator == null)
					throw new DocumentException("Unknown element type: " + elementName);

				BugInstance bugInstance = (BugInstance) translator.fromElement(element);

				add(bugInstance);
			}
		}

		// Presumably, project is now up-to-date
		project.setModified(false);
	}

	public void writeXML(String fileName, Project project, Map<String, String> classToSourceFileMap) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
		writeXML(out, project, classToSourceFileMap);
	}
	
	public void writeXML(File file, Project project, Map<String, String> classToSourceFileMap) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		writeXML(out, project, classToSourceFileMap);
	}

	public void writeXML(OutputStream out, Project project, Map<String, String> classToSourceFileMap) throws IOException {
		Document document = DocumentHelper.createDocument();
		Element root = document.addElement(ROOT_ELEMENT_NAME);

		// Save the project information
		Element projectElement = root.addElement(PROJECT_ELEMENT_NAME);
		project.writeElement(projectElement);

		// Save all of the bug instances
		Iterator<BugInstance> i = this.iterator();
		while (i.hasNext()) {
			BugInstance bugInstance = i.next();
			bugInstance.toElement(root);
		}

		// Save the class to source map information
		Iterator<Map.Entry<String, String>> j = classToSourceFileMap.entrySet().iterator();
		while (j.hasNext()) {
			Map.Entry<String, String> entry = j.next();
			root.addElement(SRCMAP_ELEMENT_NAME)
				.addAttribute("classname", entry.getKey())
				.addAttribute("srcfile", entry.getValue());
		}

		XMLWriter writer = new XMLWriter(out, OutputFormat.createPrettyPrint());
		writer.write(document);
	}

}

// vim:ts=4
