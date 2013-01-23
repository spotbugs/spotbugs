/*
 * Contributions to FindBugs
 * Copyright (C) 2010-2012, Andrey Loskutov
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
package de.tobject.findbugs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import de.tobject.findbugs.io.IO;

/**
 * Helper class to read contributions for the "detectorPlugins" extension point
 */
public class DetectorsExtensionHelper {

    private static final String DEFAULT_USE_PLUGIN_JAR = ".";

    private static final String EXTENSION_POINT_ID = FindbugsPlugin.PLUGIN_ID + ".findbugsPlugins";

    private static final String LIBRARY_PATH = "libraryPath";
    private static final String PLUGIN_ID = "fbPluginId";

    /** key is the plugin id, value is the plugin library path */
    private static SortedMap<String, String> contributedDetectors;

    /** key is the plugin id, value is the plugin library path */
    public static synchronized SortedMap<String, String> getContributedDetectors() {
        if (contributedDetectors != null) {
            return new TreeMap<String, String>(contributedDetectors);
        }
        TreeMap<String, String> set = new TreeMap<String, String>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXTENSION_POINT_ID);
        if (point == null) {
            return set;
        }
        IExtension[] extensions = point.getExtensions();
        for (IExtension extension : extensions) {
            IConfigurationElement[] elements = extension.getConfigurationElements();
            for (IConfigurationElement configElt : elements) {
                addContribution(set, configElt);
            }
        }
        contributedDetectors = set;
        return new TreeMap<String, String>(contributedDetectors);
    }

    private static void addContribution(TreeMap<String, String> set, IConfigurationElement configElt) {
        String libPathAsString;
        String pluginId;
        IContributor contributor = null;
        try {
            contributor = configElt.getContributor();
            if (contributor == null) {
                throw new IllegalArgumentException("Null contributor");
            }
            pluginId = configElt.getAttribute(PLUGIN_ID);
            if (pluginId == null) {
                throw new IllegalArgumentException("Missing '" + PLUGIN_ID + "'");
            }
            libPathAsString = configElt.getAttribute(LIBRARY_PATH);
            if (libPathAsString == null) {
                throw new IllegalArgumentException("Missing '" + LIBRARY_PATH + "'");
            }
            libPathAsString = resolveRelativePath(contributor, libPathAsString);
            if (libPathAsString == null) {
                throw new IllegalArgumentException("Failed to resolve library path for: " + pluginId);
            }
            if(set.containsKey(pluginId)) {
                throw new IllegalArgumentException("Duplicated '" + pluginId + "' contribution.");
            }
            set.put(pluginId, libPathAsString);
        } catch (Throwable e) {
            String cName = contributor != null ? contributor.getName() : "unknown contributor";
            String message = "Failed to read contribution for '" + EXTENSION_POINT_ID
                    + "' extension point from " + cName;
            FindbugsPlugin.getDefault().logException(e, message);
        }
    }

    /**
     *
     * @param contributor
     *            non null
     * @param libPathAsString
     *            non null
     * @return resolved absolute path for the detector package
     */
    @CheckForNull
    private static String resolveRelativePath(IContributor contributor, String libPathAsString) {
        String bundleName = contributor.getName();
        Bundle bundle = Platform.getBundle(bundleName);
        if (bundle == null) {
            return null;
        }
        File bundleFile;
        try {
            bundleFile = FileLocator.getBundleFile(bundle);
        } catch (IOException e) {
            FindbugsPlugin.getDefault().logException(e, "Failed to resolve detector library for " + bundle.getSymbolicName());
            return null;
        }
        boolean runningInDebugger = Boolean.getBoolean("eclipse.pde.launch");
        if (!DEFAULT_USE_PLUGIN_JAR.equals(libPathAsString)) {
            return new Path(bundleFile.getAbsolutePath()).append(libPathAsString).toOSString();
        }
        if (!bundleFile.isDirectory()) {
            return bundleFile.getAbsolutePath();
        }
        if (runningInDebugger) {
            // in case we are inside debugger & see bundle as directory
            return resolvePluginClassesDir(bundleName, bundleFile);
        }

        // it's a directory, and we are in the production environment.
        IllegalArgumentException e = new IllegalArgumentException("Failed to resolve detector library for "
                + bundle.getSymbolicName());
        String message = "Failed to resolve detector library. '" + bundleFile
                + "' is a directory and can't be used as FindBugs detector package." + " Please specify '" + LIBRARY_PATH
                + "' argument as a relative path to the detectors jar file.";
        FindbugsPlugin.getDefault().logException(e, message);
        return null;
    }

    /**
     * Used for Eclipse instances running inside debugger. During development Eclipse plugins
     * are just directories. The code below tries to locate plugin's
     * "bin" directory. It doesn't work if the plugin build.properties are not
     * existing or contain invalid content
     */
    @CheckForNull
    private static String resolvePluginClassesDir(String bundleName, File sourceDir) {
         if (sourceDir.listFiles() == null) {
            FindbugsPlugin.getDefault().logException(new IllegalStateException("No files in the bundle!"),
                    "Failed to create temporary detector package for bundle " + sourceDir);
            return null;
        }

        String outputDir = getBuildDirectory(bundleName, sourceDir);
        if (outputDir.length() == 0) {
            FindbugsPlugin.getDefault().logException(new IllegalStateException("No output directory in build.properties"),
                    "No output directory in build.properties " + sourceDir);
            return null;
        }

        File classDir = new File(sourceDir, outputDir);

        if (classDir.listFiles() == null) {
            FindbugsPlugin.getDefault().logException(new IllegalStateException("No files in the bundle output dir!"),
                    "Failed to create temporary detector package for bundle " + sourceDir);
            return null;
        }
        File etcDir = new File(sourceDir, "etc");
        if (etcDir.listFiles() == null) {
            FindbugsPlugin.getDefault().logException(new IllegalStateException("No files in the bundle etc dir!"),
                    "Failed to create temporary detector package for bundle " + sourceDir);
            return null;
        }
        return classDir.getAbsolutePath();
    }

    /**
     * @return possible deployment root directory of a plugin project
     */
    @Nonnull
    private static String getBuildDirectory(String bundleName, File sourceDir) {
        Properties props = new Properties();
        File buildProps = new File(sourceDir, "build.properties");
        if (buildProps.isFile()) {
            FileInputStream inStream = null;
            try {
                inStream = new FileInputStream(buildProps);
                props.load(inStream);
            } catch (IOException e) {
                FindbugsPlugin.getDefault().logException(e, "Failed to read build.properties for bundle " + bundleName);
            } finally {
                IO.closeQuietly(inStream);
            }
        }
        // this works only for plugins which are self-contained and do not
        // include
        // any external libraries
        return props.getProperty("output..", "");
    }

}
