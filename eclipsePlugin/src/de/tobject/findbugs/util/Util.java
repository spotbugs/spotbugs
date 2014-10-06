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

package de.tobject.findbugs.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.annotation.CheckForNull;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;

import edu.umd.cs.findbugs.util.Archive;

/**
 * Eclipse-specific utilities.
 *
 * @author Phil Crosby
 * @author Peter Friese
 * @author Andrei Loskutov
 */
public class Util {

    /**
     * Checks whether the given resource is a Java source file.
     *
     * @param resource
     *            The resource to check.
     * @return <code>true</code> if the given resource is a Java source file,
     *         <code>false</code> otherwise.
     */
    public static boolean isJavaFile(IResource resource) {
        if (resource == null || (resource.getType() != IResource.FILE)) {
            return false;
        }
        String ex = resource.getFileExtension();
        return "java".equalsIgnoreCase(ex); //$NON-NLS-1$
    }

    /**
     * Checks whether the given resource is a Java source file.
     *
     * @param resource
     *            The resource to check.
     * @return <code>true</code> if the given resource is a Java source file,
     *         <code>false</code> otherwise.
     */
    public static boolean isJavaArchive(IResource resource) {
        if (resource == null || (resource.getType() != IResource.FILE)) {
            return false;
        }
        String name = resource.getName();
        return Archive.isArchiveFileName(name);
    }

    /**
     * Checks whether the given resource is a Java class file.
     *
     * @param resource
     *            The resource to check.
     * @return <code>true</code> if the given resource is a class file,
     *         <code>false</code> otherwise.
     */
    public static boolean isClassFile(IResource resource) {
        if (resource == null || (resource.getType() != IResource.FILE)) {
            return false;
        }
        String ex = resource.getFileExtension();
        return "class".equalsIgnoreCase(ex); //$NON-NLS-1$

    }

    /**
     * Checks whether the given java element is a Java class file.
     *
     * @param elt
     *            The resource to check.
     * @return <code>true</code> if the given resource is a class file,
     *         <code>false</code> otherwise.
     */
    public static boolean isClassFile(IJavaElement elt) {
        if (elt == null) {
            return false;
        }
        return elt instanceof IClassFile || elt instanceof ICompilationUnit;

    }

    /**
     * Checks whether the given resource is a Java artifact (i.e. either a Java
     * source file or a Java class file).
     *
     * @param resource
     *            The resource to check.
     * @return <code>true</code> if the given resource is a Java artifact.
     *         <code>false</code> otherwise.
     */
    public static boolean isJavaArtifact(IResource resource) {
        if (resource == null || (resource.getType() != IResource.FILE)) {
            return false;
        }
        String ex = resource.getFileExtension();
        if ("java".equalsIgnoreCase(ex) || "class".equalsIgnoreCase(ex)) {
            return true;
        }
        String name = resource.getName();
        return Archive.isArchiveFileName(name);
    }

    /**
     * A countdown timer which starts to work with the first entry and prints
     * the results ascending with the overall time.
     */
    public static class StopTimer {
        TreeMap<Long, String> stopTimes = new TreeMap<Long, String>();

        public synchronized void newPoint(String name) {
            Long time = Long.valueOf(System.currentTimeMillis());
            if (stopTimes.size() == 0) {
                stopTimes.put(time, name);
                return;
            }
            Long lastTime = stopTimes.lastKey();
            if (time.longValue() <= lastTime.longValue()) {
                time = Long.valueOf(lastTime.longValue() + 1);
            }
            stopTimes.put(time, name);
        }

        public synchronized String getResults() {
            StringBuilder sb = new StringBuilder();
            Iterator<Entry<Long, String>> iterator = stopTimes.entrySet().iterator();
            Entry<Long, String> firstEntry = iterator.next();
            while (iterator.hasNext()) {
                Entry<Long, String> entry = iterator.next();
                long diff = entry.getKey().longValue() - firstEntry.getKey().longValue();
                sb.append(firstEntry.getValue()).append(": ").append(diff).append(" ms\n");
                firstEntry = entry;
            }

            long overall = stopTimes.lastKey().longValue() - stopTimes.firstKey().longValue();
            sb.append("Overall: ").append(overall).append(" ms");
            return sb.toString();
        }
    }

    /**
     * Copies given string to the system clipboard
     *
     * @param content
     *            non null String
     */
    public static void copyToClipboard(String content) {
        if (content == null) {
            return;
        }
        Clipboard cb = null;
        try {
            cb = new Clipboard(Display.getDefault());
            cb.setContents(new String[] { content }, new TextTransfer[] { TextTransfer.getInstance() });
        } finally {
            if (cb != null) {
                cb.dispose();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @CheckForNull
    public static <V> V getAdapter(Class<V> adapter, Object obj) {
        if (obj == null) {
            return null;
        }
        if (adapter.isAssignableFrom(obj.getClass())) {
            return (V) obj;
        }
        if (obj instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) obj;
            return (V) adaptable.getAdapter(adapter);
        }
        return null;
    }

    /**
     * Sorts an array of IMarkers based on their underlying resource name
     * @param markers
     */
    public static void sortIMarkers(IMarker[] markers) {
        Arrays.sort(markers, new Comparator<IMarker>() {
            @Override
            public int compare(IMarker arg0, IMarker arg1) {
                IResource resource0 = arg0.getResource();
                IResource resource1 = arg1.getResource();
                if (resource0 != null && resource1 != null) {
                    return resource0.getName().compareTo(resource1.getName());
                }
                if (resource0 != null && resource1 == null) {
                    return 1;
                }
                if (resource0 == null && resource1 != null) {
                    return -1;
                }
                return 0;
            }
        });
    }

}
