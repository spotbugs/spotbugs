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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.updates.PluginUpdateListener;
import edu.umd.cs.findbugs.updates.UpdateCheckCallback;
import edu.umd.cs.findbugs.updates.UpdateChecker;
import edu.umd.cs.findbugs.util.ClassPathUtil;
import edu.umd.cs.findbugs.util.FutureValue;

/**
 * The DetectorFactoryCollection stores all of the DetectorFactory objects used
 * to create the Detectors which implement the various analyses. It is a
 * singleton class.
 *
 * @author David Hovemeyer
 * @see DetectorFactory
 */
public class DetectorFactoryCollection implements UpdateCheckCallback {

    private static final Logger LOGGER = Logger.getLogger(DetectorFactoryCollection.class.getName());
    private static final boolean DEBUG_JAWS = SystemProperties.getBoolean("findbugs.jaws.debug");
    //    private static final boolean DEBUG = Boolean.getBoolean("dfc.debug");

    private static DetectorFactoryCollection theInstance;
    private static final Object lock = new Object();

    private final Map<String, Plugin> pluginByIdMap = new LinkedHashMap<String, Plugin>();
    private Plugin corePlugin;
    private final List<DetectorFactory> factoryList = new ArrayList<DetectorFactory>();
    private final Map<String, DetectorFactory> factoriesByName = new HashMap<String, DetectorFactory>();
    private final Map<String, DetectorFactory> factoriesByDetectorClassName = new HashMap<String, DetectorFactory>();
    private final Map<String, CloudPlugin> registeredClouds = new LinkedHashMap<String, CloudPlugin>();
    protected final Map<String, BugCategory> categoryDescriptionMap = new HashMap<String, BugCategory>();
    protected final Map<String, BugPattern> bugPatternMap = new HashMap<String, BugPattern>();
    protected final Map<String, BugCode> bugCodeMap = new HashMap<String, BugCode>();

    private final UpdateChecker updateChecker;
    private final CopyOnWriteArrayList<PluginUpdateListener> pluginUpdateListeners
    = new CopyOnWriteArrayList<PluginUpdateListener>();
    private volatile List<UpdateChecker.PluginUpdate> updates;
    private boolean updatesForced;
    private final Collection<Plugin> pluginsToUpdate;
    final Map<String, String> globalOptions = new HashMap<String,String>();
    final Map<String, Plugin> globalOptionsSetter = new HashMap<String,Plugin>();

    protected DetectorFactoryCollection() {
        this(true, false, Plugin.getAllPlugins(), new ArrayList<Plugin>());
    }

    protected DetectorFactoryCollection(Plugin onlyPlugin) {
        this(false, true, Collections.singleton(onlyPlugin), new ArrayList<Plugin>());
    }

    protected DetectorFactoryCollection(Collection<Plugin> enabled) {
        this(true, true, enabled, enabled);
    }

    private DetectorFactoryCollection(boolean loadCore, boolean forceLoad,
            @Nonnull Collection<Plugin> pluginsToLoad,
            @Nonnull Collection<Plugin> enabledPlugins) {
        if(loadCore) {
            loadCorePlugin();
        }
        for(Plugin plugin : pluginsToLoad) {
            if (forceLoad || plugin.isGloballyEnabled() && !plugin.isCorePlugin()) {
                loadPlugin(plugin);
                if(!enabledPlugins.contains(plugin)) {
                    enabledPlugins.add(plugin);
                }
            }
        }
        setGlobalOptions();
        updateChecker = new UpdateChecker(this);
        pluginsToUpdate = combine(enabledPlugins);
        // There are too many places where the detector factory collection can be created,
        // and in many cases it has nothing to do with update checks, like Plugin#addCustomPlugin(URI).

        // Line below commented to allow custom plugins be loaded/registered BEFORE we
        // start to update something. The reason is that custom plugins
        // can disable update globally OR just will be NOT INCLUDED in the update check!
        // updateChecker.checkForUpdates(pluginsToUpdate, false);
    }

    /**
     * @param force whether the updates should be shown to the user no matter what - even if the updates have been
     *        seen before
     */
    public void checkForUpdates(boolean force) {
        updateChecker.checkForUpdates(pluginsToUpdate, force);
    }

    private Collection<Plugin> combine(Collection<Plugin> enabled) {
        List<Plugin> result = new ArrayList<Plugin>(enabled);
        if (corePlugin != null && !result.contains(corePlugin)) {
            result.add(corePlugin);
        }
        return result;
    }

    /**
     * Reset the factory singleton.
     * <p>
     * <b>Implementation note:</b> This method is public for tests only!
     *
     * @param instance
     *            use null to clear the instance
     */
    public static void resetInstance(@CheckForNull DetectorFactoryCollection instance) {
        synchronized (lock) {
            theInstance = instance;
        }
    }

    /**
     * Get the single instance of DetectorFactoryCollection, which knows each and every
     * loaded plugin, independently of it's enablement
     */
    public static DetectorFactoryCollection instance() {
        synchronized (lock) {
            if (theInstance == null) {
                theInstance = new DetectorFactoryCollection();
            }
            return theInstance;
        }
    }

