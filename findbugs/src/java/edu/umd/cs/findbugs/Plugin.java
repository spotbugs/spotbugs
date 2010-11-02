/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.classfile.IAnalysisEngineRegistrar;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.plan.DetectorOrderingConstraint;
import edu.umd.cs.findbugs.util.DualKeyHashMap;

/**
 * A FindBugs plugin. A plugin contains executable Detector classes, as well as
 * meta information decribing those detectors (such as human-readable detector
 * and bug descriptions).
 *
 * @see PluginLoader
 * @author David Hovemeyer
 */
public class Plugin {
    private final String pluginId;

    private String provider;

    private String website;

    private String shortDescription;
    private String detailedDescription;

    private final ArrayList<DetectorFactory> detectorFactoryList;

    private final LinkedHashSet<BugPattern> bugPatterns;

    private final LinkedHashSet<BugCode> bugCodeList;

    private final LinkedHashSet<BugCategory> bugCategoryList;
    private final LinkedHashSet<CloudPlugin> cloudList = new LinkedHashSet<CloudPlugin>();

    private final DualKeyHashMap<Class, String, ComponentPlugin> componentPlugins
        = new DualKeyHashMap<Class, String, ComponentPlugin> ();

    private BugRanker bugRanker;

    // Ordering constraints
    private final ArrayList<DetectorOrderingConstraint> interPassConstraintList;

    private final ArrayList<DetectorOrderingConstraint> intraPassConstraintList;

    // Optional: engine registrar class
    private Class<? extends IAnalysisEngineRegistrar> engineRegistrarClass;

    // PluginLoader that loaded this plugin
    private final PluginLoader pluginLoader;

    private final boolean enabledByDefault;

    EnabledState enabledState = EnabledState.PLUGIN_DEFAULT;

    static Map<URI, Plugin> allPlugins = new LinkedHashMap<URI, Plugin>();

    /**
     * Constructor. Creates an empty plugin object.
     *
     * @param pluginId
     *            the plugin's unique identifier
     * @param enabled TODO
     */
    public Plugin(String pluginId, PluginLoader pluginLoader, boolean enabled) {
        this.pluginId = pluginId;
        this.detectorFactoryList = new ArrayList<DetectorFactory>();
        this.bugPatterns = new LinkedHashSet<BugPattern>();
        this.bugCodeList = new LinkedHashSet<BugCode>();
        this.bugCategoryList = new LinkedHashSet<BugCategory>();
        this.interPassConstraintList = new ArrayList<DetectorOrderingConstraint>();
        this.intraPassConstraintList = new ArrayList<DetectorOrderingConstraint>();
        this.pluginLoader = pluginLoader;
        this.enabledByDefault = enabled;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ":" + pluginId;
    }

    /**
     * Set whether or not this Plugin is enabled.
     *
     * @param enabled
     *            true if the Plugin is enabled, false if not
     */
    @Deprecated
    public void setEnabled(boolean enabled) {
        throw new UnsupportedOperationException();
    }

    /**
     * Return whether or not the Plugin is enabled.
     *
     * @return true if the Plugin is enabled, false if not
     */
    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    /**
     * Set plugin provider.
     *
     * @param provider
     *            the plugin provider
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * Get the plugin provider.
     *
     * @return the provider, or null if the provider was not specified
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Set plugin website.
     *
     * @param website
     *            the plugin website
     */
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * Get the plugin website.
     *
     * @return the website, or null if the was not specified
     */
    public String getWebsite() {
        return website;
    }

