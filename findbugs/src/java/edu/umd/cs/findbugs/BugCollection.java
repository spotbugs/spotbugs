/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/*
// notyet
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
*/

/**
 * Abstract base class for collections of BugInstance objects
 * and error messages associated with analysis.
 * Supports reading and writing XML files.
 *
 * @author David Hovemeyer
 * @see BugInstance
 */
public abstract class BugCollection {

	static int x;

	static {
		// Make sure BugInstance and all of the annotation classes
		// are loaded, and that their static initializers run,
		// to ensure that their XMLTranslators are registered.
		x = BugInstance.dummy + ClassAnnotation.dummy +
		        FieldAnnotation.dummy + MethodAnnotation.dummy +
		        SourceLineAnnotation.dummy + IntAnnotation.dummy;
	}

	public void addAll(Collection<BugInstance> collection) {
		Iterator<BugInstance> i = collection.iterator();
		while (i.hasNext()) {
			add(i.next());
		}
	}

	public abstract boolean add(BugInstance bugInstance);

	public abstract boolean remove(BugInstance bugInstance);

	public abstract Iterator<BugInstance> iterator();

	public abstract Collection<BugInstance> getCollection();

	public abstract void addError(String message);

	public abstract void addMissingClass(String message);

	public abstract Iterator<String> errorIterator();

	public abstract Iterator<String> missingClassIterator();

	public abstract void setSummaryHTML(String html);

	public abstract String getSummaryHTML();

	private static final String ROOT_ELEMENT_NAME = "BugCollection";
	private static final String SRCMAP_ELEMENT_NAME = "SrcMap";
	private static final String PROJECT_ELEMENT_NAME = "Project";
	private static final String ERRORS_ELEMENT_NAME = "Errors";
	private static final String ANALYSIS_ERROR_ELEMENT_NAME = "AnalysisError";
	private static final String MISSING_CLASS_ELEMENT_NAME = "MissingClass";
	private static final String SUMMARY_HTML_ELEMENT_NAME = "SummaryHTML";
	private static final String APP_CLASS_ELEMENT_NAME = "AppClass";

