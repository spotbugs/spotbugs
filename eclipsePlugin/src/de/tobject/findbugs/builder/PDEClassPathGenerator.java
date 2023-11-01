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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
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

        // try to get default java classpath
        classPath = createJavaClasspath(javaProject);

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
                    if (classpathContainer != null) {
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

}
