/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
 * Copyright (C) 2004-2005, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package de.tobject.findbugs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.CheckForNull;

import org.dom4j.DocumentException;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;

import de.tobject.findbugs.builder.FindBugsBuilder;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.io.FileOutput;
import de.tobject.findbugs.io.IO;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.nature.FindBugsNature;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.preferences.FindBugsPreferenceInitializer;
import de.tobject.findbugs.properties.DetectorProvider;
import de.tobject.findbugs.properties.DetectorValidator;
import de.tobject.findbugs.properties.DetectorValidator.ValidationStatus;
import de.tobject.findbugs.reporter.Reporter;
import de.tobject.findbugs.view.IMarkerSelectionHandler;
import de.tobject.findbugs.view.explorer.BugContentProvider;
import edu.umd.cs.findbugs.BugCode;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.plugins.DuplicatePluginIdException;


/**
 * The main plugin class to be used in the desktop.
 */
public class FindbugsPlugin extends AbstractUIPlugin {
    /**
     * The plug-in identifier of the FindBugs Plug-in (value
     * "edu.umd.cs.findbugs.plugin.eclipse", was
     * <code>"de.tobject.findbugs"</code>).
     */
    public static final String PLUGIN_ID = "edu.umd.cs.findbugs.plugin.eclipse"; //$NON-NLS-1$

    private static final String DEFAULT_CLOUD_ID = "edu.umd.cs.findbugs.cloud.doNothingCloud";

    public static final String ICON_PATH = "icons/";
    public static final String ICON_DEFAULT = "buggy-tiny-gray.png";

    @SuppressWarnings("restriction")
    private static final IPath WORKSPACE_PREFS_PATH = Platform.getStateLocation(Platform.getBundle(Platform.PI_RUNTIME))
            .append(EclipsePreferences.DEFAULT_PREFERENCES_DIRNAME)
            .append(PLUGIN_ID + "." + EclipsePreferences.PREFS_FILE_EXTENSION);

    @java.lang.SuppressWarnings("restriction")
    public static final IPath DEFAULT_PREFS_PATH = new Path(EclipsePreferences.DEFAULT_PREFERENCES_DIRNAME)
    .append("edu.umd.cs.findbugs.core.prefs");

    public static final IPath DEPRECATED_PREFS_PATH = new Path(".fbprefs");

    public static final String DETAILS_VIEW_ID = "de.tobject.findbugs.view.buginfoview";

    public static final String USER_ANNOTATIONS_VIEW_ID = "de.tobject.findbugs.view.userannotationsview";

    public static final String TREE_VIEW_ID = "de.tobject.findbugs.view.bugtreeview";

    public static final String BUG_CONTENT_PROVIDER_ID = "de.tobject.findbugs.view.explorer.BugContentProvider";

    /** Map containing preloaded ImageDescriptors */
    private final Map<String, ImageDescriptor> imageDescriptors = new HashMap<String, ImageDescriptor>(13);

    /** Controls debugging of the plugin */
    public static boolean DEBUG;


    /**
     * The identifier for the FindBugs builder (value
     * <code>"edu.umd.cs.findbugs.plugin.eclipse.findbugsbuilder"</code>).
     */
    public static final String BUILDER_ID = PLUGIN_ID + ".findbugsBuilder"; //$NON-NLS-1$

    /**
     * The identifier for the FindBugs nature (value
     * <code>"edu.umd.cs.findbugs.plugin.eclipse.findbugsnature"</code>).
     *
     * @see org.eclipse.core.resources.IProject#hasNature(java.lang.String)
     */
    public static final String NATURE_ID = PLUGIN_ID + ".findbugsNature"; //$NON-NLS-1$

    // Debugging options
    private static final String PLUGIN_DEBUG = PLUGIN_ID + "/debug"; //$NON-NLS-1$

    private static final String BUILDER_DEBUG = PLUGIN_ID + "/debug/builder"; //$NON-NLS-1$

    private static final String NATURE_DEBUG = PLUGIN_ID + "/debug/nature"; //$NON-NLS-1$

    private static final String REPORTER_DEBUG = PLUGIN_ID + "/debug/reporter"; //$NON-NLS-1$

    private static final String CONTENT_DEBUG = PLUGIN_ID + "/debug/content"; //$NON-NLS-1$

    private static final String PROFILER_DEBUG = PLUGIN_ID + "/debug/profiler"; //$NON-NLS-1$

    // Persistent and session property keys
    public static final QualifiedName SESSION_PROPERTY_BUG_COLLECTION = new QualifiedName(FindbugsPlugin.PLUGIN_ID
            + ".sessionprops", "bugcollection");

    public static final QualifiedName SESSION_PROPERTY_BUG_COLLECTION_DIRTY = new QualifiedName(FindbugsPlugin.PLUGIN_ID
            + ".sessionprops", "bugcollection.dirty");

    public static final QualifiedName SESSION_PROPERTY_USERPREFS = new QualifiedName(FindbugsPlugin.PLUGIN_ID + ".sessionprops",
            "userprefs");

