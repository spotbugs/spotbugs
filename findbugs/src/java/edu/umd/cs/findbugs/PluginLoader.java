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

import java.util.*;
import java.net.*;
import java.io.*;
import org.dom4j.*;
import org.dom4j.io.*;

/**
 * Loader for a FindBugs plugin.
 * A plugin is a jar file containing two metadata files,
 * "findbugs.xml" and "messages.xml".  Those files specify
 * <ul>
 * <li> the bug pattern detector classes,
 * <li> the bug patterns detected (including all text for displaying
 *    detected instances of those patterns), and
 * <li> the "bug codes" which group together related bug instances
 * </ul>
 *
 * <p> The PluginLoader creates instances of DetectorFactory, BugPattern, and BugCode,
 * and provides methods for accessing those instances.
 *
 * @see DetectorFactory
 * @see BugPattern
 * @see BugCode
 * @see PluginException
 * @author David Hovemeyer
 */
public class PluginLoader extends URLClassLoader {

	private ArrayList<DetectorFactory> detectorFactoryList;
	private ArrayList<BugPattern> bugPatternList;
	private ArrayList<BugCode> bugCodeList;

	/**
	 * Constructor.
	 * @param url the URL of the plugin Jar file
	 * @throws PluginException if the plugin cannot be fully loaded
	 */
	public PluginLoader(URL url) throws PluginException {
		super(new URL[]{url});
		init();
	}
	
	/**
	 * Constructor.
	 * @param url the URL of the plugin Jar file
	 * @param parent the parent classloader
	 * @throws PluginException if the plugin cannot be fully loaded
	 */
	public PluginLoader(URL url, ClassLoader parent) throws PluginException {
		super(new URL[]{url}, parent);
		init();
	}

	/**
	 * Get the DetectorFactory array containing factories for creating all
	 * of the detectors in the plugin.
	 */
	public DetectorFactory[] getDetectorFactoryList() {
		return detectorFactoryList.toArray(new DetectorFactory[detectorFactoryList.size()]);
	}

	/**
	 * Get array of BugPattern objects for bug patterns reported by
	 * the plugin.
	 */
	public BugPattern[] getBugPatternList() {
		return bugPatternList.toArray(new BugPattern[bugPatternList.size()]);
	}

	/**
	 * Get array of BugCode objects for bug codes reported by this plugin.
	 */
	public BugCode[] getBugCodeList() {
		return bugCodeList.toArray(new BugCode[bugCodeList.size()]);
	}

