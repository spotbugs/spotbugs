/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.tobject.findbugs.builder.AbstractFilesCollector;
import de.tobject.findbugs.builder.FilesCollectorFactory;
import de.tobject.findbugs.builder.FindBugsBuilder;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.nature.FindBugsNature;
import de.tobject.findbugs.reporter.Reporter;
import de.tobject.findbugs.view.DetailsView;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;

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
	public static final String LIST_DELIMITER = ";"; //$NON-NLS-1$
	
	/** The shared instance. */
	private static FindbugsPlugin plugin;
	
	/** Details view instance */
	private static DetailsView viewDetails;
	
	/** Resource bundle. */
	private ResourceBundle resourceBundle;
	
	/**
	 * The constructor.
	 */
	public FindbugsPlugin(IPluginDescriptor descriptor) {
		// basic initialization
		super(descriptor);
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
	
	public static String getFindBugsEnginePluginLocation() {
		// URL u = FindbugsPlugin.getDefault().getDescriptor().getInstallURL();
		Plugin plugin = Platform.getPlugin("edu.umd.cs.findbugs");
		if (plugin != null) {
			if (FindbugsPlugin.DEBUG) {
				System.out.println("Found the findbugs binaries."); //$NON-NLS-1$
			}
			URL u = plugin.getDescriptor().getInstallURL(); //$NON-NLS-1$  // .getBundle().getEntry("/");
			try {
				// this gets a file://... url for the plugin
				URL u2 = Platform.resolve(u);
				// convert to real path
				String pluginPath = u2.getPath();
				if (FindBugsBuilder.DEBUG) {
					System.out.println("Pluginpath: " + pluginPath); //$NON-NLS-1$
				}
				return pluginPath;
			}
			catch (RuntimeException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
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
	 * Get a sub ruleset from a rule list
	 * TODO move this to a more sensible location!
	 */
	public static List getDetectorFactoriesFromProperty(String ruleList) {
		ArrayList factoryList = new ArrayList();
		StringTokenizer st = new StringTokenizer(ruleList, LIST_DELIMITER);
		while (st.hasMoreTokens()) {
			try {
				DetectorFactory factory =
					DetectorFactoryCollection.instance().getFactory(st.nextToken());
				if (factory != null) {
					factoryList.add(factory);
				}
			}
			catch (RuntimeException e) {
				// TODO exception handling
				e.printStackTrace();
			}
		}
		return factoryList;
	}
	
	/*
	 * TODO move this to a more sensible location!
	 */	
	public static List readDetectorFactories(IProject project)
		throws CoreException {
		List factoryList =
			(List) project.getSessionProperty(SESSION_PROPERTY_ACTIVE_DETECTORS);
		if (factoryList == null) {
			String activeDetectorList =
				project.getPersistentProperty(PERSISTENT_PROPERTY_ACTIVE_DETECTORS);
			if (activeDetectorList != null) {
				factoryList = getDetectorFactoriesFromProperty(activeDetectorList);
			}
			else {
				factoryList = new ArrayList();
				Iterator iterator =
					DetectorFactoryCollection.instance().factoryIterator();
				while (iterator.hasNext()) {
					DetectorFactory factory = (DetectorFactory) iterator.next();
					if (factory.isEnabled())
						factoryList.add(factory);
				}
			}
			project.setSessionProperty(
				PERSISTENT_PROPERTY_ACTIVE_DETECTORS,
				factoryList);
		}
		return factoryList;
	}
	
}