    private void setGlobalOptions() {
        globalOptions.clear();
        globalOptionsSetter.clear();

        for(Plugin p : plugins()) {
            if (p.isGloballyEnabled()) {
                for(Map.Entry<String, String> e : p.getMyGlobalOptions().entrySet()) {
                    String key = e.getKey();
                    String value = e.getValue();
                    String oldValue = globalOptions.get(key);

                    if (oldValue != null) {
                        if (oldValue.equals(value)) {
                            continue;
                        }
                        Plugin oldP = globalOptionsSetter.get(key);
                        throw new RuntimeException(
                                "Incompatible global options for " + key + "; conflict between " + oldP.getPluginId() + " and " + p.getPluginId());
                    }
                    globalOptions.put(key, value);
                    globalOptionsSetter.put(key, p);
                }
            }
        }
    }
    @Override
    public  @CheckForNull String getGlobalOption(String key) {
        return globalOptions.get(key);
    }

    @Override
    public  @CheckForNull Plugin getGlobalOptionSetter(String key) {
        return globalOptionsSetter.get(key);
    }


    /**
     * Return an Iterator over all available Plugin objects.
     */
    public Iterator<Plugin> pluginIterator() {
        return pluginByIdMap.values().iterator();
    }

    /**
     * Return an Collection of all available Plugin objects.
     */
    public Collection<Plugin> plugins() {
        return pluginByIdMap.values();
    }

    @Nonnull
    public Plugin getCorePlugin() {
        if (corePlugin == null) {
            throw new IllegalStateException("No core plugin");
        }
        return corePlugin;
    }


    /**
     * Get a Plugin by its unique id.
     *
     * @param pluginId
     *            the unique id
     * @return the Plugin with that id, or null if no such Plugin is found
     */
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

