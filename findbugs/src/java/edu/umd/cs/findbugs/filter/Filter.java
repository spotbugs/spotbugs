/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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

package edu.umd.cs.findbugs.filter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.IdentityHashMap;
import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SAXBugCollectionHandler;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.util.Strings;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Filter to match a subset of BugInstances.
 * The filter criteria are read from an XML file.
 * 
 * @author David Hovemeyer
 */

public class Filter extends OrMatcher {
	private static final boolean DEBUG = SystemProperties.getBoolean("filter.debug");

	private IdentityHashMap<Matcher, Boolean> disabled = new IdentityHashMap<Matcher, Boolean>();
	
	/**
	 * Constructor for empty filter
	 * 
	 */
	public Filter() {

	}
	
	@Override
    public int hashCode() {
	    final int prime = 31;
	    int result = super.hashCode();
	    result = prime * result + ((disabled == null) ? 0 : disabled.hashCode());
	    return result;
    }

	@Override
    public boolean equals(Object obj) {
	    if (this == obj)
		    return true;
	    if (!super.equals(obj))
		    return false;
	    if (!(obj instanceof Filter))
		    return false;
	    final Filter other = (Filter) obj;
	    if (disabled == null) {
		    if (other.disabled != null)
			    return false;
	    } else if (!disabled.equals(other.disabled))
		    return false;
	    return true;
    }

	public boolean isEmpty() {
		return super.numberChildren() == 0;
	}
	
	public void setEnabled(Matcher m, boolean value) {
		if (value) enable(m);
		else disable(m);
	}
	public void disable(Matcher m) {
		disabled.put(m, true);
	}
	public boolean isEnabled(Matcher m) {
		return !disabled.containsKey(m);
	}
	public void enable(Matcher m) {
		disabled.remove(m);
	}
	public static Filter parseFilter(String fileName) throws IOException {
		return new Filter(fileName);
	}
	/**
	 * Constructor.
	 * 
	 * @param fileName name of the filter file
	 * @throws IOException
	 * @throws SAXException 
	 * @throws FilterException
	 */
	public Filter(String fileName) throws IOException {
		try {
	        parse(fileName);
	        if (false) System.out.println("Parsed: " + this);
        } catch (SAXException e) {
	        throw new IOException(e.getMessage());
        }
	}

	
	public boolean contains(Matcher child) {
		return children.contains(child);
	}
	
	/**
	 * Add if not present, but do not enable if already present and disabled
	 * @param child
	 */
	public void softAdd(Matcher child) {
		super.addChild(child);
	}
	@Override
	public void addChild(Matcher child) {
		super.addChild(child);
		enable(child);
	}
	@Override
	public void removeChild(Matcher child) {
		enable(child);//Remove from disabled before removing it
		super.removeChild(child);		
	}
	@Override
	public void clear(){
		disabled.clear();
		super.clear();
	}
	@Override
	public boolean match(BugInstance bugInstance) {
		Iterator<Matcher> i = childIterator();
		while (i.hasNext()) {
			Matcher child = i.next();
			if (isEnabled(child) 
					&& child.match(bugInstance))
				return true;
		}
		return false;
	}
	
	/**
	 * Parse and load the given filter file.
	 * 
	 * @param fileName name of the filter file
	 * @throws IOException
	 * @throws SAXException 
	 * @throws FilterException
	 */
	private void parse(String fileName) throws IOException, SAXException {

		
		if (true) {
			File file = new File(fileName);
			SAXBugCollectionHandler handler = new SAXBugCollectionHandler(this,file);
			XMLReader xr = XMLReaderFactory.createXMLReader();
			xr.setContentHandler(handler);
			xr.setErrorHandler(handler);
			FileInputStream fileInputStream = new FileInputStream(file);
			try {
			Reader reader = Util.getReader(fileInputStream);
			xr.parse(new InputSource(reader));
			} finally {
				Util.closeSilently(fileInputStream);
			}
			return;
			
		}
		Document filterDoc = null;

		FileInputStream fileInputStream = new FileInputStream(fileName);
		
		try {
			SAXReader reader = new SAXReader();
			filterDoc = reader.read(new BufferedInputStream(fileInputStream));
		} catch (DocumentException e) {
			throw new FilterException("Couldn't parse filter file " + fileName, e);
		}

		int count = 1;
		// Iterate over Match elements
		for (Object matchObj : filterDoc.selectNodes("/FindBugsFilter/Match")) {
			Element matchNode = (Element) matchObj;
			AndMatcher matchMatcher = new AndMatcher();

			// Each match node may have either "class" or "classregex" attributes
			Matcher classMatcher = null;
			String classAttr = matchNode.valueOf("@class");
			if (!classAttr.equals("")) {
				classMatcher = new ClassMatcher(classAttr);
			} else {
				String classRegex = matchNode.valueOf("@classregex");
				if (!classRegex.equals(""))
					classMatcher = new ClassMatcher("~" + classRegex);
			}
			if (classMatcher != null)
				matchMatcher.addChild(classMatcher);

			if (DEBUG) System.out.println("Match node");

			// Iterate over child elements of Match node.
			Iterator<Element> j = matchNode.elementIterator();
			while (j.hasNext()) {
				Element child = j.next();
				Matcher matcher = getMatcher(child);
				matchMatcher.addChild(matcher);
			}
			if (matchMatcher.numberChildren() == 0)
				throw new FilterException("Match element #" + count + " (starting at 1) is invalid in filter file " + fileName);
			// Add the Match matcher to the overall Filter
			this.addChild(matchMatcher);
			count++;
		}
		if (this.numberChildren() == 0)
		   throw new  FilterException("Could not find any /FindBugsFilter/Match nodes in filter file " + fileName);

	}

