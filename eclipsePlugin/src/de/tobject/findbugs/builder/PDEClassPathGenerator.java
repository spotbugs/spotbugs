/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
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
package de.tobject.findbugs.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.launching.JREContainer;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.build.site.PDEState;
import org.eclipse.pde.internal.core.ClasspathUtilCore;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * Helper class to resolve full classpath for Eclipse plugin projects. Plugin
 * projects are very special for Eclipse and they differ from "usual" java
 * projects...
 *
 * @author Andrei
 */
public class PDEClassPathGenerator {

    /**
     * @param javaProject non null
     * @return never null (may be empty array)
     */
    public static String[] computeClassPath(IJavaProject javaProject) {
        Collection<String> classPath = Collections.EMPTY_SET;
        try {
            // first try to check and resolve plugin project. It can fail if
            // there is no
            // PDE plugins installed in the current Eclipse instance (PDE is
            // optional)
            classPath = createPluginClassPath(javaProject);
        } catch (NoClassDefFoundError ce) {
            // ok, we do not have PDE installed, now try to get default java
            // classpath
            classPath = createJavaClasspath(javaProject);
        }
        return classPath.toArray(new String[classPath.size()]);
    }

    @SuppressWarnings("restriction")
    private static Set<String> createJavaClasspath(IJavaProject javaProject) {
        LinkedHashSet<String> classPath = new LinkedHashSet<>();
        try {
            // doesn't return jre libraries
            String[] defaultClassPath = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
            for (String classpathEntry : defaultClassPath) {
                IPath path = new Path(classpathEntry);
                if (isValidPath(path)) {
                    classPath.add(path.toOSString());
                }
            }
            // add CPE_CONTAINER classpathes
            IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
            for (IClasspathEntry entry : rawClasspath) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                    IClasspathContainer classpathContainer = JavaCore.getClasspathContainer(entry.getPath(), javaProject);
                    if (classpathContainer instanceof JREContainer) {
                        IClasspathEntry[] classpathEntries = classpathContainer.getClasspathEntries();
                        for (IClasspathEntry iClasspathEntry : classpathEntries) {
                            IPath path = iClasspathEntry.getPath();
                            // smallest possible fix for #1228 Eclipse plugin always uses host VM to resolve JDK classes
                            if (isValidPath(path) &&
                                    ("rt.jar".equals(path.lastSegment())
                                            || "jrt-fs.jar".equals(path.lastSegment())
                                            || "jce.jar".equals(path.lastSegment()))) {
                                classPath.add(path.toOSString());
                            }
                        }
                    }
                }
            }
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, "Could not compute aux. classpath for project " + javaProject);
        }
        return classPath;
    }

    /**
     * @param path may be null
     * @return true if the path is considered as valid for the classpath
     */
    private static boolean isValidPath(IPath path) {
        // segmentCount() > 1 is workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=281189
        // classpath contains unlikely one of root directories like /lib/ "as is"
        return path != null && path.segmentCount() > 1 && path.toFile().exists();
    }

    private static Collection<String> createPluginClassPath(IJavaProject javaProject) {
        Set<String> javaClassPath = createJavaClasspath(javaProject);
        IPluginModelBase model = PluginRegistry.findModel(javaProject.getProject());
        if (model == null || model.getPluginBase().getId() == null) {
            return javaClassPath;
        }
        BundleDescription target = model.getBundleDescription();

        // target is null if plugin uses non OSGI format
        if (target == null) {
            return javaClassPath;
        }
        List<String> pdeClassPath = new ArrayList<>(javaClassPath);
        Set<BundleDescription> bundles = new HashSet<>();
        addDependentBundles(target, bundles);
        for (BundleDescription bd : bundles) {
            appendBundleToClasspath(bd, pdeClassPath);
        }
        return pdeClassPath;
    }

    private static void appendBundleToClasspath(BundleDescription bd, List<String> pdeClassPath) {
        IPluginModelBase model = PluginRegistry.findModel(bd);
        if (model == null) {
            return;
        }
        ArrayList<IClasspathEntry> classpathEntries = new ArrayList<>();
        ClasspathUtilCore.addLibraries(model, classpathEntries);

        for (IClasspathEntry cpe : classpathEntries) {
            IPath location = null;
            if (cpe.getEntryKind() != IClasspathEntry.CPE_SOURCE) {
                location = cpe.getPath();
            }
            if (location == null) {
                continue;
            }
            String locationStr = location.toOSString();
            if (pdeClassPath.contains(locationStr)) {
                continue;
            }
            // extra cleanup for some directories on classpath
            String bundleLocation = bd.getLocation();
            if (bundleLocation != null && !"jar".equals(location.getFileExtension())
                    && new File(bundleLocation).isDirectory()
                    && bd.getSymbolicName().equals(location.lastSegment())) {
                // ignore badly resolved plugin directories inside workspace
                // ("." as classpath is resolved as plugin root directory)
                // which is, if under workspace, NOT a part of the classpath
                continue;
            }
            if (!location.isAbsolute()) {
                location = ResourceUtils.relativeToAbsolute(location);
            }
            if (!isValidPath(location)) {
                continue;
            }
            locationStr = location.toOSString();
            if (!pdeClassPath.contains(locationStr)) {
                pdeClassPath.add(locationStr);
            }
        }
    }

    private static void addDependentBundles(BundleDescription bd, Set<BundleDescription> bundles) {
        // TODO for some reasons, this does not add "native" fragments for the
        // platform. See also: ContributedClasspathEntriesEntry, RequiredPluginsClasspathContainer
        // BundleDescription[] requires = PDEState.getDependentBundles(target);
        BundleDescription[] bundles2 = PDEState.getDependentBundlesWithFragments(bd);
        for (BundleDescription bundleDescription : bundles2) {
            if (bundleDescription == null) {
                continue;
            }
            if (!bundles.contains(bundleDescription)) {
                bundles.add(bundleDescription);
                addDependentBundles(bundleDescription, bundles);
            }
        }
    }

}
