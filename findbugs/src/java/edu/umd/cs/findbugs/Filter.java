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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class Filter extends OrMatcher {
	private static final boolean DEBUG = Boolean.getBoolean("filter.debug");

	public Filter(String fileName) throws IOException, FilterException {
		parse(fileName);
	}

	private void parse(String fileName) throws IOException, FilterException {

		Document filterDoc = null;

		try {
			SAXReader reader = new SAXReader();
			filterDoc = reader.read(new BufferedInputStream(new FileInputStream(fileName)));
		} catch (DocumentException e) {
			throw new FilterException("Couldn't parse filter file " + fileName, e);
		}

		// Iterate over Match elements
		Iterator i = filterDoc.selectNodes("/FindBugsFilter/Match").iterator();
		while (i.hasNext()) {
			Element matchNode = (Element) i.next();

			AndMatcher matchMatcher = new AndMatcher();

			// Each match node must have either "class" or "classregex" attributes
			Matcher classMatcher = null;
			String classAttr = matchNode.valueOf("@class");
			if (!classAttr.equals("")) {
				classMatcher = new ClassMatcher(classAttr);
			} else {
				String classRegex = matchNode.valueOf("@classregex");
				if (!classRegex.equals(""))
					classMatcher = new ClassRegexMatcher(classRegex);
			}

			if (classMatcher == null)
				throw new FilterException("Match node must specify either class or classregex attribute");

			matchMatcher.addChild(classMatcher);

			if (DEBUG) System.out.println("Match node");

			// Iterate over child elements of Match node.
			Iterator j = matchNode.elementIterator();
			while (j.hasNext()) {
				Element child = (Element) j.next();
				Matcher matcher = getMatcher(child);
				matchMatcher.addChild(matcher);
			}

			// Add the Match matcher to the overall Filter
			this.addChild(matchMatcher);
		}

	}

	private Matcher getMatcher(Element element) throws FilterException {
		// These will be either BugCode, Method, or Or elements.
		String name = element.getName();
		if (name.equals("BugCode")) {
			return new BugCodeMatcher(element.valueOf("@name"));
		} else if (name.equals("Method")) {
			Attribute nameAttr = element.attribute("name");
			Attribute paramsAttr = element.attribute("params");
			Attribute returnsAttr = element.attribute("returns");

			if (nameAttr == null)
				throw new FilterException("Missing name attribute in Method element");

			if ((paramsAttr != null || returnsAttr != null) && (paramsAttr == null || returnsAttr == null))
				throw new FilterException("Method element must have both params and returns attributes if either is used");

			if (paramsAttr == null)
				return new MethodMatcher(nameAttr.getValue());
			else
				return new MethodMatcher(nameAttr.getValue(), paramsAttr.getValue(), returnsAttr.getValue());
		} else if (name.equals("Or")) {
			OrMatcher orMatcher = new OrMatcher();
			Iterator i = element.elementIterator();
			while (i.hasNext()) {
				orMatcher.addChild(getMatcher((Element) i.next()));
			}
			return orMatcher;
		} else
			throw new FilterException("Unknown element: " + name);
	}

	public static void main(String[] argv) {
		try {
			if (argv.length != 1) {
				System.err.println("Usage: " + Filter.class.getName() + " <filename>");
				System.exit(1);
			}

			new Filter(argv[0]);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}

// vim:ts=4
