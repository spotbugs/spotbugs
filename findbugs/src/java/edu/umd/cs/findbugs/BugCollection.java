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

	private static final String ROOT_ELEMENT_NAME = "BugCollection";

	public void readXML(String fileName) throws IOException, DocumentException {

		BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName));

		SAXReader reader = new SAXReader();
		Document document = reader.read(in);

		Iterator i = document.getRootElement().elements().iterator();
		while (i.hasNext()) {
			Element element = (Element) i.next();

			XMLTranslator translator = XMLTranslatorRegistry.instance().getTranslator(element.getName());
			if (translator == null)
				throw new DocumentException("Unknown element type: " + element.getName());

			BugInstance bugInstance = (BugInstance) translator.fromElement(element);

			add(bugInstance);
		}
	}

	public void writeXML(String fileName) throws IOException {

		Document document = DocumentHelper.createDocument();
		Element root = document.addElement(ROOT_ELEMENT_NAME);

		Iterator<BugInstance> i = this.iterator();

		while (i.hasNext()) {
			BugInstance bugInstance = i.next();
			bugInstance.toElement(root);
		}

		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));

		XMLWriter writer = new XMLWriter(out);
		writer.write(document);
	}

}

// vim:ts=4
