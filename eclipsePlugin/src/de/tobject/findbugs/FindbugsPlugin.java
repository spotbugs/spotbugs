/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
 * Copyright (C) 2004, University of Maryland
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

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.dom4j.DocumentException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.tobject.findbugs.builder.AbstractFilesCollector;
import de.tobject.findbugs.builder.FilesCollectorFactory;
import de.tobject.findbugs.builder.FindBugsBuilder;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.nature.FindBugsNature;
import de.tobject.findbugs.reporter.Reporter;
import de.tobject.findbugs.view.DetailsView;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.config.ProjectFilterSettings;

/**
 * The main plugin class to be used in the desktop.
 */
public class FindbugsPlugin extends AbstractUIPlugin {
	/** Controls debugging of the plugin */
	public static boolean DEBUG;
	
	/**
	 * The plug-in identifier of the FindBugs Plug-in
	 * (value <code>"de.tobject.findbugs"</code>).
	 */
	public static final String PLUGIN_ID = "de.tobject.findbugs"; //$NON-NLS-1$
	
	/**
	 * The identifier for the FindBugs builder
	 * (value <code>"de.tobject.findbugs.findbugsbuilder"</code>).
	 */
	public static final String BUILDER_ID = PLUGIN_ID + ".findbugsBuilder"; //$NON-NLS-1$
	
	/**
	 * The identifier for the FindBugs nature
	 * (value <code>"de.tobject.findbugs.findbugsnature"</code>).
	 *
	 * @see org.eclipse.core.resources.IProject#hasNature(java.lang.String)
	 */
	public static final String NATURE_ID = PLUGIN_ID + ".findbugsNature"; //$NON-NLS-1$
	
	// Debugging options
	private static final String PLUGIN_DEBUG = PLUGIN_ID + "/debug/plugin"; //$NON-NLS-1$
	private static final String WORKER_DEBUG = PLUGIN_ID + "/debug/worker"; //$NON-NLS-1$
	private static final String BUILDER_DEBUG = PLUGIN_ID + "/debug/builder"; //$NON-NLS-1$
	private static final String MARKER_DEBUG = PLUGIN_ID + "/debug/marker"; //$NON-NLS-1$
	private static final String NATURE_DEBUG = PLUGIN_ID + "/debug/nature"; //$NON-NLS-1$
	private static final String PROPERTIES_DEBUG = PLUGIN_ID + "/debug/properties"; //$NON-NLS-1$
	private static final String REPORTER_DEBUG = PLUGIN_ID + "/debug/reporter"; //$NON-NLS-1$
	private static final String UTIL_DEBUG = PLUGIN_ID + "/debug/util"; //$NON-NLS-1$
	private static final String VISITOR_DEBUG = PLUGIN_ID + "/debug/visitor"; //$NON-NLS-1$
	public static final QualifiedName PERSISTENT_PROPERTY_ACTIVE_DETECTORS = new QualifiedName(FindbugsPlugin.PLUGIN_ID + ".persprops", "detectors.active"); //$NON-NLS-1$//$NON-NLS-2$
	public static final QualifiedName SESSION_PROPERTY_ACTIVE_DETECTORS = new QualifiedName(FindbugsPlugin.PLUGIN_ID + ".sessionprops", "detectors.active"); //$NON-NLS-1$//$NON-NLS-2$
	public static final QualifiedName PERSISTENT_PROPERTY_FILTER_SETTINGS =
		new QualifiedName(FindbugsPlugin.PLUGIN_ID + ".persprops", "filter.settings");
	public static final QualifiedName SESSION_PROPERTY_FILTER_SETTINGS =
		new QualifiedName(FindbugsPlugin.PLUGIN_ID + ".sessionprops", "filter.settings");
	public static final QualifiedName SESSION_PROPERTY_BUG_COLLECTION =
		new QualifiedName(FindbugsPlugin.PLUGIN_ID + ".sessionprops", "bugcollection");
	public static final QualifiedName SESSION_PROPERTY_FB_PROJECT =
		new QualifiedName(FindbugsPlugin.PLUGIN_ID + ".sessionprops", "fbproject");
	public static final String LIST_DELIMITER = ";"; //$NON-NLS-1$
	
	
	/** The shared instance. */
	private static FindbugsPlugin plugin;
	
	/** Details view instance */
	private static DetailsView viewDetails;
	
	/** Resource bundle. */
	private ResourceBundle resourceBundle;

	/**
	 * Constructor.
	 */
	public FindbugsPlugin() {
		plugin = this;
		
		// configure debugging
		configurePluginDebugOptions();
		
		// initialize resource strings
		try {
			resourceBundle = ResourceBundle.getBundle("de.tobject.findbugs.FindbugsPluginResources"); //$NON-NLS-1$
		}
		catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		// TODO hardcore workaround for findbugs home property
		// - see de.tobject.findbugs.builder.FindBugsWorker.work() too
		String findBugsHome = getFindBugsEnginePluginLocation();
		if (DEBUG) {
			System.out.println("Looking for detecors in: " + findBugsHome);
		}
		System.setProperty("findbugs.home", findBugsHome);
	}
	
