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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;

import edu.umd.cs.findbugs.util.SecurityManagerHandler;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.classfile.IAnalysisEngineRegistrar;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.io.IO;
import edu.umd.cs.findbugs.plan.ByInterfaceDetectorFactorySelector;
import edu.umd.cs.findbugs.plan.DetectorFactorySelector;
import edu.umd.cs.findbugs.plan.DetectorOrderingConstraint;
import edu.umd.cs.findbugs.plan.ReportingDetectorFactorySelector;
import edu.umd.cs.findbugs.plan.SingleDetectorFactorySelector;
import edu.umd.cs.findbugs.plugins.DuplicatePluginIdError;
import edu.umd.cs.findbugs.plugins.DuplicatePluginIdException;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.JavaWebStart;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.xml.XMLUtil;

/**
 * Loader for a FindBugs plugin. A plugin is a jar file containing two metadata
 * files, "findbugs.xml" and "messages.xml". Those files specify
 * <ul>
 * <li>the bug pattern Detector classes,
 * <li>the bug patterns detected (including all text for displaying detected
 * instances of those patterns), and
 * <li>the "bug codes" which group together related bug instances
 * </ul>
 *
 * <p>
 * The PluginLoader creates a Plugin object to store the Detector factories and
 * metadata.
 * </p>
 *
 * @author David Hovemeyer
 * @see Plugin
 * @see PluginException
 */
public class PluginLoader implements AutoCloseable {

    private static final String XPATH_PLUGIN_SHORT_DESCRIPTION = "/MessageCollection/Plugin/ShortDescription";
    private static final String XPATH_PLUGIN_WEBSITE = "/FindbugsPlugin/@website";
    private static final String XPATH_PLUGIN_PROVIDER = "/FindbugsPlugin/@provider";
    private static final String XPATH_PLUGIN_PLUGINID = "/FindbugsPlugin/@pluginid";

    private static final Logger LOG = LoggerFactory.getLogger(PluginLoader.class);
    static boolean lazyInitialization = false;
    static LinkedList<PluginLoader> partiallyInitialized = new LinkedList<>();


    // Keep a count of how many plugins we've seen without a
    // "pluginid" attribute, so we can assign them all unique ids.
    private static int nextUnknownId;

    // ClassLoader used to load classes and resources
    private ClassLoader classLoader;

    private final ClassLoader classLoaderForResources;

    // The loaded Plugin
    private final Plugin plugin;

    private final boolean corePlugin;

    boolean initialPlugin;

    boolean cannotDisable;

    private boolean optionalPlugin;

    private final URL loadedFrom;

    private final String jarName;

    private final URI loadedFromUri;

    // The classloaders we have created, so we can close then if we want to close the plugin
    private List<URLClassLoader> createdClassLoaders = new ArrayList<>();

    /** plugin Id for parent plugin */
    String parentId;

    static HashSet<String> loadedPluginIds = new HashSet<>();
    static {
        LOG.debug("Debugging plugin loading. SpotBugs version {}", Version.VERSION_STRING);
        loadInitialPlugins();
    }

    /**
     * Constructor.
     *
     * @param url
     *            the URL of the plugin Jar file
     * @throws PluginException
     *             if the plugin cannot be fully loaded
     */
    @Deprecated
    public PluginLoader(URL url) throws PluginException {
        this(url, toUri(url), null, false, true);
    }


    /**
     * Constructor.
     *
     * @param url
     *            the URL of the plugin Jar file
     * @param parent
     *            the parent classloader
     * @deprecated Use {@link #PluginLoader(URL,URI,ClassLoader,boolean,boolean)} instead
     */
    @Deprecated
    public PluginLoader(URL url, ClassLoader parent) throws PluginException {
        this(url, toUri(url), parent, false, true);
    }

    public boolean hasParent() {
        return parentId != null && parentId.length() > 0;
    }

    /**
     * Constructor.
     *
     * @param url
     *            the URL of the plugin Jar file
     * @param uri
     * @param parent
     *            the parent classloader
     * @param isInitial
     *          is this plugin loaded from one of the standard locations for plugins
     * @param optional
     *          is this an optional plugin
     */
    private PluginLoader(@Nonnull URL url, URI uri, ClassLoader parent, boolean isInitial, boolean optional) throws PluginException {
        URL[] loaderURLs = createClassloaderUrls(url);
        classLoaderForResources = buildURLClassLoader(loaderURLs);
        loadedFrom = url;
        loadedFromUri = uri;
        jarName = getJarName(url);
        corePlugin = false;
        initialPlugin = isInitial;
        optionalPlugin = optional;
        plugin = init();
        if (!hasParent()) {
            classLoader = buildURLClassLoader(loaderURLs, parent);
        } else {
            if (parent != PluginLoader.class.getClassLoader()) {
                throw new IllegalArgumentException("Can't specify parentid " + parentId + " and provide a separate class loader");
            }
            Plugin parentPlugin = Plugin.getByPluginId(parentId);
            if (parentPlugin != null) {
                parent = parentPlugin.getClassLoader();
                classLoader = buildURLClassLoader(loaderURLs, parent);
            }
        }
        if (classLoader == null) {
            if (!lazyInitialization) {
                throw new IllegalStateException("Can't find parent plugin " + parentId);
            }
            partiallyInitialized.add(this);
        } else {
            loadPluginComponents();
            Plugin.putPlugin(loadedFromUri, plugin);
        }
    }

    private static void finishLazyInitialization() {
        if (!lazyInitialization) {
            throw new IllegalStateException("Not in lazy initialization mode");
        }
        while (!partiallyInitialized.isEmpty()) {
            boolean changed = false;
            LinkedList<String> unresolved = new LinkedList<>();
            Set<String> needed = new TreeSet<>();

            for (Iterator<PluginLoader> i = partiallyInitialized.iterator(); i.hasNext();) {
                PluginLoader pluginLoader = i.next();
                String pluginId = pluginLoader.getPlugin().getPluginId();
                assert pluginLoader.hasParent();
                String parentid = pluginLoader.parentId;
                Plugin parent = Plugin.getByPluginId(parentid);
                if (parent != null) {
                    i.remove();
                    try {
                        URL[] loaderURLs = PluginLoader.createClassloaderUrls(pluginLoader.loadedFrom);
                        pluginLoader.classLoader = pluginLoader.buildURLClassLoader(loaderURLs, parent.getClassLoader());
                        pluginLoader.loadPluginComponents();
                        Plugin.putPlugin(pluginLoader.loadedFromUri, pluginLoader.plugin);
                    } catch (PluginException e) {
                        throw new RuntimeException("Unable to load plugin " + pluginId, e);
                    }
                    changed = true;
                } else {
                    unresolved.add(pluginId);
                    needed.add(parentid);
                }
            }
            if (!changed) {
                String msg = "Unable to load parent plugins " + needed + " in order to load " + unresolved;
                System.err.println(msg);
                AnalysisContext.logError(msg);
                msg = "Available plugins are " + Plugin.getAllPluginIds();
                System.err.println(msg);
                AnalysisContext.logError(msg);

                for (PluginLoader pluginLoader : partiallyInitialized) {
                    Plugin.removePlugin(pluginLoader.loadedFromUri);
                }
                partiallyInitialized.clear();
            }
        }
        lazyInitialization = false;
    }

