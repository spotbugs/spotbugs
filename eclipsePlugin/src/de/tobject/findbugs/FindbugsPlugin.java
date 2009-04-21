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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import org.dom4j.DocumentException;
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
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPageLayout;
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
import de.tobject.findbugs.reporter.Reporter;
import de.tobject.findbugs.view.IMarkerSelectionHandler;
import de.tobject.findbugs.view.explorer.BugContentProvider;
import edu.umd.cs.findbugs.BugCode;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolutionAssociations;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolutionLoader;

/**
	* The main plugin class to be used in the desktop.
	*/
public class FindbugsPlugin extends AbstractUIPlugin {
	public static final String ICON_PATH = "icons/";

	public static final String DETAILS_VIEW_ID = IPageLayout.ID_PROP_SHEET;
	public static final String USER_ANNOTATIONS_VIEW_ID = "de.tobject.findbugs.view.userannotationsview";
	public static final String TREE_VIEW_ID = "de.tobject.findbugs.view.bugtreeview";
	public static final String BUG_CONTENT_PROVIDER_ID = "de.tobject.findbugs.view.explorer.BugContentProvider";

	/** Map containing preloaded ImageDescriptors */
	private final Map<String, ImageDescriptor> imageDescriptors = new HashMap<String, ImageDescriptor>(13);

	/** Controls debugging of the plugin */
	public static boolean DEBUG;

	/**
	 * The plug-in identifier of the FindBugs Plug-in
	 * (value "edu.umd.cs.findbugs.plugin.eclipse", was <code>"de.tobject.findbugs"</code>).
	 */
	public static final String PLUGIN_ID = "edu.umd.cs.findbugs.plugin.eclipse"; //$NON-NLS-1$

	/**
	 * The identifier for the FindBugs builder
	 * (value <code>"edu.umd.cs.findbugs.plugin.eclipse.findbugsbuilder"</code>).
	 */
	public static final String BUILDER_ID = PLUGIN_ID + ".findbugsBuilder"; //$NON-NLS-1$

	/**
	 * The identifier for the FindBugs nature
	 * (value <code>"edu.umd.cs.findbugs.plugin.eclipse.findbugsnature"</code>).
	 *
	 * @see org.eclipse.core.resources.IProject#hasNature(java.lang.String)
	 */
	public static final String NATURE_ID = PLUGIN_ID + ".findbugsNature"; //$NON-NLS-1$

	// Debugging options
	private static final String PLUGIN_DEBUG = PLUGIN_ID + "/debug/plugin"; //$NON-NLS-1$
	private static final String BUILDER_DEBUG = PLUGIN_ID + "/debug/builder"; //$NON-NLS-1$
	private static final String NATURE_DEBUG = PLUGIN_ID + "/debug/nature"; //$NON-NLS-1$
	private static final String REPORTER_DEBUG = PLUGIN_ID + "/debug/reporter"; //$NON-NLS-1$
	private static final String CONTENT_DEBUG = PLUGIN_ID + "/debug/content"; //$NON-NLS-1$
	private static final String PROFILER_DEBUG = PLUGIN_ID + "/debug/profiler"; //$NON-NLS-1$

	// Persistent and session property keys
	public static final QualifiedName SESSION_PROPERTY_BUG_COLLECTION =
		new QualifiedName(FindbugsPlugin.PLUGIN_ID + ".sessionprops", "bugcollection");

	public static final QualifiedName SESSION_PROPERTY_BUG_COLLECTION_DIRTY =
		new QualifiedName(FindbugsPlugin.PLUGIN_ID + ".sessionprops", "bugcollection.dirty");

	public static final QualifiedName SESSION_PROPERTY_USERPREFS =
		new QualifiedName(FindbugsPlugin.PLUGIN_ID + ".sessionprops", "userprefs");

	public static final QualifiedName SESSION_PROPERTY_SETTINGS_ON =
		new QualifiedName(FindbugsPlugin.PLUGIN_ID + ".sessionprops", "settingsOn");

	public static final String LIST_DELIMITER = ";"; //$NON-NLS-1$

	/** The shared instance. */
	private static FindbugsPlugin plugin;

	/** Resource bundle. */
	private ResourceBundle resourceBundle;

	private BugResolutionAssociations bugResolutions;

