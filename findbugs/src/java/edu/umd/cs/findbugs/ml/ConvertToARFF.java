/*
 * Machine Learning support for FindBugs
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

package edu.umd.cs.findbugs.ml;

import edu.umd.cs.findbugs.CommandLine;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

/**
 * Convert a BugCollection into ARFF format.
 * See Witten and Frank, <em>Data Mining</em>, ISBN 1-55860-552-5.
 *
 * @see BugCollection
 * @see BugInstance
 * @author David Hovemeyer
 */
public class ConvertToARFF {
	// ------------------------------------------------------------
	// Helper classes
	// ------------------------------------------------------------

	private static class DataFile {
		private Document document;
		private String appName;

		public DataFile(Document document, String appName) {
			this.document = document;
			this.appName = appName;
		}

		public Document getDocument() { return document; }

		public String getAppName() { return appName; }
	}

	private static class MissingNodeException extends Exception {
		private static final long serialVersionUID = -5042140832791541208L;

		public MissingNodeException(String msg) {
			super(msg);
		}
	}

	public interface Attribute {
		public String getName();
		public void scan(Element element, String appName)
			throws MissingNodeException;
		public String getRange();
		public String getInstanceValue(Element element, String appName)
			throws MissingNodeException;
	}

	private abstract static class XPathAttribute implements Attribute {
		private String name;
		private String xpath;

		public XPathAttribute(String name, String xpath) {
			this.name = name;
			this.xpath = xpath;
		}

		public String getName() {
			return name;
		}

		public String getInstanceValue(Element element, String appName) throws MissingNodeException {
			Object value = element.selectObject(xpath);
			if (value == null)
				throw new MissingNodeException("Could not get value from element (path=" +
					xpath + ")");
			if (value instanceof List) {
				List list = (List) value;
				value = list.get(0);
			}

			if (value instanceof Node) {
				Node node = (Node) value;
				return node.getText();
			} else if (value instanceof String) {
				return (String) value;
			} else if (value instanceof Number) {
				String s = value.toString();
				if (s.endsWith(".0"))
					s = s.substring(0, s.length() - 2);
				return s;
			} else
				throw new IllegalStateException("Unexpected object returned from xpath query: " + value);
		}
	}

	public static class NominalAttribute extends XPathAttribute {
		private Set<String> possibleValueSet;

		public NominalAttribute(String name, String xpath) {
			super(name, xpath);
			this.possibleValueSet = new TreeSet<String>();
		}

		public void scan(Element element, String appName) {
			try {
				possibleValueSet.add(getInstanceValue(element, appName));
			} catch (MissingNodeException ignore) {
				// Ignore: we'll just use an n/a value for this instance
			}
		}

		public String getRange() {
			return collectionToRange(possibleValueSet);
		}

		public String getInstanceValue(Element element, String appName) throws MissingNodeException {
			return "\"" + super.getInstanceValue(element, appName) + "\"";
		}
	}

	private static final int UNCLASSIFIED = 0;
	private static final int BUG = 1;
	private static final int NOT_BUG = 2;
	private static final int HARMLESS = 4;
	private static final int HARMLESS_BUG = HARMLESS | BUG;

	public static class ClassificationAttribute implements Attribute {
		public String getName() {
			return "classification";
		}

		public void scan(Element element, String appName) throws MissingNodeException {
		}

		public String getRange() {
			return "{bug,not_bug,harmless_bug}";
		}

		public String getInstanceValue(Element element, String appName) throws MissingNodeException {
			String annotationText = element.valueOf("./UserAnnotation[text()]");
			//System.out.println("annotationText=" + annotationText);

			int state = getBugClassification(annotationText);

			if (state == NOT_BUG)
				return "not_bug";
			else if (state == BUG)
				return "bug";
			else if (state == HARMLESS_BUG)
				return "harmless_bug";
			else
				throw new MissingNodeException("Unclassified warning");
		}
	}

	public static class NumericAttribute extends XPathAttribute {
		public NumericAttribute(String name, String xpath) {
			super(name, xpath);
		}

		public void scan(Element element, String appName) throws MissingNodeException {
		}

		public String getRange() {
			return "numeric";
		}
	}