    /**
     * Set plugin short (one-line) text description.
     *
     * @param shortDescription
     *            the plugin short text description
     */
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    /**
     * Get the plugin short (one-line) description.
     *
     * @return the short description, or null if the short description was not
     *         specified
     */
    public String getShortDescription() {
        return shortDescription;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public void setDetailedDescription(String detailedDescription) {
        this.detailedDescription = detailedDescription;
    }

    /**
     * Add a DetectorFactory for a Detector implemented by the Plugin.
     *
     * @param factory
     *            the DetectorFactory
     */
    public void addDetectorFactory(DetectorFactory factory) {
        detectorFactoryList.add(factory);
    }

    public void addCloudPlugin(CloudPlugin cloudPlugin) {
        cloudList.add(cloudPlugin);
    }

    /**
     * Add a BugPattern reported by the Plugin.
     *
     * @param bugPattern
     */
    public void addBugPattern(BugPattern bugPattern) {
        bugPatterns.add(bugPattern);
    }

    /**
     * Add a BugCode reported by the Plugin.
     *
     * @param bugCode
     */
    public void addBugCode(BugCode bugCode) {
        bugCodeList.add(bugCode);
    }

    /**
     * Add a BugCategory reported by the Plugin.
     *
     * @param bugCode
     */
    public void addBugCategory(BugCategory bugCategory) {
        bugCategoryList.add(bugCategory);
    }
    /**
     * Add an inter-pass Detector ordering constraint.
     *
     * @param constraint
     *            the inter-pass Detector ordering constraint
     */
    public void addInterPassOrderingConstraint(DetectorOrderingConstraint constraint) {
        interPassConstraintList.add(constraint);
    }

    /**
     * Add an intra-pass Detector ordering constraint.
     *
     * @param constraint
     *            the intra-pass Detector ordering constraint
     */
    public void addIntraPassOrderingConstraint(DetectorOrderingConstraint constraint) {
        intraPassConstraintList.add(constraint);
    }

    /**
     * Look up a DetectorFactory by short name.
     *
     * @param shortName
     *            the short name
     * @return the DetectorFactory
     */
    public DetectorFactory getFactoryByShortName(final String shortName) {
        return findFirstMatchingFactory(new FactoryChooser() {
            public boolean choose(DetectorFactory factory) {
                return factory.getShortName().equals(shortName);
            }
        });
    }

    /**
     * Look up a DetectorFactory by full name.
     *
     * @param fullName
     *            the full name
     * @return the DetectorFactory
     */
    public DetectorFactory getFactoryByFullName(final String fullName) {
        return findFirstMatchingFactory(new FactoryChooser() {
            public boolean choose(DetectorFactory factory) {
                return factory.getFullName().equals(fullName);
            }
        });
    }

    /**
     * Get Iterator over DetectorFactory objects in the Plugin.
     *
     * @return Iterator over DetectorFactory objects
     */
    public Collection<DetectorFactory> getDetectorFactories() {
        return detectorFactoryList;
    }

    /**
     * Get the set of BugPatterns
     *
     */
    public Set<BugPattern> getBugPatterns() {
        return bugPatterns;
    }

    /**
     * Get Iterator over BugCode objects in the Plugin.
     *
     * @return Iterator over BugCode objects
     */
    public Set<BugCode> getBugCodes() {
        return bugCodeList;
    }
    /**
     * Get Iterator over BugCategories objects in the Plugin.
     *
     * @return Iterator over BugCategory objects
     */
    public Set<BugCategory> getBugCategories() {
        return bugCategoryList;
    }

    public Set<CloudPlugin> getCloudPlugins() {
        return cloudList;
    }
    /**
     * Return an Iterator over the inter-pass Detector ordering constraints.
     */
    public Iterator<DetectorOrderingConstraint> interPassConstraintIterator() {
        return interPassConstraintList.iterator();
    }

    /**
     * Return an Iterator over the intra-pass Detector ordering constraints.
     */
    public Iterator<DetectorOrderingConstraint> intraPassConstraintIterator() {
        return intraPassConstraintList.iterator();
    }

    /**
     * @return Returns the pluginId.
     */
    public String getPluginId() {
        return pluginId;
    }
    /**
     * @return Returns the short pluginId.
     */
    public String getShortPluginId() {
        int i = pluginId.lastIndexOf('.');
        return pluginId.substring(i+1);
    }
    /**
     * Set the analysis engine registrar class that, when instantiated, can be
     * used to register the plugin's analysis engines with the analysis cache.
     *
     * @param engineRegistrarClass
     *            The engine registrar class to set.
     */
    public void setEngineRegistrarClass(Class<? extends IAnalysisEngineRegistrar> engineRegistrarClass) {
        this.engineRegistrarClass = engineRegistrarClass;
    }

    /**
     * Get the analysis engine registrar class that, when instantiated, can be
     * used to register the plugin's analysis engines with the analysis cache.
     *
     * @return Returns the engine registrar class.
     */
    public Class<? extends IAnalysisEngineRegistrar> getEngineRegistrarClass() {
        return engineRegistrarClass;
    }

    /**
     * @return Returns the pluginLoader.
     */
    public PluginLoader getPluginLoader() {
        return pluginLoader;
    }

    private interface FactoryChooser {
        public boolean choose(DetectorFactory factory);
    }

    private @CheckForNull
    DetectorFactory findFirstMatchingFactory(FactoryChooser chooser) {
        for (DetectorFactory factory : getDetectorFactories()) {
            if (chooser.choose(factory))
                return factory;
        }
        return null;
    }

    /**
     * @param ranker
     */
    public void setBugRanker(BugRanker ranker) {
        this.bugRanker = ranker;
    }

    public BugRanker getBugRanker() {
        return bugRanker;
    }

    <T> void addComponentPlugin(Class<T> componentClass, ComponentPlugin<T> filter) {
        if (!componentClass.isAssignableFrom(filter.getComponentClass()))
                throw new IllegalArgumentException();
        componentPlugins.put(componentClass, filter.getId(), filter);
    }

    public <T> Iterable<ComponentPlugin<T>> getComponentPlugins(Class<T> componentClass) {
        Collection values = componentPlugins.get(componentClass).values();
        return values;
    }

    public <T>  ComponentPlugin<T> getComponentPlugin(Class<T> componentClass, String name) {
        return componentPlugins.get(componentClass, name);
    }

    public static @CheckForNull Plugin getByName(String name) {
        for(Plugin plugin : allPlugins.values()) {
            if (name.equals(plugin.getPluginId()) || name.equals(plugin.getShortPluginId()))
                return plugin;
        }
        return null;
    }

    public static Collection<Plugin> getAllPlugins() {
        return Plugin.allPlugins.values();
    }
    enum EnabledState { PLUGIN_DEFAULT, ENABLED, DISABLED};



EnabledState enabled = EnabledState.PLUGIN_DEFAULT;
public boolean isCorePlugin() {
    return pluginLoader.isCorePlugin();
}
/**
 * @return
 */
    public boolean isGloballyEnabled() {
        if (isCorePlugin())
            return true;
        switch (enabled) {
        case ENABLED:
            return true;
        case DISABLED:
            return false;
        case PLUGIN_DEFAULT:
            return isEnabledByDefault();
        default:
            throw new IllegalStateException("Unknown state : " + enabled);
        }
    }

    /**
     * @return
     */
    public void setGloballyEnabled(boolean enabled) {
        if (isCorePlugin())
            return;

        if (enabled) {
            if (isEnabledByDefault())
                this.enabled = EnabledState.PLUGIN_DEFAULT;
            else
                this.enabled = EnabledState.ENABLED;
        } else {
            if (isEnabledByDefault())
                this.enabled = EnabledState.DISABLED;
            else
                this.enabled = EnabledState.PLUGIN_DEFAULT;
        }

    }

    public boolean isInitialPlugin() {
        return getPluginLoader().initialPlugin;
    }

    public URL getResource(String name) {
        return getPluginLoader().getResource(name);
    }

    public ClassLoader getClassLoader() {
        return getPluginLoader().getClassLoader();
    }

    public static Plugin addAvailablePlugin(URL u) throws PluginException {

        PluginLoader pluginLoader = PluginLoader.getPluginLoader(u, PluginLoader.class.getClassLoader());
        Plugin plugin = pluginLoader.loadPlugin();
        // register new clouds
        I18N.instance().loadPlugin(plugin);
        return plugin;
    }

}

// vim:ts=4