	/**
	 * Get a Matcher for given Element.
	 * 
	 * @param element the Element
	 * @return a Matcher representing that element
	 * @throws FilterException
	 */
	private static Matcher getMatcher(Element element) throws FilterException {
		// These will be either BugCode, Priority, Class, Method, Field, or Or elements.
		String name = element.getName();
		if (name.equals("BugCode")) {
			return new BugMatcher(element.valueOf("@name"), "", "");
		} else  if (name.equals("Local")) {
				return new LocalMatcher(element.valueOf("@name"));
		} else if (name.equals("BugPattern")) {
			return new BugMatcher("", element.valueOf("@name"), "");
		} else if (name.equals("Bug")) {
			return new BugMatcher(element.valueOf("@code"), element
					.valueOf("@pattern"), element.valueOf("@category"));
		} else if (name.equals("Priority")) {
			return new PriorityMatcher(element.valueOf("@value"));
		} else if (name.equals("Class")) {
			Attribute nameAttr = element.attribute("name");

			if (nameAttr == null)
				throw new FilterException("Missing name attribute in Class element");

			return new ClassMatcher(nameAttr.getValue());
		} else if (name.equals("Package")) {
			Attribute nameAttr = element.attribute("name");

			if (nameAttr == null)
				throw new FilterException("Missing name attribute in Package element");

			String pName = nameAttr.getValue();
			pName = pName.startsWith("~") ? pName : "~" + Strings.replace(pName, ".", "\\.");			
			return new ClassMatcher(pName + "\\.[^.]+");
		} else if (name.equals("Method")) {
			Attribute nameAttr = element.attribute("name");
			String nameValue;
			Attribute paramsAttr = element.attribute("params");
			Attribute returnsAttr = element.attribute("returns");

			if (nameAttr == null)
				if(paramsAttr == null || returnsAttr == null)
					throw new FilterException("Method element must have eiter name or params and returnss attributes");
				else
					nameValue = "~.*"; // any name
			else
				nameValue = nameAttr.getValue();

			if ((paramsAttr != null || returnsAttr != null) && (paramsAttr == null || returnsAttr == null))
				throw new FilterException("Method element must have both params and returns attributes if either is used");

			if (paramsAttr == null)
				return new MethodMatcher(nameValue);
			else
				return new MethodMatcher(nameValue, paramsAttr.getValue(), returnsAttr.getValue());
		} else if (name.equals("Field")) {
			Attribute nameAttr = element.attribute("name");
			String nameValue;
			Attribute typeAttr = element.attribute("type");

			if (nameAttr == null)
				if(typeAttr == null)
					throw new FilterException("Field element must have either name or type attribute");
				else
					nameValue = "~.*"; // any name
			else
				nameValue = nameAttr.getValue();

			if (typeAttr == null)
				return new FieldMatcher(nameValue);
			else
				return new FieldMatcher(nameValue, typeAttr.getValue());
		} else if (name.equals("Or")) {
			OrMatcher orMatcher = new OrMatcher();
			Iterator<Element> i = element.elementIterator();
			while (i.hasNext()) {
				orMatcher.addChild(getMatcher(i.next()));
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

			Filter filter = new Filter(argv[0]);
			filter.writeAsXML(System.out);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void writeAsXML(OutputStream out) throws IOException{

			XMLOutput xmlOutput = new OutputStreamXMLOutput(out);
			
			xmlOutput.beginDocument();
			xmlOutput.openTag("FindBugsFilter");
			writeBodyAsXML(xmlOutput);
			xmlOutput.closeTag("FindBugsFilter");
			xmlOutput.finish();
		}
	public void writeEnabledMatchersAsXML(OutputStream out) throws IOException{

		XMLOutput xmlOutput = new OutputStreamXMLOutput(out);
		
		xmlOutput.beginDocument();
		xmlOutput.openTag("FindBugsFilter");
		 Iterator<Matcher> i = childIterator();
		    while (i.hasNext()) {
		    	Matcher child = (Matcher) i.next();
		    	if (!disabled.containsKey(child)) child.writeXML(xmlOutput, false);
		    }
		xmlOutput.closeTag("FindBugsFilter");
		xmlOutput.finish();
	}


	public void writeBodyAsXML(XMLOutput xmlOutput) throws IOException {
	    Iterator<Matcher> i = childIterator();
	    while (i.hasNext()) {
	    	Matcher child = (Matcher) i.next();
	    	child.writeXML(xmlOutput, disabled.containsKey(child));
	    }
    }
		
}

// vim:ts=4
