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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.dom4j.Element;
import org.dom4j.Node;

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

	private interface Attribute {
		public String getName();
		public void scan(Element element) throws MissingNodeException;
		public String getRange();
		public String getInstanceValue(Element element) throws MissingNodeException;
	}

	private class NominalAttribute implements Attribute {
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

		public void scan(Element element) throws MissingNodeException {
			possibleValueSet.add(getInstanceValue(element));
		}

		public String getRange() {
			StringBuffer buf = new StringBuffer();
			buf.append("{");
			for (Iterator<String> i = possibleValueSet.iterator(); i.hasNext();) {
				if (!buf.toString().equals("{"))
					buf.append(",");
				buf.append(i.next());
			}
			buf.append("}");

			return buf.toString();
		}

		public String getInstanceValue(Element element) throws MissingNodeException {
			Node node = element.selectSingleNode(xpath);
			if (node == null)
				throw new MissingNodeException("Could not get value from element");
			return node.getText();
		}
	}
}

// vim:ts=4
