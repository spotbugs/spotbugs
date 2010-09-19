/*
 * Contributions to FindBugs
 * Copyright (C) 2010, Andrei Loskutov
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
 *
 * @author andrei
 */
public class DetectorsExtensionHelper {

    private static final String DEFAULT_USE_PLUGIN_JAR = ".";
    private static final String EXTENSION_POINT_ID = FindbugsPlugin.PLUGIN_ID
            + ".detectorPlugins";
	private static final String LIBRARY_PATH = "libraryPath";
    private static SortedSet<String> contributedDetectors;

    public static synchronized SortedSet<String> getContributedDetectors() {
        if (contributedDetectors != null) {
            return contributedDetectors;
		}
        TreeSet<String> set = new TreeSet<String>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXTENSION_POINT_ID);
        if (point == null) {
			return set;
        }
        IExtension[] extensions = point.getExtensions();
        for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension.getConfigurationElements();
            for (IConfigurationElement configElt : elements) {
                String libPathAsString;
                IContributor contributor = null;
				try {
                    contributor = configElt.getContributor();
                    libPathAsString = configElt.getAttribute(LIBRARY_PATH);
                    if (libPathAsString == null || contributor == null) {
						throw new IllegalArgumentException("Null argument");
                    }
                    libPathAsString = resolveRelativePath(contributor, libPathAsString);
                    if (libPathAsString == null) {
						throw new IllegalArgumentException(
                                "Failed to resolve library path: " + libPathAsString);
                    }
                    set.add(libPathAsString);
				} catch (Throwable e) {
                    String cName = contributor != null ? contributor.getName()
                            : "unknown contributor";
                    String message = "Failed to read '" + LIBRARY_PATH
							+ "' attribute for '" + EXTENSION_POINT_ID
                            + "' extension point from " + cName;
                    FindbugsPlugin.getDefault().logException(e, message);
                    continue;
				}

            }
        }
        contributedDetectors = set;
		return contributedDetectors;
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
    private static String resolveRelativePath(IContributor contributor,
            String libPathAsString) {
		String bundleName = contributor.getName();
        Bundle bundle = Platform.getBundle(bundleName);
        if (bundle == null) {
            return null;
		}
        File bundleFile;
        try {
            bundleFile = FileLocator.getBundleFile(bundle);
		} catch (IOException e) {
            FindbugsPlugin.getDefault().logException(e,
                    "Failed to resolve detector library for " + bundle.getSymbolicName());
            return null;
		}
        boolean runningInDebugger = Boolean.getBoolean("eclipse.pde.launch");
        if (!DEFAULT_USE_PLUGIN_JAR.equals(libPathAsString)) {
            return new Path(bundleFile.getAbsolutePath()).append(libPathAsString)
					.toOSString();
        }
        if (!bundleFile.isDirectory()) {
            return bundleFile.getAbsolutePath();
		}
        if (runningInDebugger) {
            // in case we are inside debugger & see bundle as directory
            return createTemporaryJar(bundleName, bundleFile);
		}

        // it's a directory, and we are in the production environment.
        IllegalArgumentException e = new IllegalArgumentException(
                "Failed to resolve detector library for " + bundle.getSymbolicName());
		String message = "Failed to resolve detector library. '" + bundleFile
                + "' is a directory and can't be used as FindBugs detector package."
                + " Please specify '" + LIBRARY_PATH
                + "' argument as a relative path to the detectors jar file.";
		FindbugsPlugin.getDefault().logException(e, message);
        return null;
    }

    /**
     * Used for Eclipse instances running inside debugger. FindBugs expects to see *.jar
     * files, but during development Eclipse plugins are just directories. The code below
	 * makes a temporary jar file from the plugin's "bin" directory. It doesn't work if
     * the plugin build.properties are not existing or contain invalid content.
     *
     *
	 * @param bundleName
     * @param sourceDir
     * @return
     */
	@CheckForNull
    private static String createTemporaryJar(String bundleName, File sourceDir) {
        File jarFile;
        try {
			jarFile = File.createTempFile(bundleName + "_", ".jar");
            jarFile.deleteOnExit();
        } catch (IOException e) {
            FindbugsPlugin.getDefault().logException(
					e,
                    "Failed to create temporary detector package for bundle "
                            + bundleName);
            return null;
		}

        String outputDir = getBuildDirectory(bundleName, sourceDir);
        if (outputDir.length() != 0) {
            sourceDir = new File(sourceDir, outputDir);
		}

        ZipOutputStream jar = null;
        try {
            jar = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(
					jarFile)));
            addFiles(bundleName, sourceDir, sourceDir, jar);
        } catch (IOException e) {
            FindbugsPlugin.getDefault().logException(
					e,
                    "Failed to create temporary detector package for bundle "
                            + bundleName);
            return null;
		} finally {
            IO.closeQuietly(jar);
        }
        return jarFile.getAbsolutePath();
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
                FindbugsPlugin.getDefault().logException(e,
                        "Failed to read build.properties for bundle " + bundleName);
			} finally {
                IO.closeQuietly(inStream);
            }
        }
		// this works only for plugins which are self-contained and do not include
        // any external libraries
        return props.getProperty("output..", "");
    }

    private static void addFiles(String name, File sourceDir, File root,
            ZipOutputStream zip) throws IOException {
        File[] files = sourceDir.listFiles();
		if (files == null) {
            FindbugsPlugin.getDefault().logException(
                    new IllegalStateException("No files in the bundle!"),
                    "Failed to create temporary detector package for bundle " + name);
			return;
        }
        byte[] buffer = new byte[1024];
        int bytesRead;
		int prefixLength = root.getAbsolutePath().length() + 1;
        for (File file : files) {
            boolean directory = file.isDirectory();
            if (directory) {
				String dirName = file.getName();
                if (dirName.equalsIgnoreCase(".svn") || dirName.equalsIgnoreCase(".cvs")
                        || dirName.equalsIgnoreCase(".hg")) {
                    continue;
				}
                addFiles(name, file, root, zip);
            } else {
                String fileName = file.getAbsolutePath().substring(prefixLength);
				ZipEntry entry = new ZipEntry(fileName);
                entry.setMethod(ZipEntry.DEFLATED);
                zip.putNextEntry(entry);
                BufferedInputStream bis = null;

                try {
                    bis = new BufferedInputStream(new FileInputStream(file));
                    entry.setSize(file.length());
					while ((bytesRead = bis.read(buffer)) != -1) {
                        zip.write(buffer, 0, bytesRead);
                    }
                } finally {
					IO.closeQuietly(bis);
                }
            }
        }
	}
}
