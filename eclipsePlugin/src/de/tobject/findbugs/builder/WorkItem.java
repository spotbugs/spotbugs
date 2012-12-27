/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Andrei Loskutov
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.util.Util;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.util.Archive;

/**
 * An item to work on for FB analysis - this can be entire project or single
 * file or a java element without corresponding resource (like external library)
 *
 * @author Andrei
 */
public class WorkItem {

    private final IJavaElement javaElt;

    private final IResource resource;

    private final IProject project;

    public WorkItem(IJavaElement javaElt) {
        this(null, javaElt, javaElt.getJavaProject().getProject());
    }

    public WorkItem(IResource resource) {
        this(resource, null, resource.getProject());
    }

    private WorkItem(IResource resource, IJavaElement javaElt, IProject project) {
        this.resource = resource;
        this.javaElt = javaElt;
        this.project = project;
        Assert.isLegal(resource != null || javaElt != null);
    }

    public void addFilesToProject(Project fbProject, Map<IPath, IPath> outputLocations) {
        IResource res = getCorespondingResource();
        if (res instanceof IProject) {
            for (IPath outDir : outputLocations.values()) {
                fbProject.addFile(outDir.toOSString());
            }
        } else if (res instanceof IFolder) {
            // assumption: this is a source folder.
            boolean added = addClassesForFolder((IFolder) res, outputLocations, fbProject);
            if (!added) {
                // What if this is a class folder???
                addJavaElementPath(fbProject);
            }
        } else if (res instanceof IFile) {
            // ID: 2734173: allow to analyze classes inside archives
            if (Util.isClassFile(res) || Util.isJavaArchive(res)) {
                fbProject.addFile(res.getLocation().toOSString());
            } else if (Util.isJavaFile(res)) {
                addClassesForFile((IFile) res, outputLocations, fbProject);
            }
        } else {
            addJavaElementPath(fbProject);
        }
    }

    private void addJavaElementPath(Project fbProject) {
        if (javaElt != null) {
            IPath path = getPath();
            if (path != null) {
                fbProject.addFile(path.toOSString());
            }
        }
    }

    public void clearMarkers() throws CoreException {
        IResource res = getMarkerTarget();
        if (javaElt == null || !(res instanceof IProject)) {
            MarkerUtil.removeMarkers(res);
        } else {
            // this is the case of external class folders/libraries: if we would
            // cleanup ALL project markers, it would also remove markers from
            // ALL
            // source/class files, not only for the selected one.
            IMarker[] allMarkers = MarkerUtil.getAllMarkers(res);
            Set<IMarker> set = MarkerUtil.findMarkerForJavaElement(javaElt, allMarkers, true);
            // TODO can be very slow. May think about batch operation w/o
            // resource notifications
            // P.S. if executing "clean+build", package explorer first doesn't
            // notice
            // any change because the external class file labels are not
            // refreshed after clean,
            // but after build we trigger an explicit view refresh, see
            // FindBugsAction
            for (IMarker marker : set) {
                marker.delete();
            }
        }
    }

    public IProject getProject() {
        return project;
    }

    public IJavaProject getJavaProject() {
        return JavaCore.create(project);
    }

    /**
     * @return false if no classes was added
     */
    private static boolean addClassesForFolder(IFolder folder, Map<IPath, IPath> outLocations, Project fbProject) {
        IPath path = folder.getLocation();
        IPath srcRoot = getMatchingSourceRoot(path, outLocations);
        if (srcRoot == null) {
            return false;
        }
        IPath outputRoot = outLocations.get(srcRoot);
        int firstSegments = path.matchingFirstSegments(srcRoot);
        // add relative path to the output path
        IPath out = outputRoot.append(path.removeFirstSegments(firstSegments));
        File directory = out.toFile();
        return fbProject.addFile(directory.getAbsolutePath());
        // TODO child directories too. Should add preference???
    }