    public boolean isDisabledByDefault(String bugPatternOrCode) {
        @CheckForNull BugPattern pattern = lookupBugPattern(bugPatternOrCode);
        if (pattern != null) {
            for(DetectorFactory fac : factoryList) {
                if (fac.isDefaultEnabled()  && fac.getReportedBugPatterns().contains(pattern)) {
                    return false;
                }
            }
            return true;
        }
        @CheckForNull BugCode code = lookupBugCode(bugPatternOrCode);
        if (code != null) {
            for(DetectorFactory fac : factoryList) {
                if (fac.isDefaultEnabled()) {
                    for(BugPattern p : fac.getReportedBugPatterns()) {
                        if (p.getBugCode().equals(code)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        return false;
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
        if (FindBugs.DEBUG) {
            System.out.println("Registering detector: " + factory.getFullName());
        }
        String detectorName = factory.getShortName();
        if(!factoryList.contains(factory)) {
            factoryList.add(factory);
        } else {
            LOGGER.log(Level.WARNING, "Trying to add already registered factory: " + factory +
                    ", " + factory.getPlugin());
        }
        factoriesByName.put(detectorName, factory);
        factoriesByDetectorClassName.put(factory.getFullName(), factory);
    }

    void unRegisterDetector(DetectorFactory factory) {
        if (FindBugs.DEBUG) {
            System.out.println("Unregistering detector: " + factory.getFullName());
        }
        String detectorName = factory.getShortName();
        factoryList.remove(factory);
        factoriesByName.remove(detectorName);
        factoriesByDetectorClassName.remove(factory.getFullName());
    }


    /**
     * See if the location of ${findbugs.home} can be inferred from the location
     * of findbugs.jar in the classpath.
     *
     * @return inferred ${findbugs.home}, or null if we can't figure it out
     */
    private static String inferFindBugsHome() {
        Pattern[] findbugsJarNames = { Pattern.compile("findbugs\\.jar$"), };

        for (Pattern jarNamePattern : findbugsJarNames) {
            String findbugsJarCodeBase = ClassPathUtil.findCodeBaseInClassPath(jarNamePattern,
                    SystemProperties.getProperty("java.class.path"));
            if (findbugsJarCodeBase != null) {
                File findbugsJar = new File(findbugsJarCodeBase);
                File libDir = findbugsJar.getParentFile();
                if ("lib".equals(libDir.getName())) {
                    String fbHome = libDir.getParent();
                    FindBugs.setHome(fbHome);
                    return fbHome;
                }
            }
        }
        String classFilePath = FindBugs.class.getName().replaceAll("\\.", "/") + ".class";
        URL resource = FindBugs.class.getClassLoader().getResource(classFilePath);
        if (resource != null && "file".equals(resource.getProtocol())) {
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
        return PluginLoader.getCoreResource(name);
    }

    private void loadCorePlugin() {
        Plugin plugin = PluginLoader.getCorePluginLoader().getPlugin();
        loadPlugin(plugin);
        corePlugin = plugin;
    }

    public static void jawsDebugMessage(String message) {
        if (DEBUG_JAWS) {
            JOptionPane.showMessageDialog(null, message);
        } else if (FindBugs.DEBUG) {
            System.err.println(message);
        }
    }

    void loadPlugin(Plugin plugin)  {

        if (FindBugs.DEBUG) {
            System.out.println("Loading " + plugin.getPluginId());
        }
        pluginByIdMap.put(plugin.getPluginId(), plugin);

        setGlobalOptions();

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

    void unLoadPlugin(Plugin plugin)  {
        pluginByIdMap.remove(plugin.getPluginId());

        setGlobalOptions();

        for (DetectorFactory factory : plugin.getDetectorFactories()) {
            unRegisterDetector(factory);
        }
        for (BugCategory bugCategory : plugin.getBugCategories()) {
            unRegisterBugCategory(bugCategory);
        }
        for (BugPattern bugPattern : plugin.getBugPatterns()) {
            unRegisterBugPattern(bugPattern);
        }
        for (BugCode bugCode : plugin.getBugCodes()) {
            unRegisterBugCode(bugCode);
        }
        for (CloudPlugin cloud : plugin.getCloudPlugins()) {
            unRegisterCloud(cloud);
        }
    }

    @Override
    @SuppressWarnings({"ConstantConditions"})
    public void pluginUpdateCheckComplete(List<UpdateChecker.PluginUpdate> newUpdates, boolean force) {
        this.updates = newUpdates;
        this.updatesForced = force;
        for (PluginUpdateListener listener : pluginUpdateListeners) {
            try {
                listener.pluginUpdateCheckComplete(newUpdates, force);
            } catch (Throwable e) {
                LOGGER.log(Level.INFO, "Error during update check callback", e);
            }
        }
    }

    public void addPluginUpdateListener(PluginUpdateListener listener) {
        pluginUpdateListeners.add(listener);
        if (updates != null) {
            listener.pluginUpdateCheckComplete(updates, updatesForced);
        } else if(!updateChecker.updateChecksGloballyDisabled()){
            checkForUpdates(false);
        }
    }

    public FutureValue<Collection<UpdateChecker.PluginUpdate>> getUpdates() {
        final FutureValue<Collection<UpdateChecker.PluginUpdate>> results = new FutureValue<Collection<UpdateChecker.PluginUpdate>>();
        addPluginUpdateListener(new PluginUpdateListener() {
            @Override
            public void pluginUpdateCheckComplete(Collection<UpdateChecker.PluginUpdate> u, boolean force) {
                results.set(u);
            }
        });
        return results;
    }


    public  Map<String, CloudPlugin> getRegisteredClouds() {
        return Collections.unmodifiableMap(registeredClouds);
    }

    void registerCloud(CloudPlugin cloudPlugin) {
        LOGGER.log(Level.FINE, "Registering " + cloudPlugin.getId());
        registeredClouds.put(cloudPlugin.getId(), cloudPlugin);
    }
    void unRegisterCloud(CloudPlugin cloudPlugin) {
        LOGGER.log(Level.FINE, "Unregistering " + cloudPlugin.getId());
        registeredClouds.remove(cloudPlugin.getId());
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
        if (categoryDescriptionMap.get(category) != null) {
            return false;
        }
        categoryDescriptionMap.put(category, bc);
        return true;
    }

    protected boolean unRegisterBugCategory(BugCategory bc) {
        String category = bc.getCategory();
        categoryDescriptionMap.remove(category);
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

    protected void unRegisterBugPattern(BugPattern bugPattern) {
        bugPatternMap.remove(bugPattern.getType());
    }

    /**
     * Get an Iterator over all registered bug patterns.
     */
    public Iterator<BugPattern> bugPatternIterator() {
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
        if (bugType == null) {
            return null;
        }
        return bugPatternMap.get(bugType);
    }

    public void registerBugCode(BugCode bugCode) {
        bugCodeMap.put(bugCode.getAbbrev(), bugCode);
    }
    protected void unRegisterBugCode(BugCode bugCode) {
        bugCodeMap.remove(bugCode.getAbbrev());
    }

    public Collection<BugCode> getBugCodes() {
        return bugCodeMap.values();
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
    public @Nonnull BugCode getBugCode(String shortBugType) {
        BugCode bugCode = lookupBugCode(shortBugType);
        if (bugCode == null) {
            throw new IllegalArgumentException("Error: missing bug code for key" + shortBugType);
        }
        return bugCode;
    }

    /**
     * @param shortBugType the short bug type code
     * @return the description of that short bug type code means
     */
    public @CheckForNull BugCode lookupBugCode(String shortBugType) {
        return bugCodeMap.get(shortBugType);
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
     * Excludes hidden bug categories
     *
     * @return Collection of bug category keys.
     */
    public Collection<String> getBugCategories() {
        ArrayList<String> result = new ArrayList<String>(categoryDescriptionMap.size());
        for(BugCategory c : categoryDescriptionMap.values()) {
            if (!c.isHidden()) {
                result.add(c.getCategory());
            }
        }
        return result;
    }

    public Collection<BugCategory> getBugCategoryObjects() {
        return categoryDescriptionMap.values(); // backed by the Map
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}