	public static class PriorityAttribute implements Attribute {
		public String getName() {
			return "priority";
		}

		public void scan(Element element, String appName) throws MissingNodeException {
		}

		public String getRange() {
			return "{low,medium,high}";
		}

		public String getInstanceValue(Element element, String appName) throws MissingNodeException {
			org.dom4j.Attribute attribute = element.attribute("priority");
			if (attribute ==  null)
				throw new MissingNodeException("Missing priority attribute");
			String value = attribute.getValue();
			try {
				int prio = Integer.parseInt(value);
				switch (prio) {
				case 1: return "high";
				case 2: return "medium";
				case 3: return "low";
				default: return "?";
				}
			} catch (NumberFormatException e) {
				throw new MissingNodeException("Invalid priority value: " + value);
			}
		}
	}

	/**
	 * An attribute that just gives each instance a unique id.
	 * Obviously, this attribute shouldn't be used as input
	 * to a learning algorithm.
	 */
	public static class IdAttribute implements Attribute {
		private int count = 0;

		public String getName() { return "id"; }
		public void scan(Element element, String appName) throws MissingNodeException { }
		public String getRange() { return "numeric"; }
		public String getInstanceValue(Element element, String appName) throws MissingNodeException {
			return String.valueOf(count++);
		}
	}

	public static class AppNameAttribute implements Attribute {
		private Set<String> appNameSet = new TreeSet<String>();

		public String getName() {
			return "appname";
		}

		public void scan(Element element, String appName)
			throws MissingNodeException {
			appNameSet.add(appName);
		}

		public String getRange() {
			return collectionToRange(appNameSet);
		}

		public String getInstanceValue(Element element, String appName)
			throws MissingNodeException {
			return "\"" + appName + "\"";
		}
	}

	public static String collectionToRange(Collection<String> collection) {
		StringBuffer buf = new StringBuffer();
		buf.append("{");
		for (Iterator<String> i = collection.iterator(); i.hasNext();) {
			if (buf.length() > 1)
				buf.append(',');
			buf.append(i.next());
		}
		buf.append("}");

		return buf.toString();
	}

	public interface AttributeCallback {
		public void apply(Attribute attribute) throws MissingNodeException, IOException;
	}

	// ------------------------------------------------------------
	// Fields
	// ------------------------------------------------------------

	private List<Attribute> attributeList;
	private boolean dropUnclassifiedWarnings;

	// ------------------------------------------------------------
	// Public methods
	// ------------------------------------------------------------

	public ConvertToARFF() {
		this.attributeList = new LinkedList<Attribute>();
		this.dropUnclassifiedWarnings = false;
	}

	public void dropUnclassifiedWarnings() {
		this.dropUnclassifiedWarnings = true;
	}

	public void addAttribute(Attribute attribute) {
		attributeList.add(attribute);
	}

	public void addNominalAttribute(String name, String xpath) {
		addAttribute(new NominalAttribute(name, xpath));
	}

	public void addClassificationAttribute() {
		addAttribute(new ClassificationAttribute());
	}

	public void addNumericAttribute(String name, String xpath) {
		addAttribute(new NumericAttribute(name, xpath));
	}

	public void addPriorityAttribute() {
		addAttribute(new PriorityAttribute());
	}

	public void addIdAttribute() {
		addAttribute(new IdAttribute());
	}

	public void addAppNameAttribute() {
		addAttribute(new AppNameAttribute());
	}

	/**
	 * Convert a single Document to ARFF format.
	 *
	 * @param relationName the relation name
	 * @param document     the Document
	 * @param appName      the application name
	 * @param out          Writer to write the ARFF output to
	 */
	public void convert(String relationName, Document document, String appName, final Writer out)
			throws IOException, MissingNodeException {
		scan(document, appName);
		generateHeader(relationName, out);
		generateInstances(document, appName, out);
	}