	public void readXML(String fileName, Project project)
	        throws IOException, DocumentException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName));
		readXML(in, project);
	}

	public void readXML(File file, Project project)
	        throws IOException, DocumentException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
		readXML(in, project);
	}

	/**
	 * Read XML data from given input stream into this
	 * object, populating the Project as a side effect.
	 * An attempt will be made to close the input stream
	 * (even if an exception is thrown).
	 *
	 * @param in      the InputStream
	 * @param project the Project
	 */
	public void readXML(InputStream in, Project project)
	        throws IOException, DocumentException {
		if (in == null) throw new IllegalArgumentException();
		if (project == null) throw new IllegalArgumentException();

		try {
			doReadXML(in, project);
		} finally {
			in.close();
		}
	}

	private void doReadXML(InputStream in, Project project) throws IOException, DocumentException {

		checkInputStream(in);

		SAXReader reader = new SAXReader();
		Document document = reader.read(in);

		Map<String, String> classToSourceFileMap = new HashMap<String, String>();

		for (Iterator i = document.getRootElement().elements().iterator(); i.hasNext();) {
			Element element = (Element) i.next();
			String elementName = element.getName();

			if (elementName.equals(SRCMAP_ELEMENT_NAME)) {
				// Note: this is just for backwards compatibility.
				classToSourceFileMap.put(element.attributeValue("classname"), element.attributeValue("srcfile"));
			} else if (elementName.equals(PROJECT_ELEMENT_NAME)) {
				project.readElement(element);
			} else if (elementName.equals(ERRORS_ELEMENT_NAME)) {
				readErrors(element);
			} else if (elementName.equals(SUMMARY_HTML_ELEMENT_NAME)) {
				setSummaryHTML(element.getText());
			} else if (elementName.equals(APP_CLASS_ELEMENT_NAME)) {
				// Ignore for backwards compatibility
			} else {
				XMLTranslator translator = XMLTranslatorRegistry.instance().getTranslator(elementName);
				if (translator == null)
					throw new DocumentException("Unknown element type: " + elementName);

				BugInstance bugInstance = (BugInstance) translator.fromElement(element);

				add(bugInstance);
			}
		}

		// For any bug instances lacking source information,
		// use the source map to add the information (if possible).
		// This is just for backwards compatibility with the old
		// SrcMap elements.
		for (Iterator<BugInstance> i = this.iterator(); i.hasNext();) {
			BugInstance bugInstance = i.next();

			for (Iterator<BugAnnotation> j = bugInstance.annotationIterator(); j.hasNext();) {
				BugAnnotation annotation = j.next();
				if (annotation instanceof SourceLineAnnotation) {
					updateSourceFile((SourceLineAnnotation) annotation, classToSourceFileMap);
				} else if (annotation instanceof MethodAnnotation) {
					SourceLineAnnotation srcLines = ((MethodAnnotation) annotation).getSourceLines();
					if (srcLines != null)
						updateSourceFile(srcLines, classToSourceFileMap);
				}
			}
		}

/*
		// notyet
		try {
			SAXBugCollectionHandler handler = new SAXBugCollectionHandler(this, project);
			XMLReader xr = new org.apache.xerces.parsers.SAXParser();

			xr.setContentHandler(handler);
			xr.setErrorHandler(handler);

			Reader reader = new InputStreamReader(in);

			xr.parse(new InputSource(reader));
		} catch (SAXException e) {
			throw new DocumentException("Parse error", e);
		}
*/

		// Presumably, project is now up-to-date
		project.setModified(false);
	}

	private static void updateSourceFile(SourceLineAnnotation annotation, Map<String, String> classToSourceFileMap) {
		if (!annotation.isSourceFileKnown()) {
			String className = annotation.getClassName();
			String sourceFile = classToSourceFileMap.get(className);
			if (sourceFile != null)
				annotation.setSourceFile(sourceFile);
		}
	}

	private void readErrors(Element element) throws DocumentException {
		Iterator i = element.elements().iterator();
		while (i.hasNext()) {
			Element child = (Element) i.next();
			String elementName = child.getName();

			if (elementName.equals(ANALYSIS_ERROR_ELEMENT_NAME)) {
				addError(child.getText());
			} else if (elementName.equals(MISSING_CLASS_ELEMENT_NAME)) {
				addMissingClass(child.getText());
			} else
				throw new DocumentException("Unknown element type: " + elementName);
		}
	}

	public void writeXML(String fileName, Project project) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
		writeXML(out, project);
	}

	public void writeXML(File file, Project project) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		writeXML(out, project);
	}

	public Document toDocument(Project project) {
		Document document = DocumentHelper.createDocument();
		Element root = document.addElement(ROOT_ELEMENT_NAME);

		// Set the version attribute of the root element
		root.addAttribute("version", Version.RELEASE);

		// Save the project information
		Element projectElement = root.addElement(PROJECT_ELEMENT_NAME);
		project.writeElement(projectElement);

		// Save all of the bug instances
		for (Iterator<BugInstance> i = this.iterator(); i.hasNext();) {
			BugInstance bugInstance = i.next();
			bugInstance.toElement(root);
		}

		// Save the error information
		Element errorsElement = root.addElement(ERRORS_ELEMENT_NAME);
		for (Iterator<String> i = errorIterator(); i.hasNext();) {
			errorsElement.addElement(ANALYSIS_ERROR_ELEMENT_NAME).setText(i.next());
		}
		for (Iterator<String> i = missingClassIterator(); i.hasNext();) {
			errorsElement.addElement(MISSING_CLASS_ELEMENT_NAME).setText(i.next());
		}

		// Save the HTML summary
		String html = getSummaryHTML();
		if (!html.equals("")) {
			document.getRootElement().addElement(SUMMARY_HTML_ELEMENT_NAME).addCDATA(html);
		}

		return document;
	}

	public void writeXML(OutputStream out, Project project) throws IOException {
		Document document = toDocument(project);

		XMLWriter writer = new XMLWriter(out, OutputFormat.createPrettyPrint());
		writer.write(document);
	}

	private void checkInputStream(InputStream in) throws IOException {
		if (in.markSupported()) {
			byte[] buf = new byte[60];
			in.mark(buf.length);

			int numRead = 0;
			while (numRead < buf.length) {
				int n = in.read(buf, numRead, buf.length - numRead);
				if (n < 0)
					throw new IOException("XML does not contain saved bug data");
				numRead += n;
			}

			in.reset();

			BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf)));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("<BugCollection"))
					return;
			}

			throw new IOException("XML does not contain saved bug data");
		}
	}

}

// vim:ts=4