    /**
     * Creates a new {@link URLClassLoader} and adds it to the list of classloaders
     * we need to close if we close the corresponding plugin
     *
     * @param
     *            urls the URLs from which to load classes and resources
     * @return a new {@link URLClassLoader}
     */
    private URLClassLoader buildURLClassLoader(URL[] urls) {
        URLClassLoader urlClassLoader = new URLClassLoader(urls);
        createdClassLoaders.add(urlClassLoader);

        return urlClassLoader;
    }

    /**
     * Creates a new {@link URLClassLoader} and adds it to the list of classloaders
     * we need to close if we close the corresponding plugin
     *
     * @param
     *            urls the URLs from which to load classes and resources
     * @param
     *            parent the parent class loader for delegation
     * @return a new {@link URLClassLoader}
     */
    private URLClassLoader buildURLClassLoader(URL[] urls, ClassLoader parent) {
        URLClassLoader urlClassLoader = new URLClassLoader(urls, parent);
        createdClassLoaders.add(urlClassLoader);

        return urlClassLoader;
    }

    /**
     * Closes the class loaders created in this {@link PluginLoader}
     * @throws IOException if a class loader fails to close, in that case the other classloaders won't be closed
     */
    @Override
    public void close() throws IOException {
        for (URLClassLoader urlClassLoader : createdClassLoaders) {
            urlClassLoader.close();
        }
    }


    /**
     * Patch for issue 3429143: allow plugins load classes/resources from 3rd
     * party jars
     *
     * @param url
     *            plugin jar location as url
     * @return non empty list with url to be used for loading classes from given
     *         plugin. If plugin jar contains standard Java manifest file, all
     *         entries of its "Class-Path" attribute will be translated to the
     *         jar-relative url's and added to the returned list. If plugin jar
     *         does not contains a manifest, or manifest does not have
     *         "Class-Path" attribute, the given argument will be the only entry
     *         in the array.
     * @throws PluginException
     */
    private static @Nonnull URL[] createClassloaderUrls(@Nonnull URL url) throws PluginException {
        List<URL> urls = new ArrayList<>();
        urls.add(url);

        Manifest mf = null;
        File f = new File(url.getPath());
        // default: try with jar/zip/war etc files
        if (!f.isDirectory()) {
            JarInputStream jis = null;
            try {
                jis = new JarInputStream(url.openStream());
                mf = jis.getManifest();
            } catch (IOException ioe) {
                throw new PluginException("Failed loading manifest for plugin jar: " + url, ioe);
            } finally {
                IO.close(jis);
            }
        } else {
            // If this is not a jar/zip/war etc file, can we can load from directory?
            // Allow plugins be loaded from "exploded jar" directories (e.g. while debugging
            // 3rd party FB plugin projects in Eclipse without packaging them to jars at all)
            File manifest = guessManifest(f);
            if (manifest != null) {
                FileInputStream is = null;
                try {
                    is = new FileInputStream(manifest);
                    mf = new Manifest(is);
                } catch (IOException e) {
                    throw new PluginException("Failed loading manifest for plugin jar: " + url, e);
                } finally {
                    IO.close(is);
                }
            }
        }
        if (mf != null) {
            try {
                addClassPathFromManifest(url, urls, mf);
            } catch (MalformedURLException e) {
                throw new PluginException("Failed loading manifest for plugin jar: " + url, e);
            }
        }
        return urls.toArray(new URL[0]);
    }

    private static void addClassPathFromManifest(@Nonnull URL url, @Nonnull List<URL> urls,
            @Nonnull Manifest mf) throws MalformedURLException {
        Attributes atts = mf.getMainAttributes();
        if (atts == null) {
            return;
        }
        String classPath = atts.getValue(Attributes.Name.CLASS_PATH);
        if (classPath != null) {
            String jarRoot = url.toString();
            jarRoot = jarRoot.substring(0, jarRoot.lastIndexOf('/') + 1);
            String[] jars = classPath.split(",");
            for (String jar : jars) {
                jar = jarRoot + jar.trim();
                urls.add(new URL(jar));
            }
        }
    }

    /**
     * Trying to find the manifest of "exploded plugin" in the current dir, "standard jar" manifest
     * location or "standard" Eclipse location (sibling to the current classpath)
     */
    @CheckForNull
    private static File guessManifest(@Nonnull File parent) {
        File file = new File(parent, "MANIFEST.MF");
        if (!file.isFile()) {
            file = new File(parent, "META-INF/MANIFEST.MF");
        }
        if (!file.isFile()) {
            file = new File(parent, "../META-INF/MANIFEST.MF");
        }
        if (file.isFile()) {
            return file;
        }
        return null;
    }

    /**
     * Constructor. Loads a plugin using the caller's class loader. This
     * constructor should only be used to load the "core" findbugs detectors,
     * which are built into findbugs.jar.
     * @throws PluginException
     */
    @Deprecated
    public PluginLoader() throws PluginException {
        classLoader = getClass().getClassLoader();
        classLoaderForResources = classLoader;
        corePlugin = true;
        initialPlugin = true;
        optionalPlugin = false;

        loadedFrom = computeCoreUrl();
        try {
            loadedFromUri = loadedFrom.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to parse uri: " + loadedFrom);
        }
        jarName = getJarName(loadedFrom);
        plugin = init();
        loadPluginComponents();
        Plugin.putPlugin(null, plugin);
    }

    /**
     * Fake plugin.
     */
    @Deprecated
    public PluginLoader(boolean fake, URL url) {
        classLoader = getClass().getClassLoader();
        classLoaderForResources = classLoader;
        corePlugin = false;
        initialPlugin = true;
        optionalPlugin = false;

        loadedFrom = url;
        try {
            loadedFromUri = loadedFrom.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to parse uri: " + loadedFrom);
        }
        jarName = getJarName(loadedFrom);
        plugin = null;
    }

    @Nonnull
    private static URL computeCoreUrl() {
        URL from;
        String findBugsClassFile = ClassName.toSlashedClassName(FindBugs.class) + ".class";
        URL me = FindBugs.class.getClassLoader().getResource(findBugsClassFile);
        LOG.debug("FindBugs.class loaded from {}", me);
        if (me == null) {
            throw new IllegalStateException("Failed to load " + findBugsClassFile);
        }
        try {
            String u = me.toString();
            if (u.startsWith("jar:") && u.endsWith("!/" + findBugsClassFile)) {
                u = u.substring(4, u.indexOf("!/"));

                from = new URL(u);

            } else if (u.endsWith(findBugsClassFile)) {
                u = u.substring(0, u.indexOf(findBugsClassFile));
                from = new URL(u);
            } else {
                throw new IllegalArgumentException("Unknown url shema: " + u);
            }

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Failed to parse url: " + me);
        }
        LOG.debug("Core class files loaded from {}", from);
        return from;
    }

