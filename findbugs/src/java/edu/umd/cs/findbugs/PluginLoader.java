/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import edu.umd.cs.findbugs.plan.ByInterfaceDetectorFactorySelector;
import edu.umd.cs.findbugs.plan.DetectorFactorySelector;
import edu.umd.cs.findbugs.plan.DetectorOrderingConstraint;
import edu.umd.cs.findbugs.plan.ReportingDetectorFactorySelector;
import edu.umd.cs.findbugs.plan.SingleDetectorFactorySelector;

/**
 * Loader for a FindBugs plugin.
 * A plugin is a jar file containing two metadata files,
 * "findbugs.xml" and "messages.xml".  Those files specify
 * <ul>
 * <li> the bug pattern Detector classes,
 * <li> the bug patterns detected (including all text for displaying
 * detected instances of those patterns), and
 * <li> the "bug codes" which group together related bug instances
 * </ul>
 *
 * <p> The PluginLoader creates a Plugin object to store
 * the Detector factories and metadata.</p>
 *
 * @author David Hovemeyer
 * @see Plugin
 * @see PluginException
 */
public class PluginLoader extends URLClassLoader {

	private static final boolean DEBUG = SystemProperties.getBoolean("findbugs.debug.PluginLoader");

	// Keep a count of how many plugins we've seen without a
	// "pluginid" attribute, so we can assign them all unique ids.
	private static int nextUnknownId;

	// The loaded Plugin
	private Plugin plugin;

	/**
	 * Constructor.
	 *
	 * @param url the URL of the plugin Jar file
	 * @throws PluginException if the plugin cannot be fully loaded
	 */
	public PluginLoader(URL url) throws PluginException {
		super(new URL[]{url});
		init();
	}

	/**
	 * Constructor.
	 *
	 * @param url    the URL of the plugin Jar file
	 * @param parent the parent classloader
	 */
	public PluginLoader(URL url, ClassLoader parent) throws PluginException {
		super(new URL[]{url}, parent);
		init();
	}

	/**
	 * Get the Plugin.
	 * @throws PluginException if the plugin cannot be fully loaded
	 */
	public Plugin getPlugin() throws PluginException {
		if (plugin == null)
			init();
		return plugin;
	}

