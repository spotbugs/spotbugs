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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.util.ClassPathUtil;

/**
 * The DetectorFactoryCollection stores all of the DetectorFactory objects used
 * to create the Detectors which implement the various analyses. It is a
 * singleton class.
 *
 * @author David Hovemeyer
 * @see DetectorFactory
 */
public class DetectorFactoryCollection {

    private static final Logger LOGGER = Logger.getLogger(DetectorFactoryCollection.class.getName());

    private final HashMap<String, Plugin> pluginByIdMap = new LinkedHashMap<String, Plugin>();

    private static Plugin corePlugin;

    private final ArrayList<DetectorFactory> factoryList = new ArrayList<DetectorFactory>();

    private final HashMap<String, DetectorFactory> factoriesByName = new HashMap<String, DetectorFactory>();

    private final HashMap<String, DetectorFactory> factoriesByDetectorClassName = new HashMap<String, DetectorFactory>();

    private static DetectorFactoryCollection theInstance;

    private static final Object lock = new Object();

    private final boolean loaded = false;

    static final boolean DEBUG = Boolean.getBoolean("dfc.debug");

    Map<String, CloudPlugin> registeredClouds = new LinkedHashMap<String, CloudPlugin>();

    protected final HashMap<String, BugCategory> categoryDescriptionMap = new HashMap<String, BugCategory>();

    protected final HashMap<String, BugPattern> bugPatternMap = new HashMap<String, BugPattern>();

    protected final HashMap<String, BugCode> bugCodeMap = new HashMap<String, BugCode>();

    protected DetectorFactoryCollection() {
        loadCorePlugin();
        for(Plugin plugin : Plugin.getAllPlugins()) {
            if (plugin.isGloballyEnabled() && !plugin.isCorePlugin())
                loadPlugin(plugin);
        }
    }

    protected DetectorFactoryCollection(Plugin onlyPlugin) {
        loadPlugin(onlyPlugin);
    }

    protected DetectorFactoryCollection(Collection<Plugin> enabled) {
        loadCorePlugin();
        for(Plugin plugin : enabled) {
            loadPlugin(plugin);
        }
    }

    /**
     * Set the instance that should be returned as the singleton instance.
     *
     * @param instance
     *            the singleton instance to be set
     */
    static void setInstance(DetectorFactoryCollection instance) {
        synchronized (lock) {
            if (theInstance != null) {
                throw new IllegalStateException();
            }
            if (!(instance instanceof I18N))
                throw new IllegalArgumentException();

            theInstance = instance;
        }
    }

    /**
     * Reset the factory singleton.
     * <p>
     * <b>Implementation note:</b> This method is public only to allow Eclipse
     * plugin install and uninstall additional bug detector packages without
     * restarting the JVM
     *
     * @param instance
     *            can be null
     */
    public static void resetInstance(DetectorFactoryCollection instance) {
        synchronized (lock) {
            if (!(instance instanceof I18N))
                throw new IllegalArgumentException();
            theInstance = instance;
        }
    }
    public static void resetInstance() {
        synchronized (lock) {
            theInstance = new I18N();
        }
    }
    public static boolean isLoaded() {
        synchronized (lock) {
            return theInstance != null && (theInstance).loaded;
        }
    }

    /**
     * Get the single instance of DetectorFactoryCollection.
     */
    public static DetectorFactoryCollection instance() {
        synchronized (lock) {
            if (theInstance == null) {
                theInstance = I18N.instance();
            }
            return theInstance;
        }
    }