    public URL getURL() {
        return loadedFrom;
    }

    public URI getURI() {
        return loadedFromUri;
    }

    private static URI toUri(URL url) throws PluginException {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new PluginException("Bad uri: " + url, e);
        }
    }

    private static String getJarName(URL url) {
        String location = url.getPath();
        int i = location.lastIndexOf('/');
        location = location.substring(i + 1);
        return location;
    }

    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            throw new IllegalStateException("Plugin not completely initialized; classloader not set yet");
        }
        return classLoader;
    }

    /**
     * Get the Plugin.
     *
     * @throws PluginException
     *             if the plugin cannot be fully loaded
     */
    public Plugin loadPlugin() throws PluginException {
        return getPlugin();
    }

    public Plugin getPlugin() {
        if (plugin == null) {
            throw new AssertionError("plugin not already loaded");
        }

        return plugin;
    }

    private static URL resourceFromPlugin(URL u, String args) throws MalformedURLException {
        String path = u.getPath();
        if (path.endsWith(".zip") || path.endsWith(".jar")) {
            return new URL("jar:" + u.toString() + "!/" + args);
        } else if (path.endsWith("/")) {
            return new URL(u.toString() + args);
        } else {
            return new URL(u.toString() + "/" + args);

        }
    }

    /**
     * Get a resource using the URLClassLoader classLoader. We try findResource
     * first because (based on experiment) we can trust it to prefer resources
     * in the jarfile to resources on the filesystem. Simply calling
     * classLoader.getResource() allows the filesystem to override the jarfile,
     * which can mess things up if, for example, there is a findbugs.xml or
     * messages.xml in the current directory.
     *
     * @param name
     *            resource to get
     * @return URL for the resource, or null if it could not be found
     */
    public URL getResource(String name) {
        if (isCorePlugin()) {
            URL url = getCoreResource(name);
            if (url != null && IO.verifyURL(url)) {
                return url;
            }
        }
        if (loadedFrom != null) {
            try {
                URL url = resourceFromPlugin(loadedFrom, name);
                LOG.debug("Trying to load {} from {}", name, url);
                if (IO.verifyURL(url)) {
                    return url;
                }
            } catch (MalformedURLException e) {
                assert true;
            }

        }

        if (classLoaderForResources instanceof URLClassLoader) {

            URLClassLoader urlClassLoader = (URLClassLoader) classLoaderForResources;
            LOG.debug("Trying to load {} using URLClassLoader.findResource", name);
            LOG.debug("  from urls: {}", Arrays.asList(urlClassLoader.getURLs()));
            URL url = urlClassLoader.findResource(name);
            if (url == null) {
                url = urlClassLoader.findResource("/" + name);
            }
            if (IO.verifyURL(url)) {
                return url;
            }
        }

        LOG.debug("Trying to load {} using ClassLoader.getResource", name);
        URL url = classLoaderForResources.getResource(name);
        if (url == null) {
            url = classLoaderForResources.getResource("/" + name);
        }
        if (IO.verifyURL(url)) {
            return url;
        }

        return null;
    }

    static @CheckForNull URL getCoreResource(String name) {
        URL u = loadFromFindBugsPluginDir(name);
        if (u != null) {
            return u;
        }
        u = loadFromFindBugsEtcDir(name);
        if (u != null) {
            return u;
        }
        u = resourceFromFindbugsJar(name);
        if (u != null) {
            return u;
        }
        u = PluginLoader.class.getResource(name);
        if (u != null) {
            return u;
        }
        u = PluginLoader.class.getResource("/" + name);
        return u;
    }

    /**
     * Try to load resource from JAR file of Findbugs core plugin.
     * @param slashedResourceName Name of resource to load
     * @return URL which points resource in jar file, or null if JAR file not found
     */
    @CheckForNull
    private static URL resourceFromFindbugsJar(String slashedResourceName) {
        try {
            @Nullable
            URL findbugsJar = getFindbugsJar();
            if (findbugsJar == null) {
                return null;
            }
            assert findbugsJar.getProtocol().equals("file");
            try (ZipFile jarFile = new ZipFile(new File(findbugsJar.toURI()))) {
                ZipEntry entry = jarFile.getEntry(slashedResourceName);
                if (entry != null) {
                    return resourceFromPlugin(findbugsJar, slashedResourceName);
                }
            } catch (ZipException e) {
                LOG.warn("Failed to load resourceFromFindbugsJar: {} is not valid zip file.", findbugsJar, e);
            } catch (IOException e) {
                LOG.warn("Failed to load resourceFromFindbugsJar: IOException was thrown at zip file {} loading.",
                        findbugsJar, e);
            }
        } catch (MalformedURLException | URISyntaxException e) {
            LOG.warn("Failed to load resourceFromFindbugsJar: Resource name is {}", slashedResourceName, e);
        }
        return null;
    }

    /**
     * @return URL of findbugs.jar,
     * or null if found no jar file which contains FindBugs.class
     * @throws MalformedURLException
     */
    @CheckForNull
    private static URL getFindbugsJar() throws MalformedURLException {
        String findBugsClassFile = ClassName.toSlashedClassName(FindBugs.class) + ".class";
        URL me = FindBugs.class.getClassLoader().getResource(findBugsClassFile);
        if (me == null) {
            return null;
        }
        if (!"jar".equals(me.getProtocol())) {
            return null;
        }
        String u = me.toString();
        String jarPath = u.substring(4, u.indexOf("!/"));
        return new URL(jarPath);
    }

    public static @CheckForNull URL loadFromFindBugsEtcDir(String name) {

        String findBugsHome = DetectorFactoryCollection.getFindBugsHome();
        if (findBugsHome != null) {
            File f = new File(new File(new File(findBugsHome), "etc"), name);
            if (f.canRead()) {
                try {
                    return f.toURL();
                } catch (MalformedURLException e) {
                    // ignore it
                    assert true;
                }
            }
        }
        return null;
    }

    public static @CheckForNull URL loadFromFindBugsPluginDir(String name) {

        String findBugsHome = DetectorFactoryCollection.getFindBugsHome();
        if (findBugsHome != null) {
            File f = new File(new File(new File(findBugsHome), "plugin"), name);
            if (f.canRead()) {
                try {
                    return f.toURI().toURL();
                } catch (MalformedURLException e) {
                    // ignore it
                    assert true;
                }
            }
        }
        return null;
    }

    private static <T> Class<? extends T> getClass(ClassLoader loader, @DottedClassName String className, Class<T> type) throws PluginException {
        try {
            return loader.loadClass(className).asSubclass(type);
        } catch (ClassNotFoundException e) {
            throw new PluginException("Unable to load " + className, e);
        } catch (ClassCastException e) {
            throw new PluginException("Cannot cast " + className + " to " + type.getName(), e);
        }
    }

    private Plugin init() throws PluginException {
        LOG.debug("Loading plugin from {}", loadedFrom);
        // Plugin descriptor (a.k.a, "findbugs.xml"). Defines
        // the bug detectors and bug patterns that the plugin provides.
        Document pluginDescriptor = getPluginDescriptor();
        List<Document> messageCollectionList = getMessageDocuments();

        Plugin constructedPlugin = constructMinimalPlugin(pluginDescriptor, messageCollectionList);

        // Success!
        LOG.debug("Loaded {} from {}", constructedPlugin.getPluginId(), loadedFrom);
        return constructedPlugin;
    }

    private void loadPluginComponents()
            throws PluginException {
        Document pluginDescriptor = getPluginDescriptor();
        List<Document> messageCollectionList = getMessageDocuments();

        // Create PluginComponents
        try {
            List<Node> componentNodeList = XMLUtil.selectNodes(pluginDescriptor, "/FindbugsPlugin/PluginComponent");
            for (Node componentNode : componentNodeList) {
                @DottedClassName
                String componentKindname = componentNode.valueOf("@componentKind");
                if (componentKindname == null) {
                    throw new PluginException("Missing @componentKind for " + plugin.getPluginId()
                            + " loaded from " + loadedFrom);
                }
                @DottedClassName
                String componentClassname = componentNode.valueOf("@componentClass");
                if (componentClassname == null) {
                    throw new PluginException("Missing @componentClassname for " + plugin.getPluginId()
                            + " loaded from " + loadedFrom);
                }
                String componentId = componentNode.valueOf("@id");
                if (componentId == null) {
                    throw new PluginException("Missing @id for " + plugin.getPluginId()
                            + " loaded from " + loadedFrom);
                }

                try {
                    String propertiesLocation = componentNode.valueOf("@properties");
                    boolean disabled = Boolean.valueOf(componentNode.valueOf("@disabled"));

                    Node filterMessageNode = findMessageNode(messageCollectionList,
                            "/MessageCollection/PluginComponent[@id='" + componentId + "']",
                            "Missing Cloud description for PluginComponent " + componentId);
                    String description = getChildText(filterMessageNode, "Description").trim();
                    String details = getChildText(filterMessageNode, "Details").trim();
                    PropertyBundle properties = new PropertyBundle();
                    if (propertiesLocation != null && propertiesLocation.length() > 0) {
                        URL properiesURL = classLoaderForResources.getResource(propertiesLocation);
                        if (properiesURL == null) {
                            AnalysisContext.logError("Could not load properties for " + plugin.getPluginId() + " component " + componentId
                                    + " from " + propertiesLocation);
                            continue;
                        }
                        properties.loadPropertiesFromURL(properiesURL);
                    }
                    List<Node> propertyNodes = XMLUtil.selectNodes(componentNode, "Property");
                    for (Node node : propertyNodes) {
                        String key = node.valueOf("@key");
                        String value = node.getText();
                        properties.setProperty(key, value);
                    }

                    Class<?> componentKind = classLoader.loadClass(componentKindname);
                    loadComponentPlugin(plugin, componentKind, componentClassname, componentId, disabled, description, details,
                            properties);
                } catch (RuntimeException e) {
                    AnalysisContext.logError("Unable to load ComponentPlugin " + componentId +
                            " : " + componentClassname + " implementing " + componentKindname, e);
                }
            }

            // Create FindBugsMains

            if (!FindBugs.isNoMains()) {
                List<Node> findBugsMainList = XMLUtil.selectNodes(pluginDescriptor, "/FindbugsPlugin/FindBugsMain");
                for (Node main : findBugsMainList) {
                    String className = main.valueOf("@class");
                    if (className == null) {
                        throw new PluginException("Missing @class for FindBugsMain in plugin" + plugin.getPluginId()
                                + " loaded from " + loadedFrom);
                    }
                    String cmd = main.valueOf("@cmd");
                    if (cmd == null) {
                        throw new PluginException("Missing @cmd for for FindBugsMain in plugin " + plugin.getPluginId()
                                + " loaded from " + loadedFrom);
                    }
                    String kind = main.valueOf("@kind");
                    boolean analysis = Boolean.valueOf(main.valueOf("@analysis"));
                    Element mainMessageNode = (Element) findMessageNode(messageCollectionList,
                            "/MessageCollection/FindBugsMain[@cmd='" + cmd
                            // + " and @class='" + className
                                    + "']/Description",
                            "Missing FindBugsMain description for cmd " + cmd);
                    String description = mainMessageNode.getTextTrim();
                    try {
                        Class<?> mainClass = classLoader.loadClass(className);
                        plugin.addFindBugsMain(mainClass, cmd, description, kind, analysis);
                    } catch (Exception e) {
                        String msg = "Unable to load FindBugsMain " + cmd +
                                " : " + className + " in plugin " + plugin.getPluginId()
                                + " loaded from " + loadedFrom;
                        PluginException e2 = new PluginException(msg, e);
                        AnalysisContext.logError(msg, e2);
                    }
                }
            }

            List<Node> detectorNodeList = XMLUtil.selectNodes(pluginDescriptor, "/FindbugsPlugin/Detector");
            int detectorCount = 0;
            for (Node detectorNode : detectorNodeList) {
                String className = detectorNode.valueOf("@class");
                String speed = detectorNode.valueOf("@speed");
                String disabled = detectorNode.valueOf("@disabled");
                String reports = detectorNode.valueOf("@reports");
                String requireJRE = detectorNode.valueOf("@requirejre");
                String hidden = detectorNode.valueOf("@hidden");
                if (speed == null || speed.length() == 0) {
                    speed = "fast";
                }
                // System.out.println("Found detector: class="+className+", disabled="+disabled);

                // Create DetectorFactory for the detector
                Class<?> detectorClass = null;
                if (!FindBugs.isNoAnalysis()) {
                    detectorClass = classLoader.loadClass(className);

                    if (!Detector.class.isAssignableFrom(detectorClass) && !Detector2.class.isAssignableFrom(detectorClass)) {
                        throw new PluginException("Class " + className + " does not implement Detector or Detector2");
                    }
                }
                DetectorFactory factory = new DetectorFactory(plugin, className, detectorClass, !"true".equals(disabled), speed,
                        reports, requireJRE);
                if (Boolean.valueOf(hidden).booleanValue()) {
                    factory.setHidden(true);
                }
                factory.setPositionSpecifiedInPluginDescriptor(detectorCount++);
                plugin.addDetectorFactory(factory);

                // Find Detector node in one of the messages files,
                // to get the detail HTML.
                Node node = findMessageNode(messageCollectionList, "/MessageCollection/Detector[@class='" + className
                        + "']/Details", "Missing Detector description for detector " + className);

                Element details = (Element) node;
                String detailHTML = details.getText();
                StringBuilder buf = new StringBuilder();
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
        Node orderingConstraintsNode = pluginDescriptor.selectSingleNode("/FindbugsPlugin/OrderingConstraints");
        if (orderingConstraintsNode != null) {
            // Get inter-pass and intra-pass constraints
            List<Element> elements = XMLUtil.selectNodes(orderingConstraintsNode, "./SplitPass|./WithinPass");
            for (Element constraintElement : elements) {
                // Create the selectors which determine which detectors are
                // involved in the constraint
                DetectorFactorySelector earlierSelector = getConstraintSelector(constraintElement, plugin, "Earlier");
                DetectorFactorySelector laterSelector = getConstraintSelector(constraintElement, plugin, "Later");

                // Create the constraint
                DetectorOrderingConstraint constraint = new DetectorOrderingConstraint(earlierSelector, laterSelector);

                // Keep track of which constraints are single-source
                constraint.setSingleSource(earlierSelector instanceof SingleDetectorFactorySelector);

                // Add the constraint to the plugin
                if ("SplitPass".equals(constraintElement.getName())) {
                    plugin.addInterPassOrderingConstraint(constraint);
                } else {
                    plugin.addIntraPassOrderingConstraint(constraint);
                }
            }
        }

        // register global Category descriptions

        List<Node> categoryNodeListGlobal = XMLUtil.selectNodes(pluginDescriptor, "/FindbugsPlugin/BugCategory");
        for (Node categoryNode : categoryNodeListGlobal) {
            String key = categoryNode.valueOf("@category");
            if ("".equals(key)) {
                throw new PluginException("BugCategory element with missing category attribute");
            }
            BugCategory bc = plugin.addOrCreateBugCategory(key);

            boolean hidden = Boolean.valueOf(categoryNode.valueOf("@hidden"));
            if (hidden) {
                bc.setHidden(hidden);
            }
        }


        for (Document messageCollection : messageCollectionList) {
            List<Node> categoryNodeList = XMLUtil.selectNodes(messageCollection, "/MessageCollection/BugCategory");
            LOG.debug("found {} categories in {}", categoryNodeList.size(), plugin.getPluginId());
            for (Node categoryNode : categoryNodeList) {
                String key = categoryNode.valueOf("@category");
                if ("".equals(key)) {
                    throw new PluginException("BugCategory element with missing category attribute");
                }
                BugCategory bc = plugin.addOrCreateBugCategory(key);
                String shortDesc = getChildText(categoryNode, "Description");
                bc.setShortDescription(shortDesc);
                try {
                    String abbrev = getChildText(categoryNode, "Abbreviation");
                    if (bc.getAbbrev() == null) {
                        bc.setAbbrev(abbrev);
                        LOG.debug("category {} abbrev -> {}", key, abbrev);
                    } else {
                        LOG.debug("rejected abbrev '{}' for category {}: {}", abbrev, key, bc.getAbbrev());
                    }
                } catch (PluginException pe) {
                    System.out.println("missing Abbreviation for category " + key + "/" + shortDesc);
                    // do nothing else -- Abbreviation is required, but handle
                    // its omission gracefully
                }
                try {
                    String details = getChildText(categoryNode, "Details");
                    if (bc.getDetailText() == null) {
                        bc.setDetailText(details);
                        LOG.debug("category {} details -> {}", key, details);
                    } else {
                        LOG.debug("rejected details [{}] for category {}: [{}]", details, key, bc.getDetailText());
                    }
                } catch (PluginException pe) {
                    // do nothing -- LongDescription is optional
                }

            }
        }

        // Create BugPatterns
        List<Node> bugPatternNodeList = XMLUtil.selectNodes(pluginDescriptor, "/FindbugsPlugin/BugPattern");
        for (Node bugPatternNode : bugPatternNodeList) {
            String type = bugPatternNode.valueOf("@type");
            String abbrev = bugPatternNode.valueOf("@abbrev");
            String category = bugPatternNode.valueOf("@category");
            boolean experimental = Boolean.parseBoolean(bugPatternNode.valueOf("@experimental"));

            // Find the matching element in messages.xml (or translations)
            String query = "/MessageCollection/BugPattern[@type='" + type + "']";
            Node messageNode = findMessageNode(messageCollectionList, query, "messages.xml missing BugPattern element for type "
                    + type);
            Node bugsUrlNode = messageNode.getDocument().selectSingleNode("/MessageCollection/Plugin/" + (experimental ? "AllBugsUrl" : "BugsUrl"));

            String bugsUrl = bugsUrlNode == null ? null : bugsUrlNode.getText();

            String shortDesc = getChildText(messageNode, "ShortDescription");
            String longDesc = getChildText(messageNode, "LongDescription");
            String detailText = getChildText(messageNode, "Details");
            int cweid = 0;
            try {
                String cweString = bugPatternNode.valueOf("@cweid");
                if (cweString.length() > 0) {
                    cweid = Integer.parseInt(cweString);
                }
            } catch (RuntimeException e) {
                assert true; // ignore
            }

            BugPattern bugPattern = new BugPattern(type, abbrev, category, experimental, shortDesc, longDesc, detailText, bugsUrl, cweid);

            try {
                String deprecatedStr = bugPatternNode.valueOf("@deprecated");
                boolean deprecated = deprecatedStr.length() > 0 && Boolean.valueOf(deprecatedStr).booleanValue();
                if (deprecated) {
                    bugPattern.setDeprecated(deprecated);
                }
            } catch (RuntimeException e) {
                assert true; // ignore
            }

            plugin.addBugPattern(bugPattern);

        }

        // Create BugCodes
        Set<String> definedBugCodes = new HashSet<>();
        for (Document messageCollection : messageCollectionList) {
            List<Node> bugCodeNodeList = XMLUtil.selectNodes(messageCollection, "/MessageCollection/BugCode");
            for (Node bugCodeNode : bugCodeNodeList) {
                String abbrev = bugCodeNode.valueOf("@abbrev");
                if ("".equals(abbrev)) {
                    throw new PluginException("BugCode element with missing abbrev attribute");
                }
                if (definedBugCodes.contains(abbrev)) {
                    continue;
                }
                String description = bugCodeNode.getText();

                String query = "/FindbugsPlugin/BugCode[@abbrev='" + abbrev + "']";
                Node fbNode = pluginDescriptor.selectSingleNode(query);
                int cweid = 0;
                if (fbNode != null) {
                    try {
                        cweid = Integer.parseInt(fbNode.valueOf("@cweid"));
                    } catch (RuntimeException e) {
                        assert true; // ignore
                    }
                }
                BugCode bugCode = new BugCode(abbrev, description, cweid);
                plugin.addBugCode(bugCode);
                definedBugCodes.add(abbrev);
            }

        }

        // If an engine registrar is specified, make a note of its classname
        Node node = pluginDescriptor.selectSingleNode("/FindbugsPlugin/EngineRegistrar");
        if (node != null) {
            String engineClassName = node.valueOf("@class");
            if (engineClassName == null) {
                throw new PluginException("EngineRegistrar element with missing class attribute");
            }

            try {
                Class<?> engineRegistrarClass = classLoader.loadClass(engineClassName);
                if (!IAnalysisEngineRegistrar.class.isAssignableFrom(engineRegistrarClass)) {
                    throw new PluginException(engineRegistrarClass + " does not implement IAnalysisEngineRegistrar");
                }

                plugin.setEngineRegistrarClass(engineRegistrarClass
                        .<IAnalysisEngineRegistrar>asSubclass(IAnalysisEngineRegistrar.class));
            } catch (ClassNotFoundException e) {
                throw new PluginException("Could not instantiate analysis engine registrar class: " + e, e);
            }

        }
        try {
            URL bugRankURL = getResource(BugRanker.FILENAME);

            if (bugRankURL == null) {
                // see
                // https://sourceforge.net/tracker/?func=detail&aid=2816102&group_id=96405&atid=614693
                // plugin can not have bugrank.txt. In this case, an empty
                // bugranker will be created
                LOG.debug("No {} for plugin {}", BugRanker.FILENAME, plugin.getPluginId());
            }
            BugRanker ranker = new BugRanker(bugRankURL);
            plugin.setBugRanker(ranker);
        } catch (IOException e) {
            throw new PluginException("Couldn't parse \"" + BugRanker.FILENAME + "\"", e);
        }
    }

    private Plugin constructMinimalPlugin(Document pluginDescriptor, List<Document> messageCollectionList)
            throws DuplicatePluginIdError {
        // Get the unique plugin id (or generate one, if none is present)
        // Unique plugin id
        String pluginId = pluginDescriptor.valueOf(XPATH_PLUGIN_PLUGINID);
        if ("".equals(pluginId)) {
            synchronized (PluginLoader.class) {
                pluginId = "plugin" + nextUnknownId++;
            }
        }
        cannotDisable = Boolean.parseBoolean(pluginDescriptor.valueOf("/FindbugsPlugin/@cannotDisable"));

        String de = pluginDescriptor.valueOf("/FindbugsPlugin/@defaultenabled");
        if (de != null && "false".equals(de.toLowerCase().trim())) {
            optionalPlugin = true;
        }
        if (optionalPlugin) {
            cannotDisable = false;
        }
        if (!loadedPluginIds.add(pluginId)) {
            Plugin existingPlugin = Plugin.getByPluginId(pluginId);
            URL u = existingPlugin == null ? null : existingPlugin.getPluginLoader().getURL();
            if (cannotDisable && initialPlugin) {
                throw new DuplicatePluginIdError(pluginId, loadedFrom, u);
            } else {
                throw new DuplicatePluginIdException(pluginId, loadedFrom, u);
            }
        }

        parentId = pluginDescriptor.valueOf("/FindbugsPlugin/@parentid");

        String version = pluginDescriptor.valueOf("/FindbugsPlugin/@version");
        String releaseDate = pluginDescriptor.valueOf("/FindbugsPlugin/@releaseDate");

        // Create the Plugin object (but don't assign to the plugin field yet,
        // since we're still not sure if everything will load correctly)
        Date parsedDate = parseDate(releaseDate);
        Plugin constructedPlugin = new Plugin(pluginId, version, parsedDate, this, !optionalPlugin, cannotDisable);
        // Set provider and website, if specified
        String provider = pluginDescriptor.valueOf(XPATH_PLUGIN_PROVIDER).trim();
        if (!"".equals(provider)) {
            constructedPlugin.setProvider(provider);
        }
        String website = pluginDescriptor.valueOf(XPATH_PLUGIN_WEBSITE).trim();
        if (!"".equals(website)) {
            try {
                constructedPlugin.setWebsite(website);
            } catch (URISyntaxException e1) {
                AnalysisContext.logError("Plugin " + constructedPlugin.getPluginId() + " has invalid website: " + website, e1);
            }
        }

        String updateUrl = pluginDescriptor.valueOf("/FindbugsPlugin/@update-url").trim();
        if (!"".equals(updateUrl)) {
            try {
                constructedPlugin.setUpdateUrl(updateUrl);
            } catch (URISyntaxException e1) {
                AnalysisContext.logError("Plugin " + constructedPlugin.getPluginId() + " has invalid update check URL: " + website, e1);
            }
        }

        // Set short description, if specified
        Node pluginShortDesc = null;
        try {
            pluginShortDesc = findMessageNode(messageCollectionList, XPATH_PLUGIN_SHORT_DESCRIPTION,
                    "no plugin description");
        } catch (PluginException e) {
            // Missing description is not fatal, so ignore
        }
        if (pluginShortDesc != null) {
            constructedPlugin.setShortDescription(pluginShortDesc.getText().trim());
        }
        Node detailedDescription = null;
        try {
            detailedDescription = findMessageNode(messageCollectionList, "/MessageCollection/Plugin/Details",
                    "no plugin description");
        } catch (PluginException e) {
            // Missing description is not fatal, so ignore
        }
        if (detailedDescription != null) {
            constructedPlugin.setDetailedDescription(detailedDescription.getText().trim());
        }
        List<Node> globalOptionNodes = XMLUtil.selectNodes(pluginDescriptor, "/FindbugsPlugin/GlobalOptions/Property");
        for (Node optionNode : globalOptionNodes) {
            String key = optionNode.valueOf("@key");
            String value = optionNode.getText().trim();
            constructedPlugin.setMyGlobalOption(key, value);
        }
        return constructedPlugin;
    }

    public Document getPluginDescriptor() throws PluginException, PluginDoesntContainMetadataException {
        Document pluginDescriptor;

        // Read the plugin descriptor
        String name = "findbugs.xml";
        URL findbugsXML_URL = getResource(name);
        if (findbugsXML_URL == null) {
            throw new PluginException("Couldn't find \"" + name + "\" in plugin " + this);
        }
        LOG.debug("PluginLoader found {} at: {}", name, findbugsXML_URL);

        if (jarName != null && !findbugsXML_URL.toString().contains(jarName)
                && !(corePlugin && findbugsXML_URL.toString().endsWith("etc/findbugs.xml"))) {
            String classloaderName = classLoader.getClass().getName();
            if (classLoader instanceof URLClassLoader) {
                classloaderName += Arrays.asList(((URLClassLoader) classLoader).getURLs());
            }
            throw new PluginDoesntContainMetadataException((corePlugin ? "Core plugin" : "Plugin ") + jarName
                    + " doesn't contain findbugs.xml; got " + findbugsXML_URL + " from " + classloaderName);
        }
        SAXReader reader = XMLUtil.buildSAXReader();

        try (InputStream input = IO.openNonCachedStream(findbugsXML_URL);
                Reader r = UTF8.bufferedReader(input)) {
            pluginDescriptor = reader.read(r);
        } catch (DocumentException e) {
            throw new PluginException("Couldn't parse \"" + findbugsXML_URL + "\" using " + reader.getClass().getName(), e);
        } catch (IOException e) {
            throw new PluginException("Couldn't open \"" + findbugsXML_URL + "\"", e);
        }
        return pluginDescriptor;
    }

    private static List<String> getPotentialMessageFiles() {
        // Load the message collections
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        String country = locale.getCountry();

        List<String> potential = new ArrayList<>(3);
        if (country != null) {
            potential.add("messages_" + language + "_" + country + ".xml");
        }
        potential.add("messages_" + language + ".xml");
        potential.add("messages.xml");
        return potential;
    }

    private List<Document> getMessageDocuments() throws PluginException {
        // List of message translation files in decreasing order of precedence
        ArrayList<Document> messageCollectionList = new ArrayList<>();
        PluginException caught = null;
        for (String m : getPotentialMessageFiles()) {
            try {
                addCollection(messageCollectionList, m);
            } catch (PluginException e) {
                caught = e;
                AnalysisContext.logError(
                        "Error loading localized message file:" + m, e);
            }
        }
        if (messageCollectionList.isEmpty()) {
            if (caught != null) {
                throw caught;
            }
            throw new PluginException("No message.xml files found");
        }
        return messageCollectionList;
    }


    private <T> void loadComponentPlugin(Plugin plugin,
            Class<T> componentKind, @DottedClassName String componentClassname, String filterId,
            boolean disabled, String description, String details, PropertyBundle properties) throws PluginException {
        Class<? extends T> componentClass = null;
        if (!FindBugs.isNoAnalysis() || componentKind == edu.umd.cs.findbugs.bugReporter.BugReporterDecorator.class) {
            componentClass = getClass(classLoader, componentClassname, componentKind);
        }

        ComponentPlugin<T> componentPlugin = new ComponentPlugin<>(plugin, filterId, classLoader, componentClass,
                properties, !disabled, description, details);
        plugin.addComponentPlugin(componentKind, componentPlugin);
    }

    private static Date parseDate(String releaseDate) {
        if (releaseDate == null || releaseDate.length() == 0) {
            return null;
        }
        try {
            SimpleDateFormat releaseDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm aa z", Locale.ENGLISH);
            Date result = releaseDateFormat.parse(releaseDate);
            return result;
        } catch (ParseException e) {
            AnalysisContext.logError("unable to parse date " + releaseDate, e);
            return null;
        }
    }

    private DetectorFactorySelector getConstraintSelector(Element constraintElement, Plugin plugin,
            String singleDetectorElementName/*
                                            * , String
                                            * detectorCategoryElementName
                                            */) throws PluginException {
        Node node = constraintElement.selectSingleNode("./" + singleDetectorElementName);
        if (node != null) {
            String detectorClass = node.valueOf("@class");
            return new SingleDetectorFactorySelector(plugin, detectorClass);
        }

        node = constraintElement.selectSingleNode("./" + singleDetectorElementName + "Category");
        if (node != null) {
            boolean spanPlugins = Boolean.valueOf(node.valueOf("@spanplugins")).booleanValue();

            String categoryName = node.valueOf("@name");
            if (!"".equals(categoryName)) {
                if ("reporting".equals(categoryName)) {
                    return new ReportingDetectorFactorySelector(spanPlugins ? null : plugin);
                } else if ("training".equals(categoryName)) {
                    return new ByInterfaceDetectorFactorySelector(spanPlugins ? null : plugin, TrainingDetector.class);
                } else if ("interprocedural".equals(categoryName)) {
                    return new ByInterfaceDetectorFactorySelector(spanPlugins ? null : plugin,
                            InterproceduralFirstPassDetector.class);
                } else {
                    throw new PluginException("Invalid category name " + categoryName + " in constraint selector node");
                }
            }
        }

        node = constraintElement.selectSingleNode("./" + singleDetectorElementName + "Subtypes");
        if (node != null) {
            boolean spanPlugins = Boolean.valueOf(node.valueOf("@spanplugins")).booleanValue();

            String superName = node.valueOf("@super");
            if (!"".equals(superName)) {
                try {
                    Class<?> superClass = Class.forName(superName, true, classLoader);
                    return new ByInterfaceDetectorFactorySelector(spanPlugins ? null : plugin, superClass);
                } catch (ClassNotFoundException e) {
                    throw new PluginException("Unknown class " + superName + " in constraint selector node");
                }
            }
        }
        throw new PluginException("Invalid constraint selector node");
    }

    private void addCollection(List<Document> messageCollectionList, String filename) throws PluginException {
        URL messageURL = getResource(filename);
        if (messageURL != null) {
            SAXReader reader = XMLUtil.buildSAXReader();
            try (InputStream input = IO.openNonCachedStream(messageURL);
                    Reader stream = UTF8.bufferedReader(input)) {
                Document messageCollection;
                messageCollection = reader.read(stream);
                messageCollectionList.add(messageCollection);
            } catch (IOException | DocumentException e) {
                throw new PluginException("Couldn't parse \"" + messageURL + "\"", e);
            }
        }
    }

    private static Node findMessageNode(List<Document> messageCollectionList, String xpath, String missingMsg)
            throws PluginException {
        for (Document document : messageCollectionList) {
            Node node = document.selectSingleNode(xpath);
            if (node != null) {
                return node;
            }
        }
        throw new PluginException(missingMsg);
    }

    private static String findMessageText(List<Document> messageCollectionList, String xpath, String missingMsg) {
        for (Document document : messageCollectionList) {
            Node node = document.selectSingleNode(xpath);
            if (node != null) {
                return node.getText().trim();
            }
        }
        return missingMsg;
    }

    private static String getChildText(Node node, String childName) throws PluginException {
        Node child = node.selectSingleNode(childName);
        if (child == null) {
            throw new PluginException("Could not find child \"" + childName + "\" for node");
        }
        return child.getText();
    }

    public static PluginLoader getPluginLoader(URL url, ClassLoader parent, boolean isInitial, boolean optional) throws PluginException {
        URI uri = toUri(url);
        Plugin plugin = Plugin.getPlugin(uri);
        if (plugin != null) {
            PluginLoader loader = plugin.getPluginLoader();
            assert loader.getClassLoader().getParent().equals(parent);
            return loader;
        }
        return new PluginLoader(url, uri, parent, isInitial, optional);
    }

    @Nonnull
    public static synchronized PluginLoader getCorePluginLoader() {
        Plugin plugin = Plugin.getPlugin(null);
        if (plugin != null) {
            return plugin.getPluginLoader();
        }
        throw new IllegalStateException("Core plugin not loaded yet!");
    }

    public boolean isCorePlugin() {
        return corePlugin;
    }

    static void installStandardPlugins() {
        String homeDir = DetectorFactoryCollection.getFindBugsHome();
        if (homeDir == null) {
            return;
        }
        File home = new File(homeDir);
        loadPlugins(home);
    }

    private static void loadPlugins(File home) {
        if (home.canRead() && home.isDirectory()) {
            loadPluginsInDir(new File(home, "plugin"), false);
            loadPluginsInDir(new File(home, "optionalPlugin"), true);
        }
    }

    static void installUserInstalledPlugins() {
        String homeDir = System.getProperty("user.home");
        if (homeDir == null) {
            return;
        }
        File homeFindBugs = new File(new File(homeDir), ".findbugs");
        loadPlugins(homeFindBugs);
    }

    private static void loadPluginsInDir(File pluginDir, boolean optional) {
        File[] contentList = pluginDir.listFiles();
        if (contentList == null) {
            return;
        }

        for (File file : contentList) {
            if (file.getName().endsWith(".jar")) {
                try {
                    URL url = file.toURI().toURL();
                    if (IO.verifyURL(url)) {
                        loadInitialPlugin(url, true, optional);
                        if (FindBugs.DEBUG) {
                            System.out.println("Found plugin: " + file.toString());
                        }
                    }
                } catch (MalformedURLException e) {

                }
            }
        }
    }

    static synchronized void loadInitialPlugins() {
        lazyInitialization = true;
        loadCorePlugin();
        if (JavaWebStart.isRunningViaJavaWebstart()) {
            installWebStartPlugins();
        } else {
            installStandardPlugins();
            installUserInstalledPlugins();
        }
        Set<Entry<Object, Object>> entrySet = SystemProperties.getAllProperties().entrySet();
        for (Map.Entry<?, ?> e : entrySet) {
            if (e.getKey() instanceof String && e.getValue() instanceof String
                    && ((String) e.getKey()).startsWith("findbugs.plugin.")) {
                try {
                    String value = (String) e.getValue();
                    if (value.startsWith("file:") && !value.endsWith(".jar") && !value.endsWith("/")) {
                        value += "/";
                    }
                    URL url = JavaWebStart.resolveRelativeToJnlpCodebase(value);
                    System.out.println("Loading " + e.getKey() + " from " + url);
                    loadInitialPlugin(url, true, false);
                } catch (MalformedURLException e1) {
                    AnalysisContext.logError(String.format("Bad URL for plugin: %s=%s", e.getKey(), e.getValue()), e1);
                }
            }
        }

        if (Plugin.getAllPlugins().size() > 1 && JavaWebStart.isRunningViaJavaWebstart()) {
            // disable security manager; plugins cause problems
            // http://lopica.sourceforge.net/faq.html
            // URL policyUrl =
            // Thread.currentThread().getContextClassLoader().getResource("my.java.policy");
            // Policy.getPolicy().refresh();
            try {
                SecurityManagerHandler.disableSecurityManager();
            } catch (Throwable e) {
                assert true; // keep going
            }
        }
        finishLazyInitialization();
    }

    private static void loadCorePlugin() {
        try {
            Plugin plugin = Plugin.getPlugin(null);
            if (plugin != null) {
                throw new IllegalStateException("Already loaded");
            }
            PluginLoader pluginLoader = new PluginLoader();
            plugin = pluginLoader.getPlugin();
            Plugin.putPlugin(null, plugin);
        } catch (PluginException e1) {
            throw new IllegalStateException("Unable to load core plugin", e1);
        }
    }

    private static void loadInitialPlugin(URL u, boolean initial, boolean optional) {
        try {
            getPluginLoader(u, PluginLoader.class.getClassLoader(), initial, optional);
        } catch (DuplicatePluginIdException ignored) {
            assert true;
        } catch (PluginException e) {
            AnalysisContext.logError("Unable to load plugin from " + u, e);
            LOG.debug("Unable to load plugin from {}", u, e);
        }
    }

    static void installWebStartPlugins() {
        URL pluginListProperties = getCoreResource("pluginlist.properties");
        BufferedReader in = null;
        if (pluginListProperties != null) {
            try {
                DetectorFactoryCollection.jawsDebugMessage(pluginListProperties.toString());
                URL base = getUrlBase(pluginListProperties);

                in = UTF8.bufferedReader(pluginListProperties.openStream());
                while (true) {
                    String plugin = in.readLine();

                    if (plugin == null) {
                        break;
                    }
                    URL url = new URL(base, plugin);
                    try {
                        URLConnection connection = url.openConnection();
                        String contentType = connection.getContentType();
                        DetectorFactoryCollection.jawsDebugMessage("contentType : " + contentType);
                        if (connection instanceof HttpURLConnection) {
                            ((HttpURLConnection) connection).disconnect();
                        }
                        loadInitialPlugin(url, true, false);
                    } catch (IOException e) {
                        DetectorFactoryCollection.jawsDebugMessage("error loading " + url + " : " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                DetectorFactoryCollection.jawsDebugMessage("error : " + e.getMessage());
            } finally {
                Util.closeSilently(in);
            }
        }
    }

    private static URL getUrlBase(URL pluginListProperties) throws MalformedURLException {
        String urlname = pluginListProperties.toString();
        URL base = pluginListProperties;
        int pos = urlname.indexOf("!/");
        if (pos >= 0 && urlname.startsWith("jar:")) {
            urlname = urlname.substring(4, pos);
            base = new URL(urlname);
        }
        return base;
    }

    @Override
    public String toString() {
        if (plugin == null) {
            return String.format("PluginLoader(%s)", loadedFrom);
        }
        return String.format("PluginLoader(%s, %s)", plugin.getPluginId(), loadedFrom);
    }

    static public class Summary {
        public final String id;
        public final String description;
        public final String provider;
        public final String webbsite;

        public Summary(String id, String description, String provider, String website) {
            super();
            this.id = id;
            this.description = description;
            this.provider = provider;
            this.webbsite = website;
        }
    }

    public static Summary validate(File file) throws IllegalArgumentException {
        String path = file.getPath();
        if (!file.getName().endsWith(".jar")) {
            String message = "File " + path + " is not a .jar file";
            throw new IllegalArgumentException(message);
        }
        if (!file.isFile() || !file.canRead()) {
            String message = "File " + path
                    + " is not a file or is not readable";
            throw new IllegalArgumentException(message);
        }
        if (file.length() == 0) {
            String message = "File " + path + " is empty";
            throw new IllegalArgumentException(message);
        }

        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry findbugsXML = zip.getEntry("findbugs.xml");
            if (findbugsXML == null) {
                throw new IllegalArgumentException(
                        "plugin doesn't contain a findbugs.xml file");
            }
            ZipEntry messagesXML = zip.getEntry("messages.xml");
            if (messagesXML == null) {
                throw new IllegalArgumentException(
                        "plugin doesn't contain a messages.xml file");
            }
            Document pluginDocument = parseDocument(zip.getInputStream(findbugsXML));
            String pluginId = pluginDocument.valueOf(XPATH_PLUGIN_PLUGINID).trim();
            String provider = pluginDocument.valueOf(XPATH_PLUGIN_PROVIDER).trim();
            String website = pluginDocument.valueOf(XPATH_PLUGIN_WEBSITE).trim();
            List<Document> msgDocuments = new ArrayList<>(3);
            for (String msgFile : getPotentialMessageFiles()) {
                ZipEntry msgEntry = zip.getEntry(msgFile);
                if (msgEntry == null) {
                    continue;
                }
                Document msgDocument = parseDocument(zip.getInputStream(msgEntry));
                msgDocuments.add(msgDocument);
            }
            String shortDesc = findMessageText(msgDocuments,
                    XPATH_PLUGIN_SHORT_DESCRIPTION, "");
            return new Summary(pluginId, shortDesc, provider, website);
        } catch (DocumentException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Document parseDocument(@WillClose InputStream in) throws DocumentException {
        Reader r = UTF8.bufferedReader(in);
        try {
            SAXReader reader = XMLUtil.buildSAXReader();
            Document d = reader.read(r);
            return d;
        } finally {
            Util.closeSilently(r);
        }
    }
}