    public static final QualifiedName SESSION_PROPERTY_SETTINGS_ON = new QualifiedName(
            FindbugsPlugin.PLUGIN_ID + ".sessionprops", "settingsOn");

    public static final String LIST_DELIMITER = ";"; //$NON-NLS-1$

    /** The shared instance. */
    private static FindbugsPlugin plugin;

    private static boolean customDetectorsInitialized;

    /** Resource bundle. */
    private ResourceBundle resourceBundle;

    /**
     * Constructor.
     */
    public FindbugsPlugin() {
        plugin = this;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        Version.registerApplication("FindBugs-Eclipse", Version.RELEASE);

         // configure debugging
        configurePluginDebugOptions();

        // initialize resource strings
        try {
            resourceBundle = ResourceBundle.getBundle("de.tobject.findbugs.messages"); //this is correct //$NON-NLS-1$
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }

        if (System.getProperty("findbugs.home") == null) {
            // TODO workaround for findbugs home property
            // - see de.tobject.findbugs.builder.FindBugsWorker.work() too
            String findBugsHome = getFindBugsEnginePluginLocation();
            if (DEBUG) {
                logInfo("FindBugs home is: " + findBugsHome);
            }
            System.setProperty("findbugs.home", findBugsHome);
        }
        if (System.getProperty("findbugs.cloud.default") == null) {
            // TODO workaround for findbugs default cloud property
            // - see edu.umd.cs.findbugs.cloud.CloudFactory and messages.xml
            String defCloud = DEFAULT_CLOUD_ID;
            if (DEBUG) {
                logInfo("Using default cloud: " + defCloud);
            }
            System.setProperty("findbugs.cloud.default", defCloud);
        }

        /** Don't load main classes */
        FindBugs.setNoMains();

        // enable source searching for "fall" (ignore case) in switch statements
        SystemProperties.setProperty("findbugs.sf.comment", "true");

        // Register our save participant
        FindbugsSaveParticipant saveParticipant = new FindbugsSaveParticipant();
        ResourcesPlugin.getWorkspace().addSaveParticipant(PLUGIN_ID, saveParticipant);
    }

    public static void dumpClassLoader(Class<?> c) {
        System.out.printf("Class loaders for %s:%n", c.getName());
        ClassLoader loader = c.getClassLoader();
        while (loader != null) {
            System.out.printf("  %s %s%n", loader.toString(),  loader.getClass().getSimpleName());
            loader = loader.getParent();
        }

    }

    /**
     * The complexity of the code below is partly caused by the fact that we
     * might have multiple ways to install and/or enable custom plugins. There
     * are plugins discovered by FB itself, plugins contributed to Eclipse and
     * plugins added by user manually via properties. Plugins can be disabled
     * via code or properties. The code below is still work in progress, see
     * also {@link DetectorProvider#getPluginElements(UserPreferences)}.
     *
     * @param detectorPaths
     *            list of possible detector plugins
     * @param force
     *            true if we MUST set plugins even if the given list is empty
     */
    public static synchronized void applyCustomDetectors(boolean force) {
        if(customDetectorsInitialized && !force) {
            return;
        }
        customDetectorsInitialized = true;
        DetectorValidator validator = new DetectorValidator();
        final SortedSet<String> detectorPaths = new TreeSet<String>();
        SortedMap<String, String> contributedDetectors = DetectorsExtensionHelper.getContributedDetectors();
        UserPreferences corePreferences = getCorePreferences(null, force);
        detectorPaths.addAll(corePreferences.getCustomPlugins(true));
        if(DEBUG) {
            dumpClassLoader(FindbugsPlugin.class);
            dumpClassLoader(Plugin.class);
            System.out.println("applyCustomDetectors - going to add " + detectorPaths.size() + " plugin urls...");
            for (String url : detectorPaths) {
                System.out.println("\t" + url);
            }
        }

        // disable custom plugins configured via properties, if they are already loaded
        Set<String> disabledPlugins = corePreferences.getCustomPlugins(false);
        Map<URI, Plugin> allPlugins = Plugin.getAllPluginsMap();
        for (Entry<URI, Plugin> entry : allPlugins.entrySet()) {
            Plugin fbPlugin = entry.getValue();
            String pluginId = fbPlugin.getPluginId();
            // ignore all custom plugins with the same plugin id as already loaded
            if(contributedDetectors.containsKey(pluginId)) {
                contributedDetectors.remove(pluginId);
                detectorPaths.remove(pluginId);
            }

            if (fbPlugin.isCorePlugin() || fbPlugin.isInitialPlugin()) {
                continue;
            }
            if (disabledPlugins.contains(entry.getKey().getPath())
                    || disabledPlugins.contains(pluginId)) {
                fbPlugin.setGloballyEnabled(false);
                Plugin.removeCustomPlugin(fbPlugin);
                if (DEBUG) {
                    System.out.println("Removed plugin: " + fbPlugin + " loaded from " + entry.getKey());
                }
            }
        }

        HashSet<Plugin> enabled = new HashSet<Plugin>();

        // adding FindBugs *Eclipse* plugins, key plugin id, value is path
        for (Entry<String, String> entry : contributedDetectors.entrySet()) {
            String pluginId = entry.getKey();
            String pluginPath = entry.getValue();
            URI uri = new File(pluginPath).toURI();
            if (disabledPlugins.contains(pluginId)
                    || disabledPlugins.contains(pluginPath)
                    || allPlugins.containsKey(uri)) {
                continue;
            }
            addCustomPlugin(enabled, uri);
        }

        // adding custom plugins configured via properties, but only if they are not loaded yet
        for (String path : detectorPaths) {
            // this is plugin id, so we can't use it as URL
            if(new Path(path).segmentCount() == 1) {
                continue;
            }
            path = FindBugsWorker.getFilterPath(path, null).toOSString();
            URI uri = new File(path).toURI();
            if(allPlugins.containsKey(uri)) {
                continue;
            }
            ValidationStatus status = validator.validate(path);
            if (status.isOK()) {
                addCustomPlugin(enabled, uri);
            } else {
                getDefault().getLog().log(status);
            }
        }

        if (DEBUG) {
            System.out.println("applyCustomDetectors - there was " + detectorPaths.size()
                    + " extra FB plugin urls with " + enabled.size()
                    + " valid FB plugins and " + allPlugins.size()
                    + " total plugins registered by FB.");
            for (Entry<URI, Plugin> entry : allPlugins.entrySet()) {
                Plugin fbPlugin = entry.getValue();
                if (fbPlugin.isGloballyEnabled()) {
                    System.out.println("IS  enabled:\t" + fbPlugin.getPluginId());
                } else {
                    System.out.println("NOT enabled:\t" + fbPlugin.getPluginId());
                }
            }
        }
    }