    private static void addClassesForFile(IFile file, Map<IPath, IPath> outLocations, Project fbProject) {
        IPath path = file.getLocation();
        IPath srcRoot = getMatchingSourceRoot(path, outLocations);
        if (srcRoot == null) {
            return;
        }
        IPath outputRoot = outLocations.get(srcRoot);
        int firstSegments = path.matchingFirstSegments(srcRoot);
        // add relative path to the output path
        IPath out = outputRoot.append(path.removeFirstSegments(firstSegments));
        String fileName = path.removeFileExtension().lastSegment();
        String namePattern = fileName + "\\.class|" + fileName + "\\$.*\\.class";
        namePattern = addSecondaryTypesToPattern(file, fileName, namePattern);
        File directory = out.removeLastSegments(1).toFile();

        // add parent folder and regexp for file names
        Pattern classNamesPattern = Pattern.compile(namePattern);
        ResourceUtils.addFiles(fbProject, directory, classNamesPattern);
    }

    /**
     * Add secondary types patterns (not nested in the type itself but contained
     * in the java file)
     *
     * @param fileName
     *            java file name (not path!) without .java suffix
     * @param classNamePattern
     *            non null pattern for all matching .class file names
     * @return modified classNamePattern, if there are more then one type
     *         defined in the java file
     */
    private static String addSecondaryTypesToPattern(IFile file, String fileName, String classNamePattern) {
        ICompilationUnit cu = JavaCore.createCompilationUnitFrom(file);
        if (cu == null) {
            FindbugsPlugin.getDefault().logError(
                    "NULL compilation unit for " + file + ", FB analysis might  be incomplete for included types");
            return classNamePattern;
        }
        try {
            IType[] types = cu.getTypes();
            if (types.length > 1) {
                StringBuilder sb = new StringBuilder(classNamePattern);
                for (IType type : types) {
                    if (fileName.equals(type.getElementName())) {
                        // "usual" type with the same name: we have it already
                        continue;
                    }
                    sb.append("|").append(type.getElementName());
                    sb.append("\\.class|").append(type.getElementName());
                    sb.append("\\$.*\\.class");
                }
                classNamePattern = sb.toString();
            }
        } catch (JavaModelException e) {
            FindbugsPlugin.getDefault().logException(e, "Cannot get types from compilation unit: " + cu);
        }
        return classNamePattern;
    }

    public @CheckForNull
    IResource getCorespondingResource() {
        if (resource != null) {
            return resource;
        }
        try {
            IResource resource1 = javaElt.getCorrespondingResource();
            if(resource1 != null) {
                return resource1;
            }
            IJavaElement ancestor = javaElt.getAncestor(IJavaElement.COMPILATION_UNIT);
            if(ancestor != null){
                return ancestor.getCorrespondingResource();
            }
        } catch (JavaModelException e) {
            // ignore, just return nothing
        }
        return null;
    }

    public @CheckForNull
    IJavaElement getCorespondingJavaElement() {
        if (javaElt != null) {
            return javaElt;
        }
        return JavaCore.create(resource);
    }

    /**
     * @return the resource which can be used to attach markers found for this
     *         item. This resource must exist, and the return value can not be
     *         null. The return value can be absolutely unrelated to the
     *         {@link #getCorespondingResource()}.
     */
    public @Nonnull
    IResource getMarkerTarget() {
        IResource res = getCorespondingResource();
        if (res != null) {
            return res;
        }
        if (javaElt != null) {
            IResource resource2 = javaElt.getResource();
            if (resource2 != null) {
                return resource2;
            }
        }
        // probably not the best solution, but this should always work
        return project;
    }

    /**
     * @return number of markers which are <b>already</b> reported for given
     *         work item.
     */
    public int getMarkerCount(boolean recursive) {
        return getMarkers(recursive).size();
    }