	/**
	 * Scan a Document to find out the ranges of attributes.
	 * All Documents must be scanned before generating the ARFF
	 * header and instances.
	 *
	 * @param document the Document
	 * @param appName  the application name
	 */
	public void scan(Document document, final String appName) throws MissingNodeException, IOException {
		List bugInstanceList = getBugInstanceList(document);

		for (Iterator i = bugInstanceList.iterator(); i.hasNext(); ) {
			final Element element = (Element) i.next();
			scanAttributeList(new AttributeCallback() {
				public void apply(Attribute attribute) throws MissingNodeException {
					attribute.scan(element, appName);
				}
			});
		}
	}

	/**
	 * Generate ARFF header.
	 * Documents must have already been scanned.
	 *
	 * @param relationName the relation name
	 * @param out          Writer to write the ARFF output to
	 */
	public void generateHeader(String relationName, final Writer out)
			throws MissingNodeException, IOException {
		out.write("@relation ");
		out.write(relationName);
		out.write("\n\n");

		scanAttributeList(new AttributeCallback() {
			public void apply(Attribute attribute) throws IOException {
				out.write("@attribute ");
				out.write(attribute.getName());
				out.write(" ");
				out.write(attribute.getRange());
				out.write("\n");
			}
		});
		out.write("\n");

		out.write("@data\n");
	}

	/**
	 * Generate instances from given Document.
	 * Document should already have been scanned, and the ARFF header generated.
	 *
	 * @param document the Document
	 * @param appName  the application name
	 * @param out      Writer to write the ARFF output to
	 */
	public void generateInstances(Document document, final String appName, final Writer out)
			throws MissingNodeException, IOException {
		List bugInstanceList = getBugInstanceList(document);

		for (Iterator i = bugInstanceList.iterator(); i.hasNext(); ) {
			final Element element = (Element) i.next();
			scanAttributeList(new AttributeCallback() {
				boolean first = true;
				public void apply(Attribute attribute) throws IOException {
					if (!first)
						out.write(",");
					first = false;
					String value;
					try {
						value = attribute.getInstanceValue(element, appName);
					} catch (MissingNodeException e) {
						value = "?";
					}
					out.write(value);
				}
			});
			out.write("\n");
		}
	}

	/**
	 * Apply a callback to all Attributes.
	 *
	 * @param callback the callback
	 */
	public void scanAttributeList(AttributeCallback callback)
			throws MissingNodeException, IOException {
		for (Iterator<Attribute> i = attributeList.iterator(); i.hasNext();) {
			Attribute attribute = i.next();
			callback.apply(attribute);
		}
	}

	public void addDefaultAttributes() {
		// This conversion scheme is arbitrary.
		// FIXME: method and field signatures?
		addIdAttribute();
		addNominalAttribute("bugtype", "@type");
		addNominalAttribute("class", "./Class[1]/@classname");
		addNominalAttribute("methodname", "./Method[1]/@name");
		addNominalAttribute("auxmethodclass", "./Method[2]/@classname");
		addNominalAttribute("auxmethodname", "./Method[2]/@name");
		addNominalAttribute("fieldclass", "./Field[1]/@classname");
		addNominalAttribute("fieldname", "./Field[1]/@name");
		//addNumericAttribute("priority", "@priority");
		addPriorityAttribute();
		addClassificationAttribute();
	}

	// ------------------------------------------------------------
	// Implementation
	// ------------------------------------------------------------

	private static int getBugClassification(String annotationText) {
		StringTokenizer tok = new StringTokenizer(annotationText, " \t\r\n\f.,:;-");

		int state = UNCLASSIFIED;

		while (tok.hasMoreTokens()) {
			String s = tok.nextToken();
			if (s.equals("BUG"))
				state |= BUG;
			else if (s.equals("NOT_BUG"))
				state |= NOT_BUG;
			else if (s.equals("HARMLESS"))
				state |= HARMLESS;
		}

		if ((state & NOT_BUG) != 0)
			return NOT_BUG;
		else if ((state & BUG) != 0)
			return ((state & HARMLESS) != 0) ? HARMLESS_BUG : BUG;
		else
			return UNCLASSIFIED;
	}

