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

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * The DetectorFactoryCollection stores all of the DetectorFactory objects
 * used to create the Detectors which implement the various analyses.
 * It is a singleton class.
 *
 * @author David Hovemeyer
 * @see DetectorFactory
 */
public class DetectorFactoryCollection {
	private HashMap<String, Plugin> pluginByIdMap = new HashMap<String, Plugin>();
	private ArrayList<DetectorFactory> factoryList = new ArrayList<DetectorFactory>();
	private HashMap<String, DetectorFactory> factoriesByName = new HashMap<String, DetectorFactory>();
	private HashMap<String, DetectorFactory> factoriesByDetectorClassName =
		new HashMap<String, DetectorFactory>();

	private static DetectorFactoryCollection theInstance;
	private static final Object lock = new Object();

	private static File[] pluginList;

	/**
	 * Constructor.
	 */
	private DetectorFactoryCollection() {
		loadPlugins();
	}

	/**
	 * Set the list of plugins to load explicitly.
	 * This must be done before the instance of DetectorFactoryCollection
	 * is created.
	 *
	 * @param pluginList list of plugin Jar files to load
	 */
	public static void setPluginList(File[] pluginList) {
		DetectorFactoryCollection.pluginList = new File[pluginList.length];
		System.arraycopy(pluginList, 0, DetectorFactoryCollection.pluginList, 0, pluginList.length);
	}

	/**
	 * Get the single instance of DetectorFactoryCollection.
	 */
	public static DetectorFactoryCollection instance() {
		synchronized (lock) {
			if (theInstance == null)
				theInstance = new DetectorFactoryCollection();
			return theInstance;
		}
	}

	/**
	 * Return an Iterator over all available Plugin objects.
	 */
	public Iterator<Plugin> pluginIterator() {
		return pluginByIdMap.values().iterator();
	}

	/**
	 * Get a Plugin by its unique id.
	 *
	 * @param pluginId the unique id
	 * @return the Plugin with that id, or null if no such Plugin is found
	 */
	public Plugin getPluginById(String pluginId) {
		return pluginByIdMap.get(pluginId);
	}

	/**
	 * Return an Iterator over the DetectorFactory objects for all
	 * registered Detectors.
	 */
	public Iterator<DetectorFactory> factoryIterator() {
		return factoryList.iterator();
	}

	/**
	 * Look up a DetectorFactory by its short name.
	 *
	 * @param name the short name
	 * @return the DetectorFactory, or null if there is no factory with that short name
	 */
	public DetectorFactory getFactory(String name) {
		return factoriesByName.get(name);
	}
	
	/**
	 * Look up a DetectorFactory by its class name.
	 * 
	 * @param className the class name
	 * @return the DetectoryFactory, or null if there is no factory with
	 *         that class name
	 */
	public DetectorFactory getFactoryByClassName(String className) {
		return factoriesByDetectorClassName.get(className);
	}

	/**
	 * Disable all detectors.
	 */
	public void disableAll() {
		enableAll(false);
	}

	/**
	 * Enable all detectors.
	 */
	public void enableAll() {
		enableAll(true);
	}

	private void enableAll(boolean enabled) {
		Iterator<DetectorFactory> i = DetectorFactoryCollection.instance().factoryIterator();
		while (i.hasNext()) {
			DetectorFactory factory = i.next();
			factory.setEnabled(enabled);
		}
	}

	/**
	 * Register a DetectorFactory.
	 */
	private void registerDetector(DetectorFactory factory) {
		if (FindBugs.DEBUG) System.out.println("Registering detector: " + factory.getFullName());
		String detectorName = factory.getShortName();
		factoryList.add(factory);
		factoriesByName.put(detectorName, factory);
		factoriesByDetectorClassName.put(factory.getFullName(), factory);
	}

	/**
	 * Load all plugins.
	 * If a setPluginList() has been called, then those plugins
	 * are loaded.  Otherwise, the "findbugs.home" property is checked
	 * to determine where FindBugs is installed, and the plugin files
	 * are dynamically loaded from the plugin directory.
	 */
	private void loadPlugins() {
		// Load all detector plugins.
	
		if (pluginList == null) {
			String homeDir = FindBugs.getHome();
			if (homeDir == null)
				return;

			File pluginDir = new File(homeDir + File.separator + "plugin");
			File[] contentList = pluginDir.listFiles();
			if (contentList == null) {
				System.err.println("Error: The path " + pluginDir.getPath() + " does not seem to be a directory!");
				System.err.println("No FindBugs plugins could be loaded");
				pluginList = new File[0];
				return;
			}

			ArrayList<File> arr = new ArrayList<File>();
			for (int i = 0; i < contentList.length; ++i) {
				if (contentList[i].getName().endsWith(".jar")) {
					if (FindBugs.DEBUG) System.out.println("Found plugin: " + contentList[i].toString());
					arr.add(contentList[i]);
				}
			}
			pluginList = (File[]) arr.toArray(new File[arr.size()]);
		}

		int numLoaded = 0;
		for (int i = 0; i < pluginList.length; ++i) {
			File file = pluginList[i];
			try {
				if (FindBugs.DEBUG) System.out.println("Loading plugin: " + file.toString());
				URL url = file.toURL();
				PluginLoader pluginLoader = new PluginLoader(url, this.getClass().getClassLoader());

				Plugin plugin = pluginLoader.getPlugin();
				pluginByIdMap.put(plugin.getPluginId(), plugin);
				
				// Register all of the detectors that this plugin contains
				for (Iterator<DetectorFactory> j = plugin.detectorFactoryIterator();
						j.hasNext(); ) {
					DetectorFactory factory = j.next();
					registerDetector(factory);
				}

				I18N i18n = I18N.instance();

				// Register the BugPatterns
				for (Iterator<BugPattern> j = plugin.bugPatternIterator(); j.hasNext();){ 
					BugPattern bugPattern = j.next();
					i18n.registerBugPattern(bugPattern);
				}
				
				// Register the BugCodes
				for (Iterator<BugCode> j = plugin.bugCodeIterator(); j.hasNext(); ) {
					BugCode bugCode = j.next();
					i18n.registerBugCode(bugCode);
				}

				++numLoaded;
			} catch (Exception e) {
				System.err.println("Warning: could not load plugin " + file.getPath() + ": " + e.toString());
				if (FindBugs.DEBUG)
					e.printStackTrace();
			}
		}
	
		//System.out.println("Loaded " + numLoaded + " plugins");
	}

}

// vim:ts=4