	private void init() throws PluginException {
		// Plugin descriptor (a.k.a, "findbugs.xml").  Defines
		// the bug detectors and bug patterns that the plugin provides.
		Document pluginDescriptor;

		// Unique plugin id
		String pluginId;

		// List of message translation files in decreasing order of precedence
		ArrayList<Document> messageCollectionList = new ArrayList<Document>();

		// Read the plugin descriptor
		try {
			URL descriptorURL = findResource("findbugs.xml");
			if (descriptorURL == null)
				throw new PluginException("Couldn't find \"findbugs.xml\" in plugin");

			SAXReader reader = new SAXReader();
			pluginDescriptor = reader.read(descriptorURL);
		} catch (DocumentException e) {
			throw new PluginException("Couldn't parse \"findbugs.xml\"", e);
		}

		// Get the unique plugin id (or generate one, if none is present)
		pluginId = pluginDescriptor.valueOf("/FindbugsPlugin/@pluginid");
		if (pluginId.equals("")) {
			synchronized (PluginLoader.class) {
				pluginId = "plugin" + nextUnknownId++;
			}
		}

		// See if the plugin is enabled or disabled by default.
		// Note that if there is no "defaultenabled" attribute,
		// then we assume that the plugin IS enabled by default.
		String defaultEnabled = pluginDescriptor.valueOf("/FindbugsPlugin/@defaultenabled");
		boolean pluginEnabled = defaultEnabled.equals("") || Boolean.valueOf(defaultEnabled).booleanValue();

		// Load the message collections
		try {
			//Locale locale = Locale.getDefault();
			Locale locale = I18N.defaultLocale;
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

		// Create the Plugin object (but don't assign to the plugin field yet,
		// since we're still not sure if everything will load correctly)
		Plugin plugin = new Plugin(pluginId);
		plugin.setEnabled(pluginEnabled);

		// Set provider and website, if specified
		String provider = pluginDescriptor.valueOf("/FindbugsPlugin/@provider");
		if (!provider.equals(""))
			plugin.setProvider(provider);
		String website = pluginDescriptor.valueOf("/FindbugsPlugin/@website");
		if (!website.equals(""))
			plugin.setWebsite(website);

		// Set short description, if specified
		Node pluginShortDesc = null;
		try {
			pluginShortDesc = findMessageNode(
					messageCollectionList,
					"/MessageCollection/Plugin/ShortDescription",
					"no plugin description");
		} catch (PluginException e) {
			// Missing description is not fatal, so ignore
		}
		if (pluginShortDesc != null) {
			plugin.setShortDescription(pluginShortDesc.getText());
		}

		// Create a DetectorFactory for all Detector nodes
		try {
			List<Node> detectorNodeList = pluginDescriptor.selectNodes("/FindbugsPlugin/Detector");
			int detectorCount = 0;
			for (Node detectorNode : detectorNodeList) {
				String className = detectorNode.valueOf("@class");
				String speed = detectorNode.valueOf("@speed");
				String disabled = detectorNode.valueOf("@disabled");
				String reports = detectorNode.valueOf("@reports");
				String requireJRE = detectorNode.valueOf("@requirejre");
				String hidden = detectorNode.valueOf("@hidden");

				//System.out.println("Found detector: class="+className+", disabled="+disabled);

				// Create DetectorFactory for the detector
				Class<?> detectorClass = loadClass(className);
				if (!Detector.class.isAssignableFrom(detectorClass)
						&& !Detector2.class.isAssignableFrom(detectorClass))
					throw new PluginException("Class " + className + " does not implement Detector or Detector2");
				DetectorFactory factory = new DetectorFactory(
						plugin,
						detectorClass, !disabled.equals("true"),
						speed, reports, requireJRE);
				if (Boolean.valueOf(hidden).booleanValue())
					factory.setHidden(true);
				factory.setPositionSpecifiedInPluginDescriptor(detectorCount++);
				plugin.addDetectorFactory(factory);

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

		// Create ordering constraints
		Node orderingConstraintsNode =
			pluginDescriptor.selectSingleNode("/FindbugsPlugin/OrderingConstraints");
		if (orderingConstraintsNode != null) {
			// Get inter-pass and intra-pass constraints
			for (Element constraintElement : (List<Element>) orderingConstraintsNode.selectNodes("./SplitPass|./WithinPass"))
			{
				// Create the selectors which determine which detectors are
				// involved in the constraint
				DetectorFactorySelector earlierSelector = getConstraintSelector(
						constraintElement, plugin, "Earlier", "EarlierCategory");
				DetectorFactorySelector laterSelector = getConstraintSelector(
						constraintElement, plugin, "Later", "LaterCategory");

				// Create the constraint
				DetectorOrderingConstraint constraint = new DetectorOrderingConstraint(
						earlierSelector, laterSelector);

				// Add the constraint to the plugin
				if (constraintElement.getName().equals("SplitPass"))
					plugin.addInterPassOrderingConstraint(constraint);
				else
					plugin.addIntraPassOrderingConstraint(constraint);
			}
		}

		// register global Category descriptions
		I18N i18n = I18N.instance();
		for (Document messageCollection : messageCollectionList) {
			List<Node> categoryNodeList = messageCollection.selectNodes("/MessageCollection/BugCategory");
			if (DEBUG) System.out.println("found "+categoryNodeList.size()+" categories in "+messageCollection.getName());
			for (Node categoryNode : categoryNodeList) {
				String key = categoryNode.valueOf("@category");
				if (key.equals(""))
					throw new PluginException("BugCategory element with missing category attribute");
				String shortDesc = getChildText(categoryNode, "Description");
				BugCategory bc = new BugCategory(key, shortDesc);
				boolean b = i18n.registerBugCategory(key, bc);
				if (DEBUG) System.out.println(b
					? "category "+key+" -> "+shortDesc
					: "rejected \""+shortDesc+"\" for category "+key+": "+i18n.getBugCategoryDescription(key));
				/* Now set the abbreviation and details. Be prepared for messages_fr.xml
				 * to specify only the shortDesc (though it should set the abbreviation
				 * too) and fall back to messages.xml for the abbreviation and details. */
				if (!b) bc = i18n.getBugCategory(key); // get existing BugCategory object
				try {
					String abbrev = getChildText(categoryNode, "Abbreviation");
					if (bc.getAbbrev() == null) {
						bc.setAbbrev(abbrev);
						if (DEBUG) System.out.println("category "+key+" abbrev -> "+abbrev);
					}
					else if (DEBUG) System.out.println("rejected abbrev '"+abbrev+"' for category "+key+": "+bc.getAbbrev());
				} catch (PluginException pe) {
					if (DEBUG) System.out.println("missing Abbreviation for category "+key+"/"+shortDesc);
					// do nothing else -- Abbreviation is required, but handle its omission gracefully
				}
				try {
					String details = getChildText(categoryNode, "Details");
					if (bc.getDetailText() == null) {
						bc.setDetailText(details);
						if (DEBUG) System.out.println("category "+key+" details -> "+details);
					}
					else if (DEBUG) System.out.println("rejected details ["+details+"] for category "+key+": ["+bc.getDetailText()+']');
				} catch (PluginException pe) {
					// do nothing -- LongDescription is optional
				}
			}
		}

		// Create BugPatterns
		List<Node> bugPatternNodeList = pluginDescriptor.selectNodes("/FindbugsPlugin/BugPattern");
		for (Node bugPatternNode : bugPatternNodeList) {
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
			plugin.addBugPattern(bugPattern);
			boolean unknownCategory = (null == i18n.getBugCategory(category));
			if (unknownCategory) {
				i18n.registerBugCategory(category, new BugCategory(category, category));
				// no desc, but at least now it will appear in I18N.getBugCategories().
				if (DEBUG) System.out.println("Category "+category+" (of BugPattern "
					+type+") has no description in messages*.xml");
				//TODO report this even if !DEBUG
			}
		}

		// Create BugCodes
		Set<String> definedBugCodes = new HashSet<String>();
		for (Document messageCollection : messageCollectionList) {
			List<Node> bugCodeNodeList = messageCollection.selectNodes("/MessageCollection/BugCode");
			for (Node bugCodeNode : bugCodeNodeList) {
				String abbrev = bugCodeNode.valueOf("@abbrev");
				if (abbrev.equals(""))
					throw new PluginException("BugCode element with missing abbrev attribute");
				if (definedBugCodes.contains(abbrev))
					continue;
				String description = bugCodeNode.getText();
				BugCode bugCode = new BugCode(abbrev, description);
				plugin.addBugCode(bugCode);
				definedBugCodes.add(abbrev);
			}

		}

		// Success!
		// Assign to the plugin field, so getPlugin() can return the
		// new Plugin object.
		this.plugin = plugin;

	}

	private static DetectorFactorySelector getConstraintSelector(
			Element constraintElement,
			Plugin plugin,
			String singleDetectorElementName,
			String detectorCategoryElementName) throws PluginException {
		Node node = constraintElement.selectSingleNode("./" + singleDetectorElementName);
		if (node != null) {
			String detectorClass = node.valueOf("@class");
			return new SingleDetectorFactorySelector(plugin, detectorClass);
		}

		node = constraintElement.selectSingleNode("./" + detectorCategoryElementName);
		if (node != null) {
			String categoryName = node.valueOf("@name");
			boolean spanPlugins = Boolean.valueOf(node.valueOf("@spanplugins")).booleanValue();
			if (categoryName.equals("reporting")) {
				return new ReportingDetectorFactorySelector(spanPlugins ? null : plugin);
			} else if (categoryName.equals("training")) {
				return new ByInterfaceDetectorFactorySelector(spanPlugins ? null : plugin, TrainingDetector.class);
			} else if (categoryName.equals("interprocedural")) {
				return new ByInterfaceDetectorFactorySelector(spanPlugins ? null : plugin, InterproceduralFirstPassDetector.class);
			} else {
				throw new PluginException("Invalid constraint selector node");
			}
		}

		throw new PluginException("Invalid constraint selector node");
	}

	private String lookupDetectorClass(Plugin plugin, String name) throws PluginException {
		// If the detector name contains '.' characters, assume it is
		// fully qualified already.  Otherwise, assume it is a short
		// name that resolves to another detector in the same plugin.

		if (name.indexOf('.') < 0) {
			DetectorFactory factory = plugin.getFactoryByShortName(name);
			if (factory == null)
				throw new PluginException("No detector found for short name '" + name + "'");
			name = factory.getFullName();
		}
		return name;
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

		for (Document document : messageCollectionList) {
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
