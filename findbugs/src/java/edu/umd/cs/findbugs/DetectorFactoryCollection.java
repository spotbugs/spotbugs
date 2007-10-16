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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
	private boolean loaded = false;

	private URL[] pluginList;

	/**
	 * Constructor.
	 * loadPlugins() method must be called before
	 * any detector factories can be accessed.
	 */
	DetectorFactoryCollection() {
	}

	/**
	 * Set the list of plugins to load explicitly.
	 * This must be done before the instance of DetectorFactoryCollection
	 * is created.
	 *
	 * @param pluginList list of plugin Jar files to load
	 */
	public void setPluginList(URL[] pluginList) {
		if (loaded) throw new IllegalStateException();
		this.pluginList = new URL[pluginList.length];
		System.arraycopy(pluginList, 0, this.pluginList, 0, pluginList.length);
	}

	/**
	 * Set the instance that should be retured as the singleton instance.
	 * 
	 * @param instance the singleton instance to be set
	 */
	static void setInstance(DetectorFactoryCollection instance) {
		synchronized (lock) {
			if (theInstance != null) {
				throw new IllegalStateException();
			}
			theInstance = instance;
		}
	}
	
	static void resetInstance(DetectorFactoryCollection instance) {
		synchronized (lock) {
			theInstance = instance;
		}
	}

	/**
	 * Get the single instance of DetectorFactoryCollection.
	 */
	public static DetectorFactoryCollection instance() {
		synchronized (lock) {
			if (theInstance == null) {
				theInstance = new DetectorFactoryCollection();
			}
			theInstance.ensureLoaded();
			return theInstance;
		}
	}
	/**
	 * Get the single instance of DetectorFactoryCollection.
	 */
	public static DetectorFactoryCollection rawInstance() {
		synchronized (lock) {
			if (theInstance == null) {
				theInstance = new DetectorFactoryCollection();
			}
			return theInstance;
		}
	}
	/**
	 * Return an Iterator over all available Plugin objects.
	 */
	public Iterator<Plugin> pluginIterator() {
		ensureLoaded();
		return pluginByIdMap.values().iterator();
	}


	/**
	 * Get a Plugin by its unique id.
	 *
	 * @param pluginId the unique id
	 * @return the Plugin with that id, or null if no such Plugin is found
	 */
	public Plugin getPluginById(String pluginId) {
		ensureLoaded();
		return pluginByIdMap.get(pluginId);
	}

	/**
	 * Return an Iterator over the DetectorFactory objects for all
	 * registered Detectors.
	 */
	public Iterator<DetectorFactory> factoryIterator() {
		ensureLoaded();
		return factoryList.iterator();
	}

	/**
	 * Look up a DetectorFactory by its short name.
	 *
	 * @param name the short name
	 * @return the DetectorFactory, or null if there is no factory with that short name
	 */
	public DetectorFactory getFactory(String name) {
		ensureLoaded();
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
		ensureLoaded();
		return factoriesByDetectorClassName.get(className);
	}

	/**
	 * Register a DetectorFactory.
	 */
	void registerDetector(DetectorFactory factory) {
		if (FindBugs.DEBUG) System.out.println("Registering detector: " + factory.getFullName());
		String detectorName = factory.getShortName();
		factoryList.add(factory);
		factoriesByName.put(detectorName, factory);
		factoriesByDetectorClassName.put(factory.getFullName(), factory);
	}

	private void determinePlugins() {
		if (pluginList != null)
			return;
		String homeDir = FindBugs.getHome();
		if (homeDir == null) {
			System.err.println("Error: FindBugs home directory is not set");
			return;
		}

		File pluginDir = new File(homeDir + File.separator + "plugin");
		File[] contentList = pluginDir.listFiles();
		if (contentList == null) {
			System.err.println("Error: The path " + pluginDir.getPath()
					+ " does not seem to be a directory!");
			System.err.println("No FindBugs plugins could be loaded");
			pluginList = new URL[0];
			return;
		}

		ArrayList<URL> arr = new ArrayList<URL>();
		for (File aContentList : contentList) {
			if (aContentList.getName().endsWith(".jar")) {

				try {
					arr.add(aContentList.toURL());
					if (FindBugs.DEBUG)
						System.out.println("Found plugin: " + aContentList.toString());
				} catch (MalformedURLException e) {

				}

			}
		}
		pluginList = arr.toArray(new URL[arr.size()]);

	}

	public void ensureLoaded() {
		if (loaded) return;
		loadPlugins();
	}
	
	/**
	 * Directly set the collection of Plugins from which to load DetectorFactories.
	 * May be called instead of loadPlugins().
	 * 
	 * @param plugins array of Plugins to register
	 */
	void setPlugins(Plugin[] plugins) {
		if (loaded) {
			throw new IllegalStateException();
		}
		for (Plugin plugin : plugins) {
			pluginByIdMap.put(plugin.getPluginId(), plugin);
		}
		loaded = true;
	}
	
	/**
	 * Load all plugins. If a setPluginList() has been called, then those
	 * plugins are loaded. Otherwise, the "findbugs.home" property is checked to
	 * determine where FindBugs is installed, and the plugin files are
	 * dynamically loaded from the plugin directory.
	 */
	void loadPlugins() {
		if (loaded) throw new IllegalStateException();

		//If we are running under jaws, just use the loaded plugin
		if (SystemProperties.getBoolean("findbugs.jaws")) {
			URL u = DetectorFactoryCollection.class.getResource("/findbugs.xml");
			// JOptionPane.showMessageDialog(null, "Loading plugin from " + u);
			URL[] plugins = new URL[1];
			if (u != null) {
				String path = u.toString();
				path = path.substring(0, path.length() - "findbugs.xml".length());
				if (FindBugs.DEBUG) System.out.println("Jaws uses plugin: " + path);
				try {
					plugins[0] = new URL(path);

				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}
				setPluginList(plugins);

			}
		}

		// Load all detector plugins.
		loaded = true;
		determinePlugins();

		int numLoaded = 0;
		for (final URL url : pluginList) {
			try {
				if (FindBugs.DEBUG) System.out.println("Loading plugin: " + url.toString());
				PluginLoader pluginLoader =
					AccessController.doPrivileged(new PrivilegedExceptionAction<PluginLoader>() {

						public PluginLoader run() throws PluginException {
							return	new PluginLoader(url, this.getClass().getClassLoader());
						}

					});


				Plugin plugin = pluginLoader.getPlugin();
				pluginByIdMap.put(plugin.getPluginId(), plugin);

				// Register all of the detectors that this plugin contains
				for (Iterator<DetectorFactory> j = plugin.detectorFactoryIterator();
					 j.hasNext();) {
					DetectorFactory factory = j.next();
					registerDetector(factory);
				}

				I18N i18n = I18N.instance();

				// Register the BugPatterns
				for (Iterator<BugPattern> j = plugin.bugPatternIterator(); j.hasNext();) {
					BugPattern bugPattern = j.next();
					i18n.registerBugPattern(bugPattern);
				}

				// Register the BugCodes
				for (Iterator<BugCode> j = plugin.bugCodeIterator(); j.hasNext();) {
					BugCode bugCode = j.next();
					i18n.registerBugCode(bugCode);
				}

				++numLoaded;
			} catch (PluginException e) {
				System.err.println("Warning: could not load plugin " + url + ": " + e.toString());
				if (FindBugs.DEBUG)
					e.printStackTrace();
			} catch (PrivilegedActionException e) {
				System.err.println("Warning: could not load plugin " + url + ": " + e.toString());
				if (FindBugs.DEBUG)
					e.printStackTrace();
			}
		}


		//System.out.println("Loaded " + numLoaded + " plugins");
	}
}

// vim:ts=4