    /**
     * @return markers which are <b>already</b> reported for given work item
     */
    public Set<IMarker> getMarkers(boolean recursive) {
        IResource res = getCorespondingResource();
        if (res != null) {
            if (res.getType() == IResource.PROJECT || javaElt instanceof IPackageFragmentRoot) {
                // for project, depth_one does not make any sense here
                recursive = true;
            }
            IMarker[] markers = MarkerUtil.getMarkers(res, recursive ? IResource.DEPTH_INFINITE : IResource.DEPTH_ONE);
            return new HashSet<IMarker>(Arrays.asList(markers));
        }
        IResource markerTarget = getMarkerTarget();
        if(!markerTarget.isAccessible()) {
            return Collections.emptySet();
        }
        if (!recursive
                && ((markerTarget.getType() == IResource.PROJECT && (javaElt instanceof IPackageFragmentRoot) || Util
                        .isClassFile(javaElt)) || (Util.isJavaArchive(markerTarget) && Util.isClassFile(javaElt)))) {
            recursive = true;
        }
        IMarker[] markers = MarkerUtil.getMarkers(markerTarget, recursive ? IResource.DEPTH_INFINITE : IResource.DEPTH_ONE);
        Set<IMarker> forJavaElement = MarkerUtil.findMarkerForJavaElement(javaElt, markers, recursive);
        return forJavaElement;
    }

    /**
     * @param srcPath may be null
     * @param outLocations
     *            key is the source root, value is output folder
     * @return source root folder matching (parent of) given path, or null
     */
    private static @Nullable IPath getMatchingSourceRoot(@Nullable IPath srcPath, Map<IPath, IPath> outLocations) {
        if(srcPath == null) {
            return null;
        }
        Set<Entry<IPath, IPath>> outEntries = outLocations.entrySet();
        IPath result = null;
        int maxSegments = 0;
        for (Entry<IPath, IPath> entry : outEntries) {
            IPath srcRoot = entry.getKey();
            int firstSegments = srcPath.matchingFirstSegments(srcRoot);
            if (firstSegments > maxSegments && firstSegments == srcRoot.segmentCount()) {
                maxSegments = firstSegments;
                result = srcRoot;
            }
        }
        return result;
    }

    public String getName() {
        return resource != null ? resource.getName() : javaElt.getElementName();
    }

    /**
     *
     * @return full absolute path corresponding to the work item (file or
     *         directory). If the work item is a part of an achive, it's the
     *         path to the archive file. If the work item is a project, it's the
     *         path to the project root. TODO If the work item is an internal
     *         java element (method, inner class etc), results are undefined
     *         yet.
     */
    public @CheckForNull
    IPath getPath() {
        IResource corespondingResource = getCorespondingResource();
        if (corespondingResource != null) {
            return corespondingResource.getLocation();
        }
        if (javaElt != null) {
            return javaElt.getPath();
        }
        return null;
    }

    public boolean isDirectory() {
        IResource corespondingResource = getCorespondingResource();
        if (corespondingResource != null) {
            return corespondingResource.getType() == IResource.FOLDER || corespondingResource.getType() == IResource.PROJECT;
        }
        return false;
    }

    /**
     *
     * @return true if the given element is contained inside archive
     */
    public boolean isFromArchive() {
        IPath path = getPath();
        if (path == null) {
            return false;
        }
        File file = path.toFile();
        if (file.isDirectory()) {
            return false;
        }
        return Archive.isArchiveFileName(file.getName());
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((javaElt == null) ? 0 : javaElt.hashCode());
        result = prime * result + ((resource == null) ? 0 : resource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WorkItem)) {
            return false;
        }
        WorkItem other = (WorkItem) obj;
        if (javaElt == null) {
            if (other.javaElt != null) {
                return false;
            }
        } else if (!javaElt.equals(other.javaElt)) {
            return false;
        }
        if (resource == null) {
            if (other.resource != null) {
                return false;
            }
        } else if (!resource.equals(other.resource)) {
            return false;
        }
        return true;
    }

}