	/**
	 * Constructor.
	 */
	public FindbugsPlugin() {
		plugin = this;
	}

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		// configure debugging
		configurePluginDebugOptions();

		// initialize resource strings
		try {
			resourceBundle = ResourceBundle.getBundle("de.tobject.findbugs.messages"); //this is correct //$NON-NLS-1$
		}
		catch (MissingResourceException x) {
			resourceBundle = null;
		}

		// TODO hardcore workaround for findbugs home property
		// - see de.tobject.findbugs.builder.FindBugsWorker.work() too
		String findBugsHome = getFindBugsEnginePluginLocation();
		if (DEBUG) {
			logInfo("Looking for FindBugs detectors in: " + findBugsHome);
		}
		System.setProperty("findbugs.home", findBugsHome);

		// Register our save participant
		FindbugsSaveParticipant saveParticipant = new FindbugsSaveParticipant();
		ResourcesPlugin.getWorkspace().addSaveParticipant(this, saveParticipant);
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		for (FindBugsMarker.Priority prio : FindBugsMarker.Priority.values()) {
			ImageDescriptor descriptor = getImageDescriptor(prio.iconName());
			if(descriptor != null){
				reg.put(prio.iconName(), descriptor);
			}
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
		if(Display.getCurrent() != null) {
			return getDefault().getWorkbench().getActiveWorkbenchWindow();
		}
		// need to call from UI thread
		final IWorkbenchWindow [] window = new IWorkbenchWindow[1];
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				window[0] = getDefault().getWorkbench().getActiveWorkbenchWindow();
			}
		});
		return window [0];
	}

	/**
	 * Returns the SWT Shell of the active workbench window or <code>null</code> if
	 * no workbench window is active.
	 *
	 * @return the SWT Shell of the active workbench window, or <code>null</code> if
	 * 	no workbench window is active
	 */
	public static Shell getShell() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}
		return window.getShell();
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = FindbugsPlugin.getDefault().getResourceBundle();
		try {
			return bundle.getString(key);
		}
		catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	@SuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
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
			if(Boolean.valueOf(option).booleanValue()){
				System.setProperty("profiler.report", "true");
			}
		}
	}

	/**
	 * Find the filesystem path of the FindBugs plugin directory.
	 *
	 * @return the filesystem path of the FindBugs plugin directory,
	 *         or null if the FindBugs plugin directory cannot be found
	 */
	public static String getFindBugsEnginePluginLocation() {
		// findbugs.home should be set to the directory the plugin is
		// installed in.
		URL u = plugin.getBundle().getEntry("/");
		try {
			URL bundleRoot = FileLocator.resolve(u);
			if (FindBugsBuilder.DEBUG) {
				System.out.println("Pluginpath: " + bundleRoot.getPath()); //$NON-NLS-1$
			}
			return bundleRoot.getPath();
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
	 * @param e       the exception
	 * @param message message describing how/why the exception occurred
	 */
	public void logException(Throwable e, String message) {
		logMessage(IStatus.ERROR, message, e);
	}

	/**
	 * Log an error.
	 *
	 * @param message error message
	 */
	public void logError(String message) {
		logMessage(IStatus.ERROR, message, null);
	}

	/**
	 * Log a warning.
	 *
	 * @param message warning message
	 */
	public void logWarning(String message) {
		logMessage(IStatus.WARNING, message, null);
	}

	/**
	 * Log an informational message.
	 *
	 * @param message the informational message
	 */
	public void logInfo(String message) {
		logMessage(IStatus.INFO, message, null);
	}

	public void logMessage(int severity, String message, Throwable e) {
		if (DEBUG) {
			String what = (severity == IStatus.ERROR)
				? (e != null ? "Exception" : "Error")
				: "Warning";
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
	 * @param project the project
	 * @return the IPath to the file (which may not actually exist in the filesystem yet)
	 */
	private static IPath getBugCollectionFile(IProject project) {
		//IPath path = project.getWorkingLocation(PLUGIN_ID); // project-specific but not user-specific?
		IPath path = getDefault().getStateLocation(); // user-specific but not project-specific
		return path.append(project.getName()+".fbwarnings");
	}

	private static boolean isBugCollectionDirty(IProject project) throws CoreException {
		Object dirty = project.getSessionProperty(SESSION_PROPERTY_BUG_COLLECTION_DIRTY);

		if (dirty == null) {
			return false;
		}
		return ((Boolean) dirty).booleanValue();
	}

	public static void markBugCollectionDirty(IProject project, boolean isDirty) throws CoreException {
		project.setSessionProperty(
				SESSION_PROPERTY_BUG_COLLECTION_DIRTY, isDirty ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 * Get the stored BugCollection for project.
	 * If there is no stored bug collection for the project,
	 * or if an error occurs reading the stored bug collection,
	 * a default empty collection is created and returned.
	 *
	 * @param project the eclipse project
	 * @param monitor a progress monitor
	 * @return the stored BugCollection, never null
	 * @throws CoreException
	 */
	public static SortedBugCollection getBugCollection(
			IProject project, IProgressMonitor monitor) throws CoreException {
		SortedBugCollection bugCollection = (SortedBugCollection) project.getSessionProperty(
				SESSION_PROPERTY_BUG_COLLECTION);
		if (bugCollection == null) {
			try {
				readBugCollectionAndProject(project, monitor);
				bugCollection = (SortedBugCollection) project.getSessionProperty(
						SESSION_PROPERTY_BUG_COLLECTION);
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

	private static void cacheBugCollectionAndProject(IProject project, SortedBugCollection bugCollection, Project fbProject) throws CoreException {
		project.setSessionProperty(SESSION_PROPERTY_BUG_COLLECTION, bugCollection);
		markBugCollectionDirty(project, false);
	}

	private static SortedBugCollection createDefaultEmptyBugCollection(IProject project)
			throws CoreException {
		SortedBugCollection bugCollection = new SortedBugCollection();
		Project fbProject = new Project();

		cacheBugCollectionAndProject(project, bugCollection, fbProject);

		return bugCollection;
	}
//
//	/**
//	 * Read stored findbugs Project for a project.
//	 * Returns an empty default project if no project is stored.
//	 *
//	 * @param project the eclipse project
//	 * @param monitor a progress monitor
//	 * @return the saved findbugs Project, or null if there is no saved project
//	 * @throws CoreException
//	 * @throws DocumentException
//	 * @throws DocumentException
//	 * @throws IOException
//	 */
//	public static Project readProject(IProject project, IProgressMonitor monitor)
//			throws CoreException, DocumentException, DocumentException, IOException {
//		Project findbugsProject = (Project) project.getSessionProperty(
//				SESSION_PROPERTY_FB_PROJECT);
//		if (findbugsProject == null) {
//			readBugCollectionAndProject(project, monitor);
//			findbugsProject =  (Project) project.getSessionProperty(
//					SESSION_PROPERTY_FB_PROJECT);
//		}
//		return findbugsProject;
//	}

	/**
	 * Read saved bug collection and findbugs project from file.
	 * Will populate the bug collection and findbugs project session
	 * properties if successful.  If there is no saved bug collection and project
	 * for the eclipse project, then FileNotFoundException will
	 * be thrown.
	 *
	 * @param project the eclipse project
	 * @param monitor a progress monitor
	 * @throws java.io.FileNotFoundException the saved bug collection doesn't exist
	 * @throws IOException
	 * @throws DocumentException
	 * @throws CoreException
	 */
	private static void readBugCollectionAndProject(IProject project, IProgressMonitor monitor) throws IOException, DocumentException, CoreException {
		SortedBugCollection bugCollection;
		
		IPath bugCollectionPath = getBugCollectionFile(project);
		// Don't turn the path to an IFile because it isn't local to the project.
		// see the javadoc for org.eclipse.core.runtime.Plugin
		File bugCollectionFile = bugCollectionPath.toFile();
		if (!bugCollectionFile.exists()) {
			//throw new FileNotFoundException(bugCollectionFile.getLocation().toOSString());
			getDefault().logInfo("creating new bug collection: "+bugCollectionPath.toOSString());
			createDefaultEmptyBugCollection(project); // since we no longer throw, have to do this here
			return;
		}
		
		bugCollection = new SortedBugCollection();
		
		bugCollection.readXML(bugCollectionFile);

		cacheBugCollectionAndProject(project, bugCollection, bugCollection.getProject());
	}

	/**
	 * Store a new bug collection for a project.
	 * The collection is stored in the session, and also in
	 * a file in the project.
	 *
	 * @param project         the project
	 * @param bugCollection   the bug collection
	 * @param monitor         progress monitor
	 * @throws IOException
	 * @throws CoreException
	 */
	public static void storeBugCollection(
			IProject project,
			final SortedBugCollection bugCollection,
			IProgressMonitor monitor) throws IOException, CoreException {

		// Store the bug collection and findbugs project in the session
		project.setSessionProperty(SESSION_PROPERTY_BUG_COLLECTION, bugCollection);

		if (bugCollection != null) {
			writeBugCollection(project, bugCollection, monitor);
		}
	}

	/**
	 * If necessary, save current bug collection for project to disk.
	 *
	 * @param project the project
	 * @param monitor a progress monitor
	 * @throws CoreException
	 */
	public static void saveCurrentBugCollection(
			IProject project, IProgressMonitor monitor)
			throws CoreException {
		if (isBugCollectionDirty(project)) {
			SortedBugCollection bugCollection =
			(SortedBugCollection) project.getSessionProperty(SESSION_PROPERTY_BUG_COLLECTION);

			if (bugCollection != null) {
				writeBugCollection(project, bugCollection, monitor);
			}
		}
	}


	private static void writeBugCollection(
			IProject project, final SortedBugCollection bugCollection, IProgressMonitor monitor)
			throws CoreException {
		// Save to file
		IPath bugCollectionPath = getBugCollectionFile(project);
		// Don't turn the path to an IFile because it isn't local to the project.
		// see the javadoc for org.eclipse.core.runtime.Plugin
		File bugCollectionFile = bugCollectionPath.toFile();
		FileOutput fileOutput = new FileOutput() {
			public void writeFile(OutputStream os) throws IOException {
				bugCollection.writeXML(os);
			}

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
	 * @param project the project
	 * @return the IFile for the FindBugs preferences file, if any. Can be "empty" handle
	 * if the real file does not exist yet
	 */
	private static IFile getUserPreferencesFile(IProject project) {
		return project.getFile(".fbprefs");
	}

	public static boolean isProjectSettingsEnabled(IProject project){
		// fast path: read from session, if available
		Boolean enabled;
		try {
			enabled = (Boolean) project.getSessionProperty(SESSION_PROPERTY_SETTINGS_ON);
		} catch (CoreException e) {
			enabled = null;
		}
		if(enabled != null){
			return enabled.booleanValue();
		}

    	// legacy support: before 1.3.8, there was ONLY project preferences in .fbprefs
    	// so check if the file is there...
    	IFile file = getUserPreferencesFile(project);
    	boolean projectPropsEnabled = file.isAccessible();
		if(projectPropsEnabled){
			ScopedPreferenceStore store = new ScopedPreferenceStore(new ProjectScope(project), PLUGIN_ID);
			// so if the file is there, we can check if after 1.3.8 the flag is set
			// to use workspace properties instead
			projectPropsEnabled = !store
					.contains(FindBugsConstants.PROJECT_PROPS_DISABLED)
					|| !store.getBoolean(FindBugsConstants.PROJECT_PROPS_DISABLED);
		}
		// remember in the session to speedup access, don't touch the store
		setProjectSettingsEnabled(project, null, projectPropsEnabled);
		return projectPropsEnabled;
	}

	public static void setProjectSettingsEnabled(IProject project, IPreferenceStore store, boolean enabled){
		try {
			project.setSessionProperty(SESSION_PROPERTY_SETTINGS_ON, Boolean.valueOf(enabled));
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e,
			"Error setting FindBugs session property for project");
		}
		if(store != null) {
			store.setValue(FindBugsConstants.PROJECT_PROPS_DISABLED, !enabled);
		}
	}

	/**
	 * Get the preferences for given project. This method can return workspace preferences
	 * if project preferences are not created yet or they are disabled.
	 *
	 * @param project
	 *            the project (if null, workspace settings are used)
	 * @param forceRead
	 *            true to enforce reading properties from disk
	 *
	 * @return the preferences for the project or user prefs from workspace
	 */
	public static UserPreferences getUserPreferences(IProject project, boolean forceRead) {
		if(project == null || !isProjectSettingsEnabled(project)){
			// read workspace (user) settings from instance area
			return getWorkspacePreferences();
		}

		// use project settings
		return getProjectPreferences(project, forceRead);
	}

	/**
	 * Get project own preferences set.
	 * @param project must be non null, exist and be opened
	 * @param forceRead
	 * @return current project preferences, independently if project prefrences are
	 *         enabled or disabled for given project.
	 */
	public static UserPreferences getProjectPreferences(IProject project,
			boolean forceRead) {
		try {
			UserPreferences prefs = (UserPreferences) project
					.getSessionProperty(SESSION_PROPERTY_USERPREFS);
			if (prefs == null || forceRead) {
				prefs = readUserPreferences(project);
				if (prefs == null) {
					prefs = (UserPreferences) getWorkspacePreferences().clone();
				}
				project.setSessionProperty(SESSION_PROPERTY_USERPREFS, prefs);
			}
			return prefs;
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Error getting FindBugs preferences for project");
			return (UserPreferences) getWorkspacePreferences().clone();
		}
	}

	private static UserPreferences getWorkspacePreferences() {
		IPath path = getDefault().getStateLocation().append(".fbprefs");
		// create initially default settings
		UserPreferences userPrefs = FindBugsPreferenceInitializer.createDefaultUserPreferences();
		File prefsFile = path.toFile();
		if(!prefsFile.isFile()){
			return userPrefs;
		}
		// load custom settings over defaults
		FileInputStream in;
		try {
			in = new FileInputStream(prefsFile);
			userPrefs.read(in);
		} catch (IOException e) {
			FindbugsPlugin.getDefault().logException(e,
				"Error reading custom FindBugs preferences for workspace");
		}
		return userPrefs;
	}

	/**
	 * Get the UserPreferences for given project.
	 *
	 * @param project the project
	 * @return the UserPreferences for the project
	 */
	public static UserPreferences getUserPreferences(IProject project) {
		return getUserPreferences(project, false);
	}


	/**
	 * Save current UserPreferences for given project or workspace.
	 *
	 * @param project the project or null for workspace
	 * @throws CoreException
	 */
	public static void saveUserPreferences(IProject project, final UserPreferences userPrefs)
			throws CoreException {

		FileOutput userPrefsOutput = new FileOutput() {
			public void writeFile(OutputStream os) throws IOException {
				userPrefs.write(os);
			}
			public String getTaskDescription() {
				return "writing user preferences";
			}
		};

		if(project != null) {
			// Make the new user preferences current for the project
			project.setSessionProperty(SESSION_PROPERTY_USERPREFS, userPrefs);
			IFile userPrefsFile = getUserPreferencesFile(project);
			ensureReadWrite(userPrefsFile);
			IO.writeFile(userPrefsFile, userPrefsOutput, null);
		} else {
			// write file to the workspace area
			IPath path = getDefault().getStateLocation();
			path = path.append(".fbprefs");
			IO.writeFile(path.toFile(), userPrefsOutput, null);
		}
	}

	/**
	 * Ensure that a file is writable. If not currently writable,
	 * check it as so that we can edit it.
	 *
	 * @param file - file that should be made writable
	 * @throws CoreException
	 */
	private static void ensureReadWrite(IFile file) throws CoreException {
		/*
		 * fix for bug 1683264: we should checkout file before writing to it
		 */
		if(file.isReadOnly()){
			IStatus checkOutStatus =
				ResourcesPlugin.getWorkspace().validateEdit(new IFile[]{file}, null);
			if(! checkOutStatus.isOK()){
				throw new CoreException(checkOutStatus);
			}
		}
	}



	/**
	 * Read UserPreferences for project from the file in the project directory.
	 * Returns null if the preferences have not been saved to a file,
	 * or if there is an error reading the preferences file.
	 *
	 * @param project the project to get the UserPreferences for
	 * @return the UserPreferences, or null if the UserPreferences file could not be read
	 * @throws CoreException
	 */
	private static UserPreferences readUserPreferences(IProject project) throws CoreException {
		IFile userPrefsFile = getUserPreferencesFile(project);
		if (!userPrefsFile.exists()) {
			return null;
		}
		try {
			// force is preventing us for out-of-sync exception if file was changed externally
			InputStream in = userPrefsFile.getContents(true);
			UserPreferences userPrefs = FindBugsPreferenceInitializer.createDefaultUserPreferences();
			userPrefs.read(in);
			return userPrefs;
		} catch (IOException e) {
			FindbugsPlugin.getDefault().logException(
					e, "Could not read user preferences for project");
			return null;
		}
	}

	public BugResolutionAssociations getBugResolutions() {
		if (bugResolutions == null) {
			bugResolutions = loadBugResolutions();
		}
		return bugResolutions;
	}

	private BugResolutionAssociations loadBugResolutions() {
		BugResolutionLoader loader = new BugResolutionLoader();
		File xmlFile = new File(FindBugs.getHome() + File.separator + "plugin" + File.separator + "findbugs-resolutions.xml");
		return loader.loadBugResolutions(xmlFile);
	}

	public static void showMarker(IMarker marker, String viewId, IWorkbenchPart source) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IViewPart view = page.findView(viewId);
		if(!page.isPartVisible(view)){
			try {
				view = page.showView(viewId);
			} catch (PartInitException e) {
				FindbugsPlugin.getDefault().logException(
						e, "Could not open view: " + viewId);
				return;
			}
		}
		if(view instanceof IMarkerSelectionHandler){
			IMarkerSelectionHandler handler = (IMarkerSelectionHandler) view;
			handler.markerSelected(marker);
		} else if(DETAILS_VIEW_ID.equals(viewId) && view instanceof ISelectionListener){
			ISelectionListener listener = (ISelectionListener) view;
			listener.selectionChanged(source, new StructuredSelection(marker));
		}
	}

	/**
	 * Call this method to retrieve the (cache) ImageDescriptor for the given id.
	 * @param id the id of the image descriptor or relative icon path if icon is inside
	 * of default icons folder
	 * @return the ImageDescriptor instance.
	 */
	public ImageDescriptor getImageDescriptor(String id) {
		ImageDescriptor imageDescriptor = imageDescriptors.get(id);
		if (imageDescriptor == null) {
			String pluginId = getDefault()
					.getBundle().getSymbolicName();
			imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, ICON_PATH + id);
			imageDescriptors.put(id, imageDescriptor);
		}
		return imageDescriptor;
	}

	public static Set<BugPattern> getKnownPatterns() {
		Set<BugPattern> patterns = new TreeSet<BugPattern>();
		Iterator<BugPattern> patternIterator = I18N.instance().bugPatternIterator();
		while (patternIterator.hasNext()){
			patterns.add(patternIterator.next());
		}
		return patterns;
	}

	public static Set<BugCode> getKnownPatternTypes() {
		Set<BugCode> patterns = new TreeSet<BugCode>();
		Iterator<BugCode> patternIterator = I18N.instance().bugCodeIterator();
		while (patternIterator.hasNext()){
			patterns.add(patternIterator.next());
		}
		return patterns;
	}

	public static Set<String> getFilteredIds(){
		final IPreferenceStore store = FindbugsPlugin.getDefault().getPreferenceStore();
		String lastUsedFilter = store.getString(FindBugsConstants.LAST_USED_EXPORT_FILTER);
		return FindBugsConstants.decodeIds(lastUsedFilter);
	}

	public static Set<BugPattern> getFilteredPatterns(){
		Iterator<BugPattern> patternIterator = I18N.instance().bugPatternIterator();
		Set<BugPattern> set = new HashSet<BugPattern>();
		Set<String> patternTypes = getFilteredIds();
		while (patternIterator.hasNext()){
			BugPattern next = patternIterator.next();
			String patternId = next.getType();
			if(!patternTypes.contains(patternId)){
				continue;
			}
			set.add(next);
		}
		return set;
	}

	public static Set<BugCode> getFilteredPatternTypes(){
		Iterator<BugCode> patternIterator = I18N.instance().bugCodeIterator();
		Set<BugCode> set = new HashSet<BugCode>();
		Set<String> patternTypes = getFilteredIds();
		while (patternIterator.hasNext()){
			BugCode next = patternIterator.next();
			String type = next.getAbbrev();
			if(!patternTypes.contains(type)){
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


}