	/**
	 * Returns the shared instance.
	 */
	public static FindbugsPlugin getDefault() {
		return plugin;
	}
	
	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
	
	public static IWorkbench getActiveWorkbench() {
		FindbugsPlugin plugin = getDefault();
		if (plugin == null) {
			return null;
		}
		return plugin.getWorkbench();
	}
	
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		IWorkbench workbench = getActiveWorkbench();
		if (workbench == null) {
			return null;
		}
		return workbench.getActiveWorkbenchWindow();
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

	public void configurePluginDebugOptions() {
		if (isDebugging()) {
			// debugging for the plugin itself
			String option = Platform.getDebugOption(PLUGIN_DEBUG);
			if (option != null) {
				FindbugsPlugin.DEBUG = option.equalsIgnoreCase("true"); //$NON-NLS-1$
			}

			// debugging for the builder and friends
			option = Platform.getDebugOption(BUILDER_DEBUG);
			if (option != null) {
				FindBugsBuilder.DEBUG = option.equalsIgnoreCase("true"); //$NON-NLS-1$
				AbstractFilesCollector.DEBUG = FindBugsBuilder.DEBUG;
				FilesCollectorFactory.DEBUG = FindBugsBuilder.DEBUG;
				FindBugsWorker.DEBUG = FindBugsBuilder.DEBUG;
			}

			// debugging for the nature
			option = Platform.getDebugOption(NATURE_DEBUG);
			if (option != null) {
				FindBugsNature.DEBUG = option.equalsIgnoreCase("true"); //$NON-NLS-1$
			}

			// debugging for the reporter
			option = Platform.getDebugOption(REPORTER_DEBUG);
			if (option != null) {
				Reporter.DEBUG = option.equalsIgnoreCase("true"); //$NON-NLS-1$
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
		URL u = plugin.find(new Path("."));
		try {
			URL u2 = Platform.resolve(u);
			String pluginPath = u2.getPath();
			if (FindBugsBuilder.DEBUG) {
				System.out.println("Pluginpath: " + pluginPath); //$NON-NLS-1$
			}
			return pluginPath;
		} catch(RuntimeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (FindBugsBuilder.DEBUG) {
			System.out.println("Could not find findbugs binaries."); //$NON-NLS-1$
		}
		return null;
	}
	
	/**
	 * @param key
	 * @return
	 */
	public String getMessage(String key) {
		// TODO implement me!
		return key;
	}

	/**
	 * Log an exception.
	 * 
	 * @param e       the exception
	 * @param message message describing how/why the exception occurred
	 */
	public void logException(Exception e, String message) {
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

	private void logMessage(int severity, String message, Exception e) {
		if (FindbugsPlugin.DEBUG) {
			String what = (severity == IStatus.ERROR)
				? (e != null ? "Exception" : "Error")
				: "Warning";
			System.err.println(what + " in FindBugs plugin: " + message);
			if (e != null)
				e.printStackTrace();
		}
		IStatus status = new Status(severity, FindbugsPlugin.PLUGIN_ID, 0, message, e);
		getLog().log(status);
	}
	
	/**
	 * Get ProjectFilterSettings for given project.
	 * If no settings exist yet, default settings are created.
	 * 
	 * @param project the project
	 * @return the ProjectFilterSettings for the project
	 */
	public static ProjectFilterSettings getProjectFilterSettings(IProject project) throws CoreException {
		ProjectFilterSettings settings = (ProjectFilterSettings)
			project.getSessionProperty(SESSION_PROPERTY_FILTER_SETTINGS);
		if (settings == null) {
			String savedSettings = project.getPersistentProperty(PERSISTENT_PROPERTY_FILTER_SETTINGS);
			if (savedSettings == null) {
				settings = ProjectFilterSettings.createDefault();
				project.setPersistentProperty(PERSISTENT_PROPERTY_FILTER_SETTINGS, settings.toEncodedString());
			} else {
				settings = ProjectFilterSettings.fromEncodedString(savedSettings);
			}
			project.setSessionProperty(SESSION_PROPERTY_FILTER_SETTINGS, settings);
		}
		return settings;
	}
	
	/**
	 * Store project filter settings.
	 * 
	 * @param project  the project
	 * @param settings the project filter settings
	 * @throws CoreException
	 */
	public static void storeProjectFilterSettings(IProject project, ProjectFilterSettings settings) throws CoreException {
		project.setSessionProperty(SESSION_PROPERTY_FILTER_SETTINGS, settings);
		project.setPersistentProperty(PERSISTENT_PROPERTY_FILTER_SETTINGS, settings.toEncodedString());
	}
	
	/**
	 * Get the file resource used to store findbugs warnings for a project.
	 * 
	 * @param project the project
	 * @return the IFile (which may not actually exist in the filesystem yet)
	 */
	public static IFile getBugCollectionFile(IProject project) {
		IFile file = project.getFile(".fbwarnings");
		return file;
	}

	/**
	 * Read stored BugCollection for project.
	 * Returns null if there is no stored bug collection for the project.
	 * 
	 * @param project the eclipse project
	 * @param monitor a progress monitor
	 * @return the stored BugCollection, or null if there is no bug collection
	 * @throws CoreException
	 * @throws IOException
	 * @throws DocumentException
	 */
	public static SortedBugCollection readBugCollection(
			IProject project, IProgressMonitor monitor)
			throws CoreException, IOException, DocumentException {
		SortedBugCollection bugCollection = (SortedBugCollection) project.getSessionProperty(
				SESSION_PROPERTY_BUG_COLLECTION);
		if (bugCollection == null) {
			readBugCollectionAndProject(project, monitor);
			bugCollection = (SortedBugCollection) project.getSessionProperty(
					SESSION_PROPERTY_BUG_COLLECTION);
		}
		return bugCollection;
	}
	
	/**
	 * Read stored findbugs Project for a project.
	 * Returns null if there is no stored project.
	 * 
	 * @param project the eclipse project
	 * @param monitor a progress monitor
	 * @return the saved findbugs Project, or null if there is no saved project
	 * @throws CoreException
	 * @throws DocumentException
	 * @throws DocumentException
	 * @throws IOException
	 */
	public static Project readProject(IProject project, IProgressMonitor monitor)
			throws CoreException, DocumentException, DocumentException, IOException {
		Project findbugsProject = (Project) project.getSessionProperty(
				SESSION_PROPERTY_FB_PROJECT);
		if (findbugsProject == null) {
			readBugCollectionAndProject(project, monitor);
			findbugsProject =  (Project) project.getSessionProperty(
					SESSION_PROPERTY_FB_PROJECT);
		}
		return findbugsProject;
	}

	/**
	 * Read saved bug collection and findbugs project from file.
	 * Will populate the bug collection and findbugs project session
	 * properties.  If there is no saved bug collection and project
	 * for the eclipse project, then the session properties will
	 * be set to null.
	 * 
	 * @param project the eclipse project
	 * @param monitor a progress monitor
	 * @throws IOException
	 * @throws DocumentException
	 * @throws CoreException
	 */
	private static void readBugCollectionAndProject(IProject project, IProgressMonitor monitor) throws IOException, DocumentException, CoreException {
		SortedBugCollection bugCollection;
		Project findbugsProject;
		
		IFile bugCollectionFile = getBugCollectionFile(project);
		if (bugCollectionFile.exists()) {
			bugCollection = new SortedBugCollection();
			findbugsProject = new Project();

			// FIXME: use progress monitor
			bugCollection.readXML(bugCollectionFile.getContents(), findbugsProject);
		} else {
			bugCollection = null;
			findbugsProject = null;
		}

		project.setSessionProperty(SESSION_PROPERTY_BUG_COLLECTION, bugCollection);
		project.setSessionProperty(SESSION_PROPERTY_FB_PROJECT, findbugsProject);
	}
	
	/**
	 * Store a new bug collection for a project.
	 * The collection is stored in the session, and also in
	 * a file in the project.
	 * 
	 * @param project         the project
	 * @param bugCollection   the bug collection
	 * @param findbugsProject the FindBugs Project object
	 * @param monitor         progress monitor
	 * @throws IOException
	 * @throws CoreException
	 */
	public static void storeBugCollection(
			IProject project,
			final SortedBugCollection bugCollection,
			final Project findbugsProject,
			IProgressMonitor monitor) throws IOException, CoreException {
		
		// Store the bug collection and findbugs project in the session
		project.setSessionProperty(SESSION_PROPERTY_BUG_COLLECTION, bugCollection);
		project.setSessionProperty(SESSION_PROPERTY_FB_PROJECT, findbugsProject);

		// Save to file
		IFile bugCollectionFile = FindbugsPlugin.getBugCollectionFile(project);
		
		PipedInputStream pin = new PipedInputStream();
		final PipedOutputStream pout = new PipedOutputStream();
		
		// Create a thread to write bug collection to output stream
		Thread worker = new Thread() {
			public void run() {
				try {
					bugCollection.writeXML(pout, findbugsProject);
				} catch (IOException e) {
					FindbugsPlugin.getDefault().logException(e, "Exception while creating XML bug description file.");
				} finally {
					try {
						pout.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
		};		
		
		try {
			pin.connect(pout);
			worker.start();			
			
			if (!bugCollectionFile.exists())
				bugCollectionFile.create(pin, true, monitor);
			else
				bugCollectionFile.setContents(pin, true, false, monitor);
			
			// Need to refresh here?
			bugCollectionFile.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		} finally {
			try {
				pin.close();
			} catch (IOException e) {
				// ignore
			}
		}
		
	}
	
}