	private void init() throws PluginException {
		// Plugin descriptor (a.k.a, "findbugs.xml").  Defines
		// the bug detectors and bug patterns that the plugin provides.
		Document pluginDescriptor;

		// List of message translation files in decreasing order of precedence
		ArrayList<Document> messageCollectionList = new ArrayList<Document>();

		try {
			URL descriptorURL = findResource("findbugs.xml");
			if (descriptorURL == null)
				throw new PluginException("Couldn't find \"findbugs.xml\" in plugin");

			SAXReader reader = new SAXReader();
			pluginDescriptor = reader.read(descriptorURL);
		} catch (DocumentException e) {
			throw new PluginException("Couldn't parse \"findbugs.xml\"", e);
		}

		try {
			Locale locale = Locale.getDefault();
			String language = locale.getLanguage();
			String country = locale.getCountry();

			if (country != null)
				addCollection(messageCollectionList, "messages_" + language + "_" + country + ".xml");
			addCollection(messageCollectionList, "messages_" + language + ".xml");
			addCollection(messageCollectionList, "messages.xml");
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new PluginException("Couldn't parse \"messages.xml\"", e);
		}

		// Create a DetectorFactory for all Detector nodes
		HashMap<String, DetectorFactory> detectorFactoryMap = new HashMap<String, DetectorFactory>();
		try {
			detectorFactoryList = new ArrayList<DetectorFactory>();
			List detectorNodeList = pluginDescriptor.selectNodes("/FindbugsPlugin/Detector");
			for (Iterator i = detectorNodeList.iterator(); i.hasNext(); ) {
				Node detectorNode = (Node) i.next();
				String className = detectorNode.valueOf("@class");
				String speed = detectorNode.valueOf("@speed");
				String disabled = detectorNode.valueOf("@disabled");
				String reports = detectorNode.valueOf("@reports");
				String requireJRE = detectorNode.valueOf("@requirejre");
	
				//System.out.println("Found detector: class="+className+", disabled="+disabled);
	
				Class detectorClass = loadClass(className);
				DetectorFactory factory = new DetectorFactory(detectorClass, !disabled.equals("true"),
					speed, reports, requireJRE);
				detectorFactoryList.add(factory);
				detectorFactoryMap.put(className, factory);

				// Find Detector node in one of the messages files,
				// to get the detail HTML.
				Node node = findMessageNode(messageCollectionList,
					"/MessageCollection/Detector[@class='" + className + "']/Details",
					"Missing Detector description for detector " + className);

				Element details = (Element) node;
				String detailHTML = details.getText();
				StringBuffer buf = new StringBuffer();
				buf.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n");
				buf.append("<HTML><HEAD><TITLE>Detector Description</TITLE></HEAD><BODY>\n");
				buf.append(detailHTML);
				buf.append("</BODY></HTML>\n");
				factory.setDetailHTML(buf.toString());
			}
		} catch (ClassNotFoundException e) {
			throw new PluginException("Could not instantiate detector class: " + e, e);
		}

		// Create BugPatterns
		bugPatternList = new ArrayList<BugPattern>();
		List bugPatternNodeList = pluginDescriptor.selectNodes("/FindbugsPlugin/BugPattern");
		for (Iterator i = bugPatternNodeList.iterator(); i.hasNext(); ) {
			Node bugPatternNode = (Node) i.next();
			String type = bugPatternNode.valueOf("@type");
			String abbrev = bugPatternNode.valueOf("@abbrev");
			String category = bugPatternNode.valueOf("@category");
			String experimental = bugPatternNode.valueOf("@experimental");

			// Find the matching element in messages.xml (or translations)
			String query = "/MessageCollection/BugPattern[@type='" + type + "']";
			Node messageNode = findMessageNode(messageCollectionList, query,
				"messages.xml missing BugPattern element for type " + type);

			String shortDesc = getChildText(messageNode, "ShortDescription");
			String longDesc = getChildText(messageNode, "LongDescription");
			String detailText = getChildText(messageNode, "Details");

			BugPattern bugPattern = new BugPattern(type, abbrev, category,
				Boolean.valueOf(experimental).booleanValue(),
				shortDesc, longDesc, detailText);
			bugPatternList.add(bugPattern);
		}

		// Create BugCodes
		HashSet<String> definedBugCodes = new HashSet<String>();
		bugCodeList = new ArrayList<BugCode>();
		for (Iterator<Document> i = messageCollectionList.iterator(); i.hasNext(); ) {
			Document messageCollection = i.next();

			List bugCodeNodeList = messageCollection.selectNodes("/MessageCollection/BugCode");
			for (Iterator j = bugCodeNodeList.iterator(); j.hasNext(); ) {
				Node bugCodeNode = (Node) j.next();
				String abbrev = bugCodeNode.valueOf("@abbrev");
				if (abbrev.equals(""))
					throw new PluginException("BugCode element with missing abbrev attribute");
				if (definedBugCodes.contains(abbrev))
					continue;
				String description = bugCodeNode.getText();
				BugCode bugCode = new BugCode(abbrev, description);
				bugCodeList.add(bugCode);
				definedBugCodes.add(abbrev);
			}

		}

	}

	private void addCollection(List<Document> messageCollectionList, String filename)
		throws DocumentException {
		URL messageURL = findResource(filename);
		if (messageURL != null) {
			SAXReader reader = new SAXReader();
			Document messageCollection = reader.read(messageURL);
			messageCollectionList.add(messageCollection);
		}
	}

	private static Node findMessageNode(List<Document> messageCollectionList, String xpath,
		String missingMsg) throws PluginException {

		for (Iterator<Document> i = messageCollectionList.iterator(); i.hasNext(); ) {
			Document document = i.next();
			Node node = document.selectSingleNode(xpath);
			if (node != null)
				return node;
		}
		throw new PluginException(missingMsg);
	}

	private static String getChildText(Node node, String childName) throws PluginException {
		Node child = node.selectSingleNode(childName);
		if (child == null)
			throw new PluginException("Could not find child \"" + childName + "\" for node");
		return child.getText();
	}

}

// vim:ts=4
