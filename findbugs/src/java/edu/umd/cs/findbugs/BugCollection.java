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

import edu.umd.cs.findbugs.xml.Dom4JXMLOutput;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutputUtil;

import java.io.*;
import java.util.*;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Abstract base class for collections of BugInstance objects
 * and error messages associated with analysis.
 * Supports reading and writing XML files.
 *
 * @author David Hovemeyer
 * @see BugInstance
 */
public abstract class BugCollection {

	/**
	 * Add a Collection of BugInstances to this BugCollection object.
	 *
	 * @param collection the Collection of BugInstances to add
	 */
	public void addAll(Collection<BugInstance> collection) {
		Iterator<BugInstance> i = collection.iterator();
		while (i.hasNext()) {
			add(i.next());
		}
	}

	/**
	 * Add a BugInstance to this BugCollection.
	 *
	 * @param bugInstance the BugInstance
	 * @return true if the BugInstance was added, or false if a matching
	 *         BugInstance was already in the BugCollection
	 */
	public abstract boolean add(BugInstance bugInstance);

	/**
	 * Remove a BugInstance from this BugCollection.
	 *
	 * @param bugInstance the BugInstance
	 * @return true if the BugInstance was removed, or false if
	 *         it (or an equivalent BugInstance) was not present originally
	 */
	public abstract boolean remove(BugInstance bugInstance);

	/**
	 * Return an Iterator over all the BugInstance objects in
	 * the BugCollection.
	 */
	public abstract Iterator<BugInstance> iterator();

	/**
	 * Return the Collection storing the BugInstance objects.
	 */
	public abstract Collection<BugInstance> getCollection();

	/**
	 * Add an analysis error message.
	 *
	 * @param message the error message
	 */
	public abstract void addError(String message);

	/**
	 * Add a missing class message.
	 *
	 * @param message the missing class message
	 */
	public abstract void addMissingClass(String message);

	/**
	 * Return an Iterator over error messages.
	 */
	public abstract Iterator<String> errorIterator();

	/**
	 * Return an Iterator over missing class messages.
	 */
	public abstract Iterator<String> missingClassIterator();

	/**
	 * Set the summary HTML text.
	 */
	public abstract void setSummaryHTML(String html);

	/**
	 * Get the summary HTML text.
	 */
	public abstract String getSummaryHTML();

	static final String ROOT_ELEMENT_NAME = "BugCollection";
	static final String SRCMAP_ELEMENT_NAME = "SrcMap";
	static final String PROJECT_ELEMENT_NAME = "Project";
	static final String ERRORS_ELEMENT_NAME = "Errors";
	static final String ANALYSIS_ERROR_ELEMENT_NAME = "AnalysisError";
	static final String MISSING_CLASS_ELEMENT_NAME = "MissingClass";
	static final String SUMMARY_HTML_ELEMENT_NAME = "SummaryHTML";
	static final String APP_CLASS_ELEMENT_NAME = "AppClass";

	/**
	 * Read XML data from given file into this object,
	 * populating given Project as a side effect.
	 *
	 * @param fileName name of the file to read
	 * @param project  the Project
	 */
	public void readXML(String fileName, Project project)
	        throws IOException, DocumentException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName));
		readXML(in, project);
	}

	/**
	 * Read XML data from given file into this object,
	 * populating given Project as a side effect.
	 *
	 * @param file    the file
	 * @param project the Project
	 */
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

		try {
			SAXBugCollectionHandler handler = new SAXBugCollectionHandler(this, project);

			// FIXME: for now, use dom4j's XML parser
			XMLReader xr = new org.dom4j.io.aelfred.SAXDriver();

			xr.setContentHandler(handler);
			xr.setErrorHandler(handler);

			Reader reader = new InputStreamReader(in);

			xr.parse(new InputSource(reader));
		} catch (SAXException e) {
			// FIXME: throw SAXException from method?
			throw new DocumentException("Parse error", e);
		}

		// Presumably, project is now up-to-date
		project.setModified(false);
	}

	/**
	 * Write this BugCollection to a file as XML.
	 *
	 * @param fileName the file to write to
	 * @param project  the Project from which the BugCollection was generated
	 */
	public void writeXML(String fileName, Project project) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
		writeXML(out, project);
	}

	/**
	 * Write this BugCollection to a file as XML.
	 *
	 * @param file    the file to write to
	 * @param project the Project from which the BugCollection was generated
	 */
	public void writeXML(File file, Project project) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		writeXML(out, project);
	}

	/**
	 * Convert the BugCollection into a dom4j Document object.
	 *
	 * @param project the Project from which the BugCollection was generated
	 * @return the Document representing the BugCollection as a dom4j tree
	 */
	public Document toDocument(Project project) {
		DocumentFactory docFactory = new DocumentFactory();
		Document document = docFactory.createDocument();
		Dom4JXMLOutput treeBuilder = new Dom4JXMLOutput(document);

		try {
			writeXML(treeBuilder, project);
		} catch (IOException e) {
			// Can't happen
		}

		return document;
	}

	/**
	 * Write the BugCollection to given output stream as XML.
	 * The output stream will be closed, even if an exception is thrown.
	 *
	 * @param out     the OutputStream to write to
	 * @param project the Project from which the BugCollection was generated
	 */
	public void writeXML(OutputStream out, Project project) throws IOException {
		XMLOutput xmlOutput = new OutputStreamXMLOutput(out);

		writeXML(xmlOutput, project);
	}

	/**
	 * Write the BugCollection to an XMLOutput object.
	 * The finish() method of the XMLOutput object is guaranteed
	 * to be called.
	 *
	 * @param xmlOutput the XMLOutput object
	 * @param project   the Project from which the BugCollection was generated
	 */
	public void writeXML(XMLOutput xmlOutput, Project project) throws IOException {
		try {
			xmlOutput.beginDocument();
			xmlOutput.openTag(ROOT_ELEMENT_NAME,
				new XMLAttributeList().addAttribute("version",Version.RELEASE));

			project.writeXML(xmlOutput);

			// Write BugInstances
			XMLOutputUtil.writeCollection(xmlOutput, getCollection());

			// Errors, missing classes
			xmlOutput.openTag(ERRORS_ELEMENT_NAME);
			XMLOutputUtil.writeElementList(xmlOutput, ANALYSIS_ERROR_ELEMENT_NAME,
				errorIterator());
			XMLOutputUtil.writeElementList(xmlOutput, MISSING_CLASS_ELEMENT_NAME,
				missingClassIterator());
			xmlOutput.closeTag(ERRORS_ELEMENT_NAME);

			// Summary HTML
			String html = getSummaryHTML();
			if (!html.equals("")) {
				xmlOutput.openTag(SUMMARY_HTML_ELEMENT_NAME);
				xmlOutput.writeCDATA(html);
				xmlOutput.closeTag(SUMMARY_HTML_ELEMENT_NAME);
			}

			xmlOutput.closeTag(ROOT_ELEMENT_NAME);
		} finally {
			xmlOutput.finish();
		}
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