    protected static void addCustomPlugin(HashSet<Plugin> enabled, URI uri) {
        try {
            // bug 3117769 - we must provide our own classloader
            // to allow third-party plugins extend the classpath via
            // "Buddy" classloading
            // see also: Eclipse-BuddyPolicy attribute in MANIFEST.MF
            Plugin fbPlugin = Plugin.addCustomPlugin(uri, FindbugsPlugin.class.getClassLoader());
            if(fbPlugin != null) {
                // TODO line below required to enable this *optional* plugin
                // but it should be taken by FB core from the findbugs.xml,
                // which currently only works for *core* plugins only
                fbPlugin.setGloballyEnabled(true);
                enabled.add(fbPlugin);
            }
        } catch (PluginException e) {
            getDefault().logException(e, "Failed to load plugin for custom detector: " + uri);
        } catch (DuplicatePluginIdException e) {
            getDefault().logException(e, e.getPluginId() + " already loaded from " + e.getPreviouslyLoadedFrom()
                    + ", ignoring: " + uri);
        }
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry reg) {
        for (FindBugsMarker.MarkerRank prio : FindBugsMarker.MarkerRank.values()) {
            String iconName = prio.iconName();
            registerIcon(reg, iconName);
        }
        for (FindBugsMarker.MarkerConfidence prio : FindBugsMarker.MarkerConfidence.values()) {
            String iconName = prio.iconName();
            registerIcon(reg, iconName);
        }
        registerIcon(reg, ICON_DEFAULT);
    }

    private void registerIcon(ImageRegistry reg, String iconName) {
        ImageDescriptor descriptor = getImageDescriptor(iconName);
        if (descriptor != null) {
            reg.put(iconName, descriptor);
        }
    }

    /**
     * Returns the shared instance.
     */
    public static FindbugsPlugin getDefault() {
        return plugin;
    }

