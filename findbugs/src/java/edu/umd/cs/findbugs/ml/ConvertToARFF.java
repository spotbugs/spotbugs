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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
	private class MissingNodeException extends Exception {
		public MissingNodeException(String msg) {
			super(msg);
		}
	}

	public interface Attribute {
		public String getName();
		public void scan(Element element) throws MissingNodeException;
		public String getRange();
		public String getInstanceValue(Element element) throws MissingNodeException;
	}

	public class NominalAttribute implements Attribute {
		private String name;
		private String xpath;
		private Set<String> possibleValueSet;

		public NominalAttribute(String name, String xpath) {
			this.name = name;
			this.xpath = xpath;
			this.possibleValueSet = new TreeSet<String>();
		}

		public String getName() {
			return name;
		}

		public void scan(Element element) {
			try {
				possibleValueSet.add(getInstanceValue(element));
			} catch (MissingNodeException ignore) {
				// Ignore: we'll just use an n/a value for this instance
			}
		}

		public String getRange() {
			StringBuffer buf = new StringBuffer();
			buf.append("{");
			for (Iterator<String> i = possibleValueSet.iterator(); i.hasNext();) {
				if (!buf.toString().equals("{"))
					buf.append(",");
				buf.append('"');
				buf.append(i.next());
				buf.append('"');
			}
			buf.append("}");

			return buf.toString();
		}

		public String getInstanceValue(Element element) throws MissingNodeException {
			Node node = element.selectSingleNode(xpath);
			if (node == null)
				throw new MissingNodeException("Could not get value from element (path=" +
					xpath + ")");
			return node.getText();
		}
	}

	public interface AttributeCallback {
		public void apply(Attribute attribute) throws MissingNodeException, IOException;
	}

	private List<Attribute> attributeList;

	public ConvertToARFF() {
		this.attributeList = new LinkedList<Attribute>();
	}

	public void addAttribute(Attribute attribute) {
		attributeList.add(attribute);
	}

	public void addNominalAttribute(String name, String xpath) {
		addAttribute(new NominalAttribute(name, xpath));
	}

	public void convert(String relationName, Document document, final Writer out)
			throws IOException, MissingNodeException {
		List bugInstanceList = document.selectNodes("/BugCollection/BugInstance");

		out.write("@relation ");
		out.write(relationName);
		out.write("\n\n");

		for (Iterator i = bugInstanceList.iterator(); i.hasNext(); ) {
			final Element element = (Element) i.next();
			scanAttributeList(new AttributeCallback() {
				public void apply(Attribute attribute) throws MissingNodeException {
					attribute.scan(element);
				}
			});
		}

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
						value = "\"" + attribute.getInstanceValue(element) + "\"";
					} catch (MissingNodeException e) {
						value = "?";
					}
					out.write(value);
				}
			});
			out.write("\n");
		}
	}

	public void scanAttributeList(AttributeCallback callback)
			throws MissingNodeException, IOException {
		for (Iterator<Attribute> i = attributeList.iterator(); i.hasNext();) {
			Attribute attribute = i.next();
			callback.apply(attribute);
		}
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 3) {
			System.err.println("Usage: " + ConvertToARFF.class.getName() +
				" <findbugs results> <relation name> <output file>");
			System.exit(1);
		}

		String fileName = argv[0];
		String relationName = argv[1];
		String outputFileName = argv[2];

		SAXReader reader = new SAXReader();
		Document document = reader.read(fileName);

		ConvertToARFF converter = new ConvertToARFF();

		// FIXME: this conversion scheme is arbitrary
		// FIXME: method and field signatures?
		converter.addNominalAttribute("bugtype", "@type");
		converter.addNominalAttribute("class", "./Class[1]/@classname");
		converter.addNominalAttribute("methodname", "./Method[1]/@name");
		converter.addNominalAttribute("auxmethodclass", "./Method[2]/@classname");
		converter.addNominalAttribute("auxmethodname", "./Method[2]/@name");
		converter.addNominalAttribute("fieldclass", "./Field[1]/@classname");
		converter.addNominalAttribute("fieldname", "./Field[1]/@name");

		Writer out = new OutputStreamWriter(new BufferedOutputStream(
			new FileOutputStream(outputFileName)));

		converter.convert(relationName, document, out);
		out.close();
	}
}

// vim:ts=4