	private List getBugInstanceList(Document document) {
		List bugInstanceList = document.selectNodes("/BugCollection/BugInstance");
		if (dropUnclassifiedWarnings) {
			for (Iterator i = bugInstanceList.iterator(); i.hasNext(); ) {
				Element element = (Element) i.next();
				String annotationText = element.valueOf("./UserAnnotation[text()]");
				int classification = getBugClassification(annotationText);
				if (classification == UNCLASSIFIED)
					i.remove();
			}
		}
		return bugInstanceList;
	}

	private static class C2ACommandLine extends CommandLine {
		private ConvertToARFF converter = new ConvertToARFF();

		public C2ACommandLine() {
			addSwitch("-train", "drop unclassified warnings");
			addSwitch("-id", "add unique id attribute");
			addSwitch("-app", "add application name attribute");
			addSwitch("-default", "add default attributes");
			addOption("-nominal", "attrName,xpath", "add a nominal attribute");
			addOption("-numeric", "attrName,xpath", "add a numeric attribute");
			addSwitch("-classification", "add bug classification attribute");
			addSwitch("-priority", "add priority attribute");
		}

		public ConvertToARFF getConverter() {
			return converter;
		}

		protected void handleOption(String option, String optionExtraPart)
				throws IOException {
			if (option.equals("-train")) {
				converter.dropUnclassifiedWarnings();
			} else if (option.equals("-id")) {
				converter.addIdAttribute();
			} else if (option.equals("-app")) {
				converter.addAppNameAttribute();
			} else if (option.equals("-default")) {
				converter.addDefaultAttributes();
			} else if (option.equals("-classification")) {
				converter.addClassificationAttribute();
			} else if (option.equals("-priority")) {
				converter.addPriorityAttribute();
			}
		}

		protected void handleOptionWithArgument(String option, String argument)
				throws IOException {
			if (option.equals("-nominal") || option.equals("-numeric")) {
				int comma = argument.indexOf(',');
				if (comma < 0) {
					throw new IllegalArgumentException("Missing comma separating attribute name and xpath in " +
						option + " option: " + argument);
				}
				String attrName = argument.substring(0, comma);
				String xpath = argument.substring(comma + 1);
				converter.addAttribute(option.equals("-nominal")
					? new NominalAttribute(attrName, xpath)
					: new NumericAttribute(attrName, xpath));
			}
		}

		public void printUsage(PrintStream out) {
			out.println("Usage: " + ConvertToARFF.class.getName() +
				" [options] <relation name> <output file> <findbugs results> [<findbugs results>...]");
			super.printUsage(out);
		}
	}

	public static String toAppName(String fileName) {
		// Remove file extension, if any
		int lastDot = fileName.lastIndexOf('.');
		if (lastDot >= 0)
			fileName = fileName.substring(0, lastDot);
		return fileName;
	}

	public static void main(String[] argv) throws Exception {
		// Expand any option files
		argv = CommandLine.expandOptionFiles(argv, true, true);

		// Parse command line arguments
		C2ACommandLine commandLine = new C2ACommandLine();
		int argCount = commandLine.parse(argv);
		if (argCount > argv.length - 3) {
			commandLine.printUsage(System.err);
			System.exit(1);
		}
		String relationName = argv[argCount++];
		String outputFileName = argv[argCount++];

		// Create the converter
		ConvertToARFF converter = commandLine.getConverter();

		// Open output file
		Writer out = new OutputStreamWriter(new BufferedOutputStream(
			new FileOutputStream(outputFileName)));

		// Read documents,
		// scan documents to find ranges of attributes
		List<DataFile> dataFileList = new ArrayList<DataFile>();
		while (argCount < argv.length) {
			String fileName = argv[argCount++];

			// Read input file as dom4j tree
			SAXReader reader = new SAXReader();
			Document document = reader.read(fileName);

			DataFile dataFile = new DataFile(document, toAppName(fileName));
			dataFileList.add(dataFile);

			converter.scan(dataFile.getDocument(), dataFile.getAppName());
		}

		// Generate ARFF header
		converter.generateHeader(relationName, out);

		// Generate instances from each document
		for (Iterator<DataFile> i = dataFileList.iterator(); i.hasNext(); ) {
			DataFile dataFile = i.next();
			converter.generateInstances(dataFile.getDocument(), dataFile.getAppName(), out);
		}

		out.close();
	}

}

// vim:ts=4