    /**
     * @return active window instance, never null
     */
    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        if (Display.getCurrent() != null) {
            return getDefault().getWorkbench().getActiveWorkbenchWindow();
        }
        // need to call from UI thread
        final IWorkbenchWindow[] window = new IWorkbenchWindow[1];
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                window[0] = getDefault().getWorkbench().getActiveWorkbenchWindow();
            }
        });
        return window[0];
    }

    /**
     * Returns the SWT Shell of the active workbench window or <code>null</code>
     * if no workbench window is active.
     *
     * @return the SWT Shell of the active workbench window, or
     *         <code>null</code> if no workbench window is active
     */
    public static Shell getShell() {
        IWorkbenchWindow window = getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }
        return window.getShell();
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not
     * found.
     */
    public static String getResourceString(String key) {
        ResourceBundle bundle = FindbugsPlugin.getDefault().getResourceBundle();
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the plugin's resource bundle,
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    public void configurePluginDebugOptions() {
        if (isDebugging()) {
            // debugging for the plugin itself
            String option = Platform.getDebugOption(PLUGIN_DEBUG);
            FindbugsPlugin.DEBUG = Boolean.valueOf(option).booleanValue();

            // debugging for the builder and friends
            option = Platform.getDebugOption(BUILDER_DEBUG);
            FindBugsBuilder.DEBUG = Boolean.valueOf(option).booleanValue();
            FindBugsWorker.DEBUG = FindBugsBuilder.DEBUG;

            // debugging for the nature
            option = Platform.getDebugOption(NATURE_DEBUG);
            FindBugsNature.DEBUG = Boolean.valueOf(option).booleanValue();

            // debugging for the reporter
            option = Platform.getDebugOption(REPORTER_DEBUG);
            Reporter.DEBUG = Boolean.valueOf(option).booleanValue();

            // debugging for the content provider
            option = Platform.getDebugOption(CONTENT_DEBUG);
            BugContentProvider.DEBUG = Boolean.valueOf(option).booleanValue();

            option = Platform.getDebugOption(PROFILER_DEBUG);
            if (Boolean.valueOf(option).booleanValue()) {
                System.setProperty("profiler.report", "true");
            }
        }
    }

    /**
     * Find the filesystem path of the FindBugs plugin directory.
     *
     * @return the filesystem path of the FindBugs plugin directory, or null if
     *         the FindBugs plugin directory cannot be found
     */
    public static String getFindBugsEnginePluginLocation() {
        // findbugs.home should be set to the directory the plugin is
        // installed in.
        URL u = plugin.getBundle().getEntry("/");
        try {
            URL bundleRoot = FileLocator.resolve(u);
            String path = bundleRoot.getPath();
            if (FindBugsBuilder.DEBUG) {
                System.out.println("Pluginpath: " + path); //$NON-NLS-1$
            }
            if (path.endsWith("/eclipsePlugin/")) {
                File f = new File(path);
                f = f.getParentFile();
                f = new File(f, "findbugs");
                path = f.getPath() + "/";
            }

            return path;
        } catch (IOException e) {
            FindbugsPlugin.getDefault().logException(e, "IO Exception locating engine plugin");
        }
        return null;
    }

    /**
     * @param key
     * @return
     */
    public String getMessage(String key) {
        return getResourceString(key);
    }

    /**
     * Log an exception.
     *
     * @param e
     *            the exception
     * @param message
     *            message describing how/why the exception occurred
     */
    public void logException(Throwable e, String message) {
        logMessage(IStatus.ERROR, message, e);
    }

    /**
     * Log an error.
     *
     * @param message
     *            error message
     */
    public void logError(String message) {
        logMessage(IStatus.ERROR, message, null);
    }

    /**
     * Log a warning.
     *
     * @param message
     *            warning message
     */
    public void logWarning(String message) {
        logMessage(IStatus.WARNING, message, null);
    }

    /**
     * Log an informational message.
     *
     * @param message
     *            the informational message
     */
    public void logInfo(String message) {
        logMessage(IStatus.INFO, message, null);
    }

    public void logMessage(int severity, String message, Throwable e) {
        if (DEBUG) {
            String what = (severity == IStatus.ERROR) ? (e != null ? "Exception" : "Error") : "Warning";
            System.out.println(what + " in FindBugs plugin: " + message);
            if (e != null) {
                e.printStackTrace();
            }
        }
        IStatus status = createStatus(severity, message, e);
        getLog().log(status);
    }

    public static IStatus createStatus(int severity, String message, Throwable e) {
        return new Status(severity, FindbugsPlugin.PLUGIN_ID, 0, message, e);
    }

    public static IStatus createErrorStatus(String message, Throwable e) {
        return new Status(IStatus.ERROR, FindbugsPlugin.PLUGIN_ID, 0, message, e);
    }

    /**
     * Get the file resource used to store findbugs warnings for a project.
     *
     * @param project
     *            the project
     * @return the IPath to the file (which may not actually exist in the
     *         filesystem yet)
     */
    public static IPath getBugCollectionFile(IProject project) {
        // IPath path = project.getWorkingLocation(PLUGIN_ID); //
        // project-specific but not user-specific?
        IPath path = getDefault().getStateLocation(); // user-specific but not
        // project-specific
        return path.append(project.getName() + ".fbwarnings.xml");
    }

    private static boolean isBugCollectionDirty(IProject project) throws CoreException {
        Object dirty = project.getSessionProperty(SESSION_PROPERTY_BUG_COLLECTION_DIRTY);

        if (dirty == null) {
            return false;
        }
        return ((Boolean) dirty).booleanValue();
    }

    public static void markBugCollectionDirty(IProject project, boolean isDirty) throws CoreException {
        project.setSessionProperty(SESSION_PROPERTY_BUG_COLLECTION_DIRTY, isDirty ? Boolean.TRUE : Boolean.FALSE);
    }

    @CheckForNull
    public static SortedBugCollection getBugCollectionIfSet(IProject project) {
        try {
            return (SortedBugCollection) project.getSessionProperty(SESSION_PROPERTY_BUG_COLLECTION);
        } catch (CoreException ignored) {
            FindbugsPlugin.getDefault().logException(ignored, "IO Exception reading project bugs.");
            return null;
        }
    }

    public static SortedBugCollection getBugCollection(IProject project, IProgressMonitor monitor) throws CoreException {
        return getBugCollection(project, monitor, true);
    }

    /**
     * Get the stored BugCollection for project. If there is no stored bug
     * collection for the project, or if an error occurs reading the stored bug
     * collection, a default empty collection is created and returned.
     *
     * @param project
     *            the eclipse project
     * @param monitor
     *            a progress monitor
     * @return the stored BugCollection, never null
     * @throws CoreException
     */
    public static SortedBugCollection getBugCollection(IProject project, IProgressMonitor monitor, boolean useCloud)
            throws CoreException {
        SortedBugCollection bugCollection = (SortedBugCollection) project.getSessionProperty(SESSION_PROPERTY_BUG_COLLECTION);
        if (bugCollection == null) {
            try {
                readBugCollectionAndProject(project, monitor, useCloud);
                bugCollection = (SortedBugCollection) project.getSessionProperty(SESSION_PROPERTY_BUG_COLLECTION);
            } catch (IOException e) {
                FindbugsPlugin.getDefault().logException(e, "Could not read bug collection for project");
                bugCollection = createDefaultEmptyBugCollection(project);
            } catch (DocumentException e) {
                FindbugsPlugin.getDefault().logException(e, "Could not read bug collection for project");
                bugCollection = createDefaultEmptyBugCollection(project);
            }
        }
        return bugCollection;
    }

    private static void cacheBugCollectionAndProject(IProject project, SortedBugCollection bugCollection, Project fbProject)
            throws CoreException {
        project.setSessionProperty(SESSION_PROPERTY_BUG_COLLECTION, bugCollection);
        markBugCollectionDirty(project, false);
    }

    private static SortedBugCollection createDefaultEmptyBugCollection(IProject project) throws CoreException {
        SortedBugCollection bugCollection = new SortedBugCollection();
        Project fbProject = bugCollection.getProject();

        UserPreferences userPrefs = getUserPreferences(project);

        String cloudId = userPrefs.getCloudId();
        if (cloudId != null) {
            fbProject.setCloudId(cloudId);
        }
        cacheBugCollectionAndProject(project, bugCollection, fbProject);
        return bugCollection;
    }

    /**
     * Read saved bug collection and findbugs project from file. Will populate
     * the bug collection and findbugs project session properties if successful.
     * If there is no saved bug collection and project for the eclipse project,
     * then FileNotFoundException will be thrown.
     *
     * @param project
     *            the eclipse project
     * @param monitor
     *            a progress monitor
     * @throws java.io.FileNotFoundException
     *             the saved bug collection doesn't exist
     * @throws IOException
     * @throws DocumentException
     * @throws CoreException
     */
    private static void readBugCollectionAndProject(IProject project, IProgressMonitor monitor, boolean useCloud)
            throws IOException, DocumentException, CoreException {
        SortedBugCollection bugCollection;

        IPath bugCollectionPath = getBugCollectionFile(project);
        // Don't turn the path to an IFile because it isn't local to the
        // project.
        // see the javadoc for org.eclipse.core.runtime.Plugin
        File bugCollectionFile = bugCollectionPath.toFile();
        if (!bugCollectionFile.exists()) {
            // throw new
            // FileNotFoundException(bugCollectionFile.getLocation().toOSString());
            getDefault().logInfo("creating new bug collection: " + bugCollectionPath.toOSString());
            createDefaultEmptyBugCollection(project); // since we no longer
            // throw, have to do this
            // here
            return;
        }

        UserPreferences prefs = getUserPreferences(project);
        bugCollection = new SortedBugCollection();
        bugCollection.getProject().setGuiCallback(new EclipseGuiCallback(project));
        bugCollection.setDoNotUseCloud(!useCloud);

        bugCollection.readXML(bugCollectionFile);
        if (useCloud) {
            String cloudId = prefs.getCloudId();
            if (cloudId != null) {
                bugCollection.getProject().setCloudId(cloudId);
            }
        }

        cacheBugCollectionAndProject(project, bugCollection, bugCollection.getProject());
    }

    /**
     * Store a new bug collection for a project. The collection is stored in the
     * session, and also in a file in the project.
     *
     * @param project
     *            the project
     * @param bugCollection
     *            the bug collection
     * @param monitor
     *            progress monitor
     * @throws IOException
     * @throws CoreException
     */
    public static void storeBugCollection(IProject project, final SortedBugCollection bugCollection, IProgressMonitor monitor)
            throws IOException, CoreException {

        // Store the bug collection and findbugs project in the session
        project.setSessionProperty(SESSION_PROPERTY_BUG_COLLECTION, bugCollection);

        if (bugCollection != null) {
            writeBugCollection(project, bugCollection, monitor);
        }
    }

    /**
     * If necessary, save current bug collection for project to disk.
     *
     * @param project
     *            the project
     * @param monitor
     *            a progress monitor
     * @throws CoreException
     */
    public static void saveCurrentBugCollection(IProject project, IProgressMonitor monitor) throws CoreException {
        if (isBugCollectionDirty(project)) {
            SortedBugCollection bugCollection = (SortedBugCollection) project.getSessionProperty(SESSION_PROPERTY_BUG_COLLECTION);

            if (bugCollection != null) {
                writeBugCollection(project, bugCollection, monitor);
            }
        }
    }

    private static void writeBugCollection(IProject project, final SortedBugCollection bugCollection, IProgressMonitor monitor)
            throws CoreException {
        // Save to file
        IPath bugCollectionPath = getBugCollectionFile(project);
        // Don't turn the path to an IFile because it isn't local to the
        // project.
        // see the javadoc for org.eclipse.core.runtime.Plugin
        File bugCollectionFile = bugCollectionPath.toFile();
        FileOutput fileOutput = new FileOutput() {
            @Override
            public void writeFile(OutputStream os) throws IOException {
                bugCollection.writeXML(os);
            }

            @Override
            public String getTaskDescription() {
                return "creating XML FindBugs data file";
            }
        };
        IO.writeFile(bugCollectionFile, fileOutput, monitor);
        markBugCollectionDirty(project, false);
    }

    /**
     * Get the FindBugs preferences file for a project (which may not exist yet)
     *
     * @param project
     *            the project
     * @return the IFile for the FindBugs preferences file, if any. Can be
     *         "empty" handle if the real file does not exist yet
     */
    private static IFile getUserPreferencesFile(IProject project) {
        IFile defaultFile = project.getFile(DEFAULT_PREFS_PATH);
        IFile oldFile = project.getFile(DEPRECATED_PREFS_PATH);
        if (defaultFile.isAccessible() || !oldFile.isAccessible()) {
            return defaultFile;
        }
        return oldFile;
    }

    public static boolean isProjectSettingsEnabled(IProject project) {
        // fast path: read from session, if available
        Boolean enabled;
        try {
            enabled = (Boolean) project.getSessionProperty(SESSION_PROPERTY_SETTINGS_ON);
        } catch (CoreException e) {
            enabled = null;
        }
        if (enabled != null) {
            return enabled.booleanValue();
        }

        // legacy support: before 1.3.8, there was ONLY project preferences in
        // .fbprefs
        // so check if the file is there...
        IFile file = getUserPreferencesFile(project);
        boolean projectPropsEnabled = file.isAccessible();
        if (projectPropsEnabled) {
            ScopedPreferenceStore store = new ScopedPreferenceStore(new ProjectScope(project), PLUGIN_ID);
            // so if the file is there, we can check if after 1.3.8 the flag is
            // set
            // to use workspace properties instead
            projectPropsEnabled = !store.contains(FindBugsConstants.PROJECT_PROPS_DISABLED)
                    || !store.getBoolean(FindBugsConstants.PROJECT_PROPS_DISABLED);
        }
        // remember in the session to speedup access, don't touch the store
        setProjectSettingsEnabled(project, null, projectPropsEnabled);
        return projectPropsEnabled;
    }

    public static void setProjectSettingsEnabled(IProject project,
            @CheckForNull IPreferenceStore store, boolean enabled) {
        try {
            project.setSessionProperty(SESSION_PROPERTY_SETTINGS_ON, Boolean.valueOf(enabled));
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, "Error setting FindBugs session property for project");
        }
        if (store != null) {
            store.setValue(FindBugsConstants.PROJECT_PROPS_DISABLED, !enabled);
        }
    }

    /**
     * Get the FindBugs core preferences for given project. This method can
     * return workspace preferences if project preferences are not created yet
     * or they are disabled.
     *
     * @param project
     *            the project (if null, workspace settings are used)
     * @param forceRead
     *            true to enforce reading properties from disk
     *
     * @return the preferences for the project or prefs from workspace
     */
    public static UserPreferences getCorePreferences(@CheckForNull IProject project, boolean forceRead) {
        if (project == null || !isProjectSettingsEnabled(project)) {
            // read workspace (user) settings from instance area
            return getWorkspacePreferences();
        }

        // use project settings
        return getProjectPreferences(project, forceRead);
    }

    /**
     * Get the Eclipse plugin preferences for given project. This method can
     * return workspace preferences if project preferences are not created yet
     * or they are disabled.
     *
     * @param project
     *            the project (if null, workspace settings are used)
     *
     * @return the preferences for the project or prefs from workspace
     */
    public static IPreferenceStore getPluginPreferences(@CheckForNull IProject project) {
        if (project == null || !isProjectSettingsEnabled(project)) {
            // read workspace (user) settings from instance area
            return new ScopedPreferenceStore(new InstanceScope(), FindbugsPlugin.PLUGIN_ID);
        }

        // use project settings
        return new ScopedPreferenceStore(new ProjectScope(project), FindbugsPlugin.PLUGIN_ID);
    }

    /**
     * Get project own preferences set.
     *
     * @param project
     *            must be non null, exist and be opened
     * @param forceRead
     * @return current project preferences, independently if project preferences
     *         are enabled or disabled for given project.
     */
    public static UserPreferences getProjectPreferences(IProject project, boolean forceRead) {
        try {
            UserPreferences prefs = (UserPreferences) project.getSessionProperty(SESSION_PROPERTY_USERPREFS);
            if (prefs == null || forceRead) {
                prefs = readUserPreferences(project);
                if (prefs == null) {
                    prefs = getWorkspacePreferences().clone();
                }
                project.setSessionProperty(SESSION_PROPERTY_USERPREFS, prefs);
            }
            return prefs;
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, "Error getting FindBugs preferences for project");
            return getWorkspacePreferences().clone();
        }
    }

    private static UserPreferences getWorkspacePreferences() {
        // create initially default settings
        UserPreferences userPrefs = FindBugsPreferenceInitializer.createDefaultUserPreferences();
        File prefsFile = WORKSPACE_PREFS_PATH.toFile();
        if (!prefsFile.isFile()) {
            return userPrefs;
        }
        // load custom settings over defaults
        FileInputStream in;
        try {
            in = new FileInputStream(prefsFile);
            userPrefs.read(in);
        } catch (IOException e) {
            FindbugsPlugin.getDefault().logException(e, "Error reading custom FindBugs preferences for workspace");
        }
        return userPrefs;
    }

    /**
     * Get the UserPreferences for given project.
     *
     * @param project
     *            the project
     * @return the UserPreferences for the project
     */
    public static UserPreferences getUserPreferences(IProject project) {
        return getCorePreferences(project, false);
    }

    /**
     * Save current UserPreferences for given project or workspace.
     *
     * @param project
     *            the project or null for workspace
     * @throws CoreException
     */
    public static void saveUserPreferences(IProject project, final UserPreferences userPrefs) throws CoreException {

        FileOutput userPrefsOutput = new FileOutput() {
            @Override
            public void writeFile(OutputStream os) throws IOException {
                userPrefs.write(os);
            }

            @Override
            public String getTaskDescription() {
                return "writing user preferences";
            }
        };

        if (project != null) {
            // Make the new user preferences current for the project
            project.setSessionProperty(SESSION_PROPERTY_USERPREFS, userPrefs);
            IFile userPrefsFile = getUserPreferencesFile(project);
            ensureReadWrite(userPrefsFile);
            IO.writeFile(userPrefsFile, userPrefsOutput, null);
            if (project.getFile(DEPRECATED_PREFS_PATH).equals(userPrefsFile)) {
                String message = "Found old style FindBugs preferences for project '" + project.getName()
                        + "'. This preferences are not at the default location: '" + DEFAULT_PREFS_PATH + "'." + " Please move '"
                        + DEPRECATED_PREFS_PATH + "' to '" + DEFAULT_PREFS_PATH + "'.";
                getDefault().logWarning(message);
            }
        } else {
            // write the workspace preferences to the eclipse preference store
            ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
            try {
                userPrefs.write(bos);
            } catch (IOException e) {
                getDefault().logException(e, "Failed to write user preferences");
                return;
            }
            Properties props = new Properties();
            try {
                props.load(new ByteArrayInputStream(bos.toByteArray()));
            } catch (IOException e) {
                getDefault().logException(e, "Failed to save user preferences");
                return;
            }
            IPreferenceStore store = getDefault().getPreferenceStore();
            // Reset any existing custom group entries
            resetStore(store, UserPreferences.KEY_PLUGIN);
            resetStore(store, UserPreferences.KEY_EXCLUDE_BUGS);
            resetStore(store, UserPreferences.KEY_EXCLUDE_FILTER);
            resetStore(store, UserPreferences.KEY_INCLUDE_FILTER);
            for (Entry<Object, Object> entry : props.entrySet()) {
                store.putValue((String) entry.getKey(), (String) entry.getValue());
            }
            if(store instanceof IPersistentPreferenceStore){
                IPersistentPreferenceStore store2 = (IPersistentPreferenceStore) store;
                try {
                    store2.save();
                } catch (IOException e) {
                    getDefault().logException(e, "Failed to save user preferences");
                }
            }
        }
    }

    /**
     * Removes all consequent enumerated keys from given store staring with given prefix
     */
    private static void resetStore(IPreferenceStore store, String prefix) {
        int start = 0;
        // 99 is paranoia.
        while(start < 99){
            String name = prefix + start;
            if(store.contains(name)){
                store.setToDefault(name);
            } else {
                break;
            }
            start ++;
        }
    }

    /**
     * Ensure that a file is writable. If not currently writable, check it as so
     * that we can edit it.
     *
     * @param file
     *            - file that should be made writable
     * @throws CoreException
     */
    private static void ensureReadWrite(IFile file) throws CoreException {
        /*
         * fix for bug 1683264: we should checkout file before writing to it
         */
        if (file.isReadOnly()) {
            IStatus checkOutStatus = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] { file }, null);
            if (!checkOutStatus.isOK()) {
                throw new CoreException(checkOutStatus);
            }
        }
    }

    /**
     * Read UserPreferences for project from the file in the project directory.
     * Returns null if the preferences have not been saved to a file, or if
     * there is an error reading the preferences file.
     *
     * @param project
     *            the project to get the UserPreferences for
     * @return the UserPreferences, or null if the UserPreferences file could
     *         not be read
     * @throws CoreException
     */
    private static UserPreferences readUserPreferences(IProject project) throws CoreException {
        IFile userPrefsFile = getUserPreferencesFile(project);
        if (!userPrefsFile.exists()) {
            return null;
        }
        try {
            // force is preventing us for out-of-sync exception if file was
            // changed externally
            InputStream in = userPrefsFile.getContents(true);
            UserPreferences userPrefs = FindBugsPreferenceInitializer.createDefaultUserPreferences();
            userPrefs.read(in);
            return userPrefs;
        } catch (IOException e) {
            FindbugsPlugin.getDefault().logException(e, "Could not read user preferences for project");
            return null;
        }
    }

    public static void showMarker(IMarker marker, String viewId, IWorkbenchPart source) {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IViewPart view = page.findView(viewId);
        if (!page.isPartVisible(view)) {
            try {
                view = page.showView(viewId);
            } catch (PartInitException e) {
                FindbugsPlugin.getDefault().logException(e, "Could not open view: " + viewId);
                return;
            }
        }
        if (view instanceof IMarkerSelectionHandler) {
            IMarkerSelectionHandler handler = (IMarkerSelectionHandler) view;
            handler.markerSelected(source, marker);
        } else if (DETAILS_VIEW_ID.equals(viewId) && view instanceof ISelectionListener) {
            ISelectionListener listener = (ISelectionListener) view;
            listener.selectionChanged(source, new StructuredSelection(marker));
        }
    }

    /**
     * Call this method to retrieve the (cache) ImageDescriptor for the given
     * id.
     *
     * @param id
     *            the id of the image descriptor or relative icon path if icon
     *            is inside of default icons folder
     * @return the ImageDescriptor instance.
     */
    public ImageDescriptor getImageDescriptor(String id) {
        ImageDescriptor imageDescriptor = imageDescriptors.get(id);
        if (imageDescriptor == null) {
            String pluginId = getDefault().getBundle().getSymbolicName();
            imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, ICON_PATH + id);
            imageDescriptors.put(id, imageDescriptor);
        }
        return imageDescriptor;
    }

    public static Set<BugPattern> getKnownPatterns() {
        Set<BugPattern> patterns = new TreeSet<BugPattern>();
        Iterator<BugPattern> patternIterator = DetectorFactoryCollection.instance().bugPatternIterator();
        while (patternIterator.hasNext()) {
            patterns.add(patternIterator.next());
        }
        return patterns;
    }

    public static Set<BugCode> getKnownPatternTypes() {
        Set<BugCode> patterns = new TreeSet<BugCode>(DetectorFactoryCollection.instance().getBugCodes());
        return patterns;
    }

    public static Set<String> getFilteredIds() {
        final IPreferenceStore store = FindbugsPlugin.getDefault().getPreferenceStore();
        String lastUsedFilter = store.getString(FindBugsConstants.LAST_USED_EXPORT_FILTER);
        return FindBugsConstants.decodeIds(lastUsedFilter);
    }

    public static Set<BugPattern> getFilteredPatterns() {
        Iterator<BugPattern> patternIterator = DetectorFactoryCollection.instance().bugPatternIterator();
        Set<BugPattern> set = new HashSet<BugPattern>();
        Set<String> patternTypes = getFilteredIds();
        while (patternIterator.hasNext()) {
            BugPattern next = patternIterator.next();
            String patternId = next.getType();
            if (!patternTypes.contains(patternId)) {
                continue;
            }
            set.add(next);
        }
        return set;
    }

    public static Set<BugCode> getFilteredPatternTypes() {
        Set<BugCode> set = new HashSet<BugCode>();
        Set<String> patternTypes = getFilteredIds();
        for(BugCode next :  DetectorFactoryCollection.instance().getBugCodes()) {
            String type = next.getAbbrev();
            if (!patternTypes.contains(type)) {
                continue;
            }
            set.add(next);
        }
        return set;
    }

    public static void clearBugCollection(IProject project) throws CoreException {
        createDefaultEmptyBugCollection(project);
        markBugCollectionDirty(project, true);
        saveCurrentBugCollection(project, null);
    }


    public static void log(String msg) {
        log(msg, null);
     }

     public static void log(String msg, Exception e) {
        plugin.getLog().log(new Status(IStatus.INFO, FindbugsPlugin.PLUGIN_ID,  msg, e));
     }
}
