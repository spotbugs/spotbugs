/*
 * Machine Learning support for FindBugs
 * Copyright (C) 2004,2005 University of Maryland
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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.config.CommandLine;

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
				if (list.size() == 0)
					throw new MissingNodeException("Could not get value from element (path=" +
							xpath + ")");
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

		@Override
		public String getInstanceValue(Element element, String appName) throws MissingNodeException {
			return "\"" + super.getInstanceValue(element, appName) + "\"";
		}
	}
	
	public static class BooleanAttribute extends XPathAttribute {
		public BooleanAttribute(String name, String xpath) {
			super(name, xpath);
		}

		public void scan(Element element, String appName) throws MissingNodeException {
			// Nothing to do.
		}

		public String getRange() {
			return "{true, false}";
		}
		
		//@Override
		@Override
		public String getInstanceValue(Element element, String appName) throws MissingNodeException {
			try {
				String value = super.getInstanceValue(element, appName);
				return "\"" + Boolean.valueOf(value).toString() + "\"";
			} catch (MissingNodeException e) {
				return "\"false\"";
			}
		}
	}

	private static final int UNCLASSIFIED = 0;
	private static final int BUG = 1;
	private static final int NOT_BUG = 2;
	private static final int HARMLESS = 4;
	private static final int HARMLESS_BUG = HARMLESS | BUG;
	
	public static abstract class AbstractClassificationAttribute implements Attribute {

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ml.ConvertToARFF.Attribute#getName()
		 */
		public String getName() {
			return "classification";
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ml.ConvertToARFF.Attribute#scan(org.dom4j.Element, java.lang.String)
		 */
		public void scan(Element element, String appName) throws MissingNodeException {
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ml.ConvertToARFF.Attribute#getInstanceValue(org.dom4j.Element, java.lang.String)
		 */
		public String getInstanceValue(Element element, String appName) throws MissingNodeException {
			String annotationText = element.valueOf("./UserAnnotation[text()]");
			//System.out.println("annotationText=" + annotationText);

			int state = getBugClassification(annotationText);
			return bugToString(state);
		}
		
		protected abstract String bugToString(int bugType) throws MissingNodeException;
		
	}

	public static class ClassificationAttribute extends AbstractClassificationAttribute {
		public String getRange() {
			return "{bug,not_bug,harmless_bug}";
		}
		
		@Override
		protected String bugToString(int state) throws MissingNodeException {
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
	
	public static class BinaryClassificationAttribute extends AbstractClassificationAttribute {
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ml.ConvertToARFF.Attribute#getRange()
		 */
		public String getRange() {
			return "{bug, not_bug}";
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ml.ConvertToARFF.AbstractClassificationAttribute#bugToString(int)
		 */
		@Override
		protected String bugToString(int state) throws MissingNodeException {
			if (state == BUG)
				return "bug";
			else if (state == NOT_BUG || state == HARMLESS_BUG)
				return "not_bug";
			else
				throw new MissingNodeException("unclassified warning");
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
	 * The application name is prepended, so each unique id
	 * really unique, even across applications.
	 * Obviously, this attribute shouldn't be used as input
	 * to a learning algorithm.
	 * 
	 * <p>Uses the Element's uid attribute if it has one.</p>
	 */
	public static class IdAttribute implements Attribute {
		private TreeSet<String> possibleValueSet = new TreeSet<String>();
		
		private boolean scanning = true;
		private int count = 0;

		public String getName() { return "id"; }
		
		public void scan(Element element, String appName) throws MissingNodeException {
			possibleValueSet.add(instanceValue(element, appName));
		}
		
		public String getRange() { return collectionToRange(possibleValueSet); }
		
		public String getInstanceValue(Element element, String appName) throws MissingNodeException {
			if (scanning) {
				count = 0;
				scanning = false;
			}
			return instanceValue(element, appName);
		}
		
		private String instanceValue(Element element, String appName) {
			String nextId;

			org.dom4j.Attribute uidAttr= element.attribute("uid");
			if (uidAttr != null) {
				nextId = uidAttr.getValue();
			} else {
				nextId = String.valueOf(count++);
			}

			return "\"" + appName + "-" + nextId + "\"";
		}
	}
	
	public static class IdStringAttribute implements Attribute {

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ml.ConvertToARFF.Attribute#getName()
		 */
		public String getName() {
			return "ids";
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ml.ConvertToARFF.Attribute#scan(org.dom4j.Element, java.lang.String)
		 */
		public void scan(Element element, String appName) throws MissingNodeException {
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ml.ConvertToARFF.Attribute#getRange()
		 */
		public String getRange() {
			return "string";
		}
		
		int count = 0;

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ml.ConvertToARFF.Attribute#getInstanceValue(org.dom4j.Element, java.lang.String)
		 */
		public String getInstanceValue(Element element, String appName) throws MissingNodeException {
			String value;
			org.dom4j.Attribute uidAttr = element.attribute("uid");
			if (uidAttr == null) {
				value = String.valueOf(count++);
			} else {
				value = uidAttr.getStringValue();
			}
			
			return "\"" + appName + "-" + value + "\"";
		}
		
	}

	private static final String RANDOM_CHARS =
		"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	
	public static class RandomIdAttribute implements Attribute {
		
		private Random rng = new Random();
		private IdentityHashMap<Element, String> idMap = new IdentityHashMap<Element, String>();

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ml.ConvertToARFF.Attribute#getName()
		 */
		public String getName() {
			return "idr";
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ml.ConvertToARFF.Attribute#scan(org.dom4j.Element, java.lang.String)
		 */
		public void scan(Element element, String appName) throws MissingNodeException {
			idMap.put(element, generateId());
		}

		private String generateId() {
			StringBuffer buf = new StringBuffer();
			
			for (int i = 0; i < 20; ++i) {
				char c = RANDOM_CHARS.charAt(rng.nextInt(RANDOM_CHARS.length()));
				buf.append(c);
			}
			
			return buf.toString();
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ml.ConvertToARFF.Attribute#getRange()
		 */
		public String getRange() {
			TreeSet<String> range = new TreeSet<String>();
			range.addAll(idMap.values());
			if (range.size() != idMap.size())
				throw new IllegalStateException("id collision!");
			return collectionToRange(range);
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ml.ConvertToARFF.Attribute#getInstanceValue(org.dom4j.Element, java.lang.String)
		 */
		public String getInstanceValue(Element element, String appName) throws MissingNodeException {
			String id = idMap.get(element);
			if (id == null)
				throw new IllegalStateException("Element not scanned?");
			return "\"" + id + "\"";
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
		for (String aCollection : collection) {
			if (buf.length() > 1)
				buf.append(',');
			buf.append(aCollection);
		}
		buf.append("}");

		return buf.toString();
	}

	public interface AttributeCallback {
		public void apply(Attribute attribute) throws MissingNodeException, IOException;
	}

	// ------------------------------------------------------------
	// Constants
	// ------------------------------------------------------------
	
	private static final String DEFAULT_NODE_SELECTION_XPATH = "/BugCollection/BugInstance";

	// ------------------------------------------------------------
	// Fields
	// ------------------------------------------------------------

	private List<Attribute> attributeList;
	private String nodeSelectionXpath;
	private boolean dropUnclassifiedWarnings;
	private String appName;

	// ------------------------------------------------------------
	// Public methods
	// ------------------------------------------------------------

	public ConvertToARFF() {
		this.attributeList = new LinkedList<Attribute>();
		this.nodeSelectionXpath = DEFAULT_NODE_SELECTION_XPATH;
		this.dropUnclassifiedWarnings = false;
	}
	
	public void setAppName(String appName) {
		this.appName = appName;
	}
	
	/**
	 * Set the xpath expression used to select BugInstance nodes.
	 * 
	 * @param nodeSelectionXpath the node selection xpath expression
	 */
	public void setNodeSelectionXpath(String nodeSelectionXpath) {
		this.nodeSelectionXpath = nodeSelectionXpath;
	}
	
	public int getNumAttributes() {
		return attributeList.size();
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
	
	public void addBooleanAttribute(String name, String xpath) {
		addAttribute(new BooleanAttribute(name, xpath));
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
		List<Element> bugInstanceList = getBugInstanceList(document);

		for (final Element element : bugInstanceList) {
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
		List<Element> bugInstanceList = getBugInstanceList(document);

		for (final Element element : bugInstanceList) {
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
		for (Attribute attribute : attributeList) {
			callback.apply(attribute);
		}
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

	private List<Element> getBugInstanceList(Document document) {
		List <Element>bugInstanceList = document.selectNodes(nodeSelectionXpath);
		if (dropUnclassifiedWarnings) {
			for (Iterator<Element> i = bugInstanceList.iterator(); i.hasNext(); ) {
				Element element = i.next();
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
			addOption("-select","xpath expression","select BugInstance elements");
			addSwitch("-train", "drop unclassified warnings");
			addSwitch("-id", "add unique id attribute (as nominal)");
			addSwitch("-ids", "add unique id attribute (as string)");
			addSwitch("-idr", "add random unique id attribtue (as nominal)");
			addSwitch("-app", "add application name attribute");
			addOption("-nominal", "attrName,xpath", "add a nominal attribute");
			addOption("-boolean", "attrName,xpath", "add a boolean attribute");
			addOption("-numeric", "attrName,xpath", "add a numeric attribute");
			addSwitch("-classification", "add bug classification attribute");
			addSwitch("-binclass", "add binary (bug/not_bug) classification attribute");
			addSwitch("-priority", "add priority attribute");
			addOption("-appname", "app name", "set application name of all tuples");
		}

		public ConvertToARFF getConverter() {
			return converter;
		}

		@Override
		protected void handleOption(String option, String optionExtraPart)
				throws IOException {
			if (option.equals("-train")) {
				converter.dropUnclassifiedWarnings();
			} else if (option.equals("-id")) {
				converter.addIdAttribute();
			} else if (option.equals("-ids")) {
				converter.addAttribute(new IdStringAttribute());
			} else if (option.equals("-idr")) {
				converter.addAttribute(new RandomIdAttribute());
			} else if (option.equals("-app")) {
				converter.addAppNameAttribute();
			} else if (option.equals("-classification")) {
				converter.addClassificationAttribute();
			} else if (option.equals("-binclass")) {
				converter.addAttribute(new BinaryClassificationAttribute());
			} else if (option.equals("-priority")) {
				converter.addPriorityAttribute();
			}
		}
		
		private interface XPathAttributeCreator {
			public Attribute create(String name, String xpath);
		}

		@Override
		protected void handleOptionWithArgument(String option, String argument)
				throws IOException {
			
			if (option.equals("-select")) {
				converter.setNodeSelectionXpath(argument);
			} else if (option.equals("-nominal")) {
				addXPathAttribute(option, argument, new XPathAttributeCreator() {
					public Attribute create(String name,String xpath) {
						return new NominalAttribute(name, xpath);
					}
				});
			} else if (option.equals("-boolean")) {
				addXPathAttribute(option, argument, new XPathAttributeCreator() {
					public Attribute create(String name,String xpath) {
						return new BooleanAttribute(name, xpath);
					}
				});
			} else if (option.equals("-numeric")) {
				addXPathAttribute(option, argument, new XPathAttributeCreator(){
					public Attribute create(String name,String xpath) {
						return new NumericAttribute(name, xpath);
					}
				});
			} else if (option.equals("-appname")) {
				converter.setAppName(argument);
			}
		}
		
		protected void addXPathAttribute(String option, String argument, XPathAttributeCreator creator) {
			int comma = argument.indexOf(',');
			if (comma < 0) {
				throw new IllegalArgumentException("Missing comma separating attribute name and xpath in " +
					option + " option: " + argument);
			}
			String attrName = argument.substring(0, comma);
			String xpath = argument.substring(comma + 1);
			converter.addAttribute(creator.create(attrName, xpath));
		}

		public void printUsage(PrintStream out) {
			out.println("Usage: " + ConvertToARFF.class.getName() +
				" [options] <relation name> <output file> <findbugs results> [<findbugs results>...]");
			super.printUsage(out);
		}
	}

	public String toAppName(String fileName) {
		if (appName != null)
			return appName;
		
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
		if (converter.getNumAttributes() == 0) {
			throw new IllegalArgumentException("No attributes specified!");
		}

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

			DataFile dataFile = new DataFile(document, converter.toAppName(fileName));
			dataFileList.add(dataFile);

			converter.scan(dataFile.getDocument(), dataFile.getAppName());
		}

		// Generate ARFF header
		converter.generateHeader(relationName, out);

		// Generate instances from each document
		for (DataFile dataFile : dataFileList) {
			converter.generateInstances(dataFile.getDocument(), dataFile.getAppName(), out);
		}

		out.close();
	}

}

// vim:ts=4