    /**
     * Get the single instance of DetectorFactoryCollection.
     */
    public static DetectorFactoryCollection rawInstance() {
        synchronized (lock) {
            if (theInstance == null) {
                theInstance = I18N.instance();
            }
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
     * Return an Iterable of all available Plugin objects.
     */
    public Iterable<Plugin> plugins() {
        return pluginByIdMap.values();
    }

    @Nonnull
    Plugin getCorePlugin() {
        if (corePlugin == null)
            throw new IllegalStateException("No core plugin");
        return corePlugin;
    }


    /**
     * Get a Plugin by its unique id.
     *
     * @param pluginId
     *            the unique id
     * @return the Plugin with that id, or null if no such Plugin is found
     */
    @Deprecated
    public Plugin getPluginById(String pluginId) {
        return pluginByIdMap.get(pluginId);
    }

    /**
     * Return an Iterator over the DetectorFactory objects for all registered
     * Detectors.
     */
    public Iterator<DetectorFactory> factoryIterator() {
        return factoryList.iterator();
    }

    /**
     * Return an Iterable over the DetectorFactory objects for all registered
     * Detectors.
     */
    public Iterable<DetectorFactory> getFactories() {
        return factoryList;
    }

    /**
     * Look up a DetectorFactory by its short name.
     *
     * @param name
     *            the short name
     * @return the DetectorFactory, or null if there is no factory with that
     *         short name
     */
    public DetectorFactory getFactory(String name) {
        return factoriesByName.get(name);
    }

    /**
     * Look up a DetectorFactory by its class name.
     *
     * @param className
     *            the class name
     * @return the DetectoryFactory, or null if there is no factory with that
     *         class name
     */
    public DetectorFactory getFactoryByClassName(String className) {
        return factoriesByDetectorClassName.get(className);
    }

    /**
     * Register a DetectorFactory.
     */
    void registerDetector(DetectorFactory factory) {
        if (FindBugs.DEBUG)
            System.out.println("Registering detector: " + factory.getFullName());
        String detectorName = factory.getShortName();
        factoryList.add(factory);
        factoriesByName.put(detectorName, factory);
        factoriesByDetectorClassName.put(factory.getFullName(), factory);
    }

    private static final Pattern[] findbugsJarNames = { Pattern.compile("findbugs\\.jar$"), };

    /**
     * See if the location of ${findbugs.home} can be inferred from the location
     * of findbugs.jar in the classpath.
     *
     * @return inferred ${findbugs.home}, or null if we can't figure it out
     */
    private static String inferFindBugsHome() {
        for (Pattern jarNamePattern : findbugsJarNames) {
            String findbugsJarCodeBase = ClassPathUtil.findCodeBaseInClassPath(jarNamePattern,
                    SystemProperties.getProperty("java.class.path"));
            if (findbugsJarCodeBase != null) {
                File findbugsJar = new File(findbugsJarCodeBase);
                File libDir = findbugsJar.getParentFile();
                if (libDir.getName().equals("lib")) {
                    String fbHome = libDir.getParent();
                    FindBugs.setHome(fbHome);
                    return fbHome;
                }
            }
        }
        String classFilePath = FindBugs.class.getName().replaceAll("\\.", "/") + ".class";
        URL resource = FindBugs.class.getClassLoader().getResource(classFilePath);
        if (resource != null && resource.getProtocol().equals("file")) {
            try {
                String classfile = URLDecoder.decode(resource.getPath(), Charset.defaultCharset().name());
                Matcher m = Pattern.compile("(.*)/.*?/edu/umd.*").matcher(classfile);
                if (m.matches()) {
                    String home = m.group(1);
                    if (new File(home + "/etc/findbugs.xml").exists()) {
                        FindBugs.setHome(home);
                        return home;
                    }
                }
            } catch (UnsupportedEncodingException e) {
            }
        }
        return null;

    }

    public static String getFindBugsHome() {

        String homeDir = FindBugs.getHome();

        if (homeDir == null) {
            // Attempt to infer findbugs.home from the observed
            // location of findbugs.jar.
            homeDir = inferFindBugsHome();
        }
        return homeDir;

    }

    @CheckForNull
    public static URL getCoreResource(String name) {
        URL u = PluginLoader.getCoreResource(name);
        return u;
    }

    /**
     * @throws PluginException
     *
     */
    private void loadCorePlugin() {
        //
        // Load the core plugin.
        //
        PluginLoader corePluginLoader = PluginLoader.getCorePluginLoader();
        Plugin plugin = corePluginLoader.getPlugin();
        loadPlugin(plugin);
        corePlugin = corePluginLoader.getPlugin();
    }



    final static boolean DEBUG_JAWS = SystemProperties.getBoolean("findbugs.jaws.debug");

    /**
     * @param message
     */

    public static void jawsDebugMessage(String message) {
        if (DEBUG_JAWS)
            JOptionPane.showMessageDialog(null, message);
        else if (FindBugs.DEBUG)
            System.err.println(message);
    }

    public static void jawsErrorMessage(String message) {
        if (DEBUG_JAWS)
            JOptionPane.showMessageDialog(null, message);
        else
            System.err.println(message);
    }

    void loadPlugin(Plugin plugin)  {

        pluginByIdMap.put(plugin.getPluginId(), plugin);

        // Register all of the detectors that this plugin contains
        for (DetectorFactory factory : plugin.getDetectorFactories()) {
            registerDetector(factory);
        }


        for (BugCategory bugCategory : plugin.getBugCategories()) {
            registerBugCategory(bugCategory);
        }

        // Register the BugPatterns
        for (BugPattern bugPattern : plugin.getBugPatterns()) {
            registerBugPattern(bugPattern);
        }

        // Register the BugCodes
        for (BugCode bugCode : plugin.getBugCodes()) {
            registerBugCode(bugCode);
        }
        for (CloudPlugin cloud : plugin.getCloudPlugins()) {
            registerCloud(cloud);
        }
    }

    public  Map<String, CloudPlugin> getRegisteredClouds() {
        return Collections.unmodifiableMap(registeredClouds);
    }

    /**
     * @param cloudPlugin
     */
      void registerCloud(CloudPlugin cloudPlugin) {
       LOGGER.log(Level.FINE, "Registering " + cloudPlugin.getId());
       registeredClouds.put(cloudPlugin.getId(), cloudPlugin);
    }

    /**
     * @param array
     */
    public void setPluginList(URL[] array) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param plugins
     */
    public void setPlugins(Plugin[] plugins) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the metadata for a bug category. If the category's metadata has
     * already been set, this does nothing.
     * @param bc
     *            the BugCategory object holding the metadata for the category
     *
     * @return false if the category's metadata has already been set, true
     *         otherwise
     */
    public boolean registerBugCategory(BugCategory bc) {
        String category = bc.getCategory();
        if (categoryDescriptionMap.get(category) != null)
            return false;
        categoryDescriptionMap.put(category, bc);
        return true;
    }

    /**
     * Register a BugPattern.
     *
     * @param bugPattern
     *            the BugPattern
     */
    public void registerBugPattern(BugPattern bugPattern) {
        bugPatternMap.put(bugPattern.getType(), bugPattern);
    }

    /**
     * Get an Iterator over all registered bug patterns.
     */
    public Iterator<BugPattern> bugPatternIterator() {
        DetectorFactoryCollection.instance(); // ensure detectors loaded

        return bugPatternMap.values().iterator();
    }

    /**
     * Get an Iterator over all registered bug patterns.
     */
    public Collection<BugPattern> getBugPatterns() {
       return bugPatternMap.values();
    }

    /**
     * Look up bug pattern.
     *
     * @param bugType
     *            the bug type for the bug pattern
     * @return the BugPattern, or null if it can't be found
     */
    public @CheckForNull BugPattern lookupBugPattern(String bugType) {
        return bugPatternMap.get(bugType);
    }

    /**
     * Get an Iterator over all registered bug codes.
     */
    public Iterator<BugCode> bugCodeIterator() {
       return bugCodeMap.values().iterator();
    }

    /**
     * Register a BugCode.
     *
     * @param bugCode
     *            the BugCode
     */
    public void registerBugCode(BugCode bugCode) {
        bugCodeMap.put(bugCode.getAbbrev(), bugCode);
    }

    /**
     * Get a description for given "bug type". FIXME: this is referred to
     * elsewhere as the "bug code" or "bug abbrev". Should make the terminology
     * consistent everywhere. In this case, the bug type refers to the short
     * prefix code prepended to the long and short bug messages.
     *
     * @param shortBugType
     *            the short bug type code
     * @return the description of that short bug type code means
     */
    public BugCode getBugCode(String shortBugType) {
        BugCode bugCode = bugCodeMap.get(shortBugType);
        if (bugCode == null)
            throw new IllegalArgumentException("Error: missing bug code for key" + shortBugType);
        return bugCode;
    }

    /**
     * Get the BugCategory object for a category key. Returns null if no
     * BugCategory object can be found.
     *
     * @param category
     *            the category key
     * @return the BugCategory object (may be null)
     */
    public BugCategory getBugCategory(String category) {
        return categoryDescriptionMap.get(category);
    }

    /**
     * Get a Collection containing all known bug category keys. E.g.,
     * "CORRECTNESS", "MT_CORRECTNESS", "PERFORMANCE", etc.
     *
     * @return Collection of bug category keys.
     */
    public Collection<String> getBugCategories() {
        return categoryDescriptionMap.keySet(); // backed by the Map
    }

    public Collection<BugCategory> getBugCategoryObjects() {
        return categoryDescriptionMap.values(); // backed by the Map
    }

    /**
     * @return
     */
    @Deprecated
    public URL[] getPluginList() {
        return new URL[0];
    }
}

// vim:ts=4
