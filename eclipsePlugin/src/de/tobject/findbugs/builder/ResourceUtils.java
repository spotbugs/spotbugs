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
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.ui.IAggregateWorkingSet;
import org.eclipse.ui.IWorkingSet;

import de.tobject.findbugs.util.ProjectUtilities;
import de.tobject.findbugs.util.Util;
import edu.umd.cs.findbugs.Project;

/**
 * @author Andrei
 */
public class ResourceUtils {

    /**
     * Convenience empty array of resources.
     */
    private static final List<WorkItem> EMPTY = Collections.emptyList();

    private ResourceUtils() {
        // forbidden
    }

    public static IPath getOutputLocation(IClasspathEntry classpathEntry, IPath defaultOutputLocation) {
        IPath outputLocation = classpathEntry.getOutputLocation();
        if (outputLocation != null) {
            // this location is workspace relative and starts with project dir
            outputLocation = relativeToAbsolute(outputLocation);
        } else {
            outputLocation = defaultOutputLocation;
        }
        return outputLocation;
    }

    public static final class FileCollector implements FileFilter {
        private final Pattern pat;

        private final Project findBugsProject;

        private FileCollector(Pattern pat, Project findBugsProject) {
            this.pat = pat;
            this.findBugsProject = findBugsProject;
        }

        public boolean accept(File file) {
            if (!file.isDirectory()) {
                // add the clzs to the list of files to be analyzed
                if (pat.matcher(file.getName()).matches()) {
                    findBugsProject.addFile(file.getAbsolutePath());
                }
            }
            return false;
        }
    }

    /**
     * recurse add all the files matching given name pattern inside the given
     * directory and all subdirectories
     */
    public static void addFiles(final Project findBugsProject, File clzDir, final Pattern pat) {
        if (clzDir.isDirectory()) {
            clzDir.listFiles(new FileCollector(pat, findBugsProject));
        }
    }

    /**
     * @param relativePath
     *            workspace relative path
     * @return given path if path is not known in workspace
     */
    public static IPath relativeToAbsolute(IPath relativePath) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(relativePath);
        if (resource != null) {
            return resource.getLocation();
        }
        return relativePath;
    }

    /**
     * Returns a list of all <b>Java source related</b> files in a resource
     * delta. This is of help when performing an incremental build.
     *
     * @return Collection A list of all <b>Java source related</b> files to be
     *         built.
     */
    public static List<WorkItem> collectIncremental(IResourceDelta delta) {
        // XXX deleted packages should be considered to remove markers
        List<WorkItem> result = new ArrayList<WorkItem>();
        List<IResourceDelta> foldersDelta = new ArrayList<IResourceDelta>();
        IResourceDelta affectedChildren[] = delta.getAffectedChildren();
        for (int i = 0; i < affectedChildren.length; i++) {
            IResourceDelta childDelta = affectedChildren[i];
            IResource child = childDelta.getResource();
            if (child.isDerived()) {
                continue;
            }
            int childType = child.getType();
            int deltaKind = childDelta.getKind();
            if (childType == IResource.FILE) {
                if ((deltaKind == IResourceDelta.ADDED || deltaKind == IResourceDelta.CHANGED) && Util.isJavaFile(child)) {
                    result.add(new WorkItem(child));
                }
            } else if (childType == IResource.FOLDER) {
                if (deltaKind == IResourceDelta.ADDED) {
                    result.add(new WorkItem(child));
                } else if (deltaKind == IResourceDelta.REMOVED) {
                    // TODO should just remove markers....
                    IContainer parent = child.getParent();
                    if (parent instanceof IProject) {
                        // have to recompute entire project if one of root
                        // folders is removed
                        result.clear();
                        result.add(new WorkItem(parent));
                        return result;
                    }
                    result.add(new WorkItem(parent));
                } else if (deltaKind != IResourceDelta.REMOVED) {
                    foldersDelta.add(childDelta);
                }
            }
        }

        for (IResourceDelta childDelta : foldersDelta) {
            result.addAll(collectIncremental(childDelta));
        }
        return result;
    }

    /**
     * Collects and combines the selection which may contain sources from
     * different projects and / or multiple sources from same project.
     * <p>
     * If selection contains hierarchical data (like file and it's parent
     * directory), the only topmost element is returned (same for directories
     * from projects).
     * <p>
     * The children from selected parents are not resolved, so that the return
     * value contains the 'highest' possible hierarchical elements without
     * children.
     *
     * @param structuredSelection
     * @return a map with the project as a key and selected resources as value.
     *         If project itself was selected, then key is the same as value.
     */
    public static Map<IProject, List<WorkItem>> getResourcesPerProject(IStructuredSelection structuredSelection) {
        Map<IProject, List<WorkItem>> projectsMap = new HashMap<IProject, List<WorkItem>>();
        for (Iterator<?> iter = structuredSelection.iterator(); iter.hasNext();) {
            Object element = iter.next();
            WorkItem workItem = getWorkItem(element);
            if (workItem == null) {
                IWorkingSet wset = Util.getAdapter(IWorkingSet.class, element);
                if (wset != null) {
                    mapResources(wset, projectsMap);
                    continue;
                }

                // Support for active changesets
                ChangeSet set = Util.getAdapter(ChangeSet.class, element);
                for (WorkItem change : getResources(set)) {
                    mapResource(change, projectsMap, true);
                }
                continue;
            }
            mapResource(workItem, projectsMap, false);
        }
        return projectsMap;
    }

    private static void mapResources(IWorkingSet wset, Map<IProject, List<WorkItem>> projectsMap) {
        Set<WorkItem> set = getResources(wset);
        for (WorkItem item : set) {
            mapResource(item, projectsMap, true);
        }
    }

    /**
     * @param wset
     *            non null working set
     * @return non null set with work items, which may be empty
     */
    public static Set<WorkItem> getResources(IWorkingSet wset) {
        Set<WorkItem> set = new HashSet<WorkItem>();
        boolean aggregateWorkingSet = wset.isAggregateWorkingSet();
        // IAggregateWorkingSet was introduced in Eclipse 3.5
        if (aggregateWorkingSet && wset instanceof IAggregateWorkingSet) {
            IAggregateWorkingSet aggr = (IAggregateWorkingSet) wset;
            IWorkingSet[] sets = aggr.getComponents();
            for (IWorkingSet iWorkingSet : sets) {
                set.addAll(getResources(iWorkingSet));
            }
        } else {
            IAdaptable[] elements = wset.getElements();
            for (IAdaptable iAdaptable : elements) {
                WorkItem item = getWorkItem(iAdaptable);
                if (item != null) {
                    set.add(item);
                }
            }
        }
        return set;
    }

    /**
     * Maps the resource into its project
     *
     * @param resource
     * @param projectsMap
     */
    private static void mapResource(WorkItem resource, Map<IProject, List<WorkItem>> projectsMap, boolean checkJavaProject) {

        IProject project = resource.getProject();
        if (checkJavaProject && !ProjectUtilities.isJavaProject(project)) {
            // non java projects: can happen only for changesets
            return;
        }
        List<WorkItem> resources = projectsMap.get(project);
        if (resources == null) {
            resources = new ArrayList<WorkItem>();
            projectsMap.put(project, resources);
        }
        // do not need to check for duplicates, cause user cannot select
        // the same element twice
        if (!containsParents(resources, resource)) {
            resources.add(resource);
        }
    }

    /**
     * Extracts only files from a change set
     *
     * @param set
     * @return
     */
    @SuppressWarnings("restriction")
    public static List<WorkItem> getResources(ChangeSet set) {
        if (set != null && !set.isEmpty()) {
            IResource[] resources = set.getResources();
            List<WorkItem> filtered = new ArrayList<WorkItem>();
            for (IResource resource : resources) {
                if (resource.getType() == IResource.FILE && !Util.isJavaArtifact(resource)) {
                    // Ignore non java files
                    continue;
                }
                if (resource.exists()) {
                    // add only resources which are NOT deleted
                    filtered.add(new WorkItem(resource));
                }
            }
            return filtered;
        }
        return EMPTY;
    }

    /**
     * @param resources
     * @param candidate
     * @return true if the given list contains at least one parent of the given
     *         candidate
     */
    private static boolean containsParents(List<WorkItem> resources, WorkItem candidate) {
        IPath location = candidate.getPath();
        if (location == null) {
            // TODO java elements?
            return false;
        }
        for (WorkItem resource : resources) {
            if (!resource.isDirectory()) {
                continue;
            }
            IPath parentLoc = resource.getPath();
            if (parentLoc != null && parentLoc.isPrefixOf(location)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convenient method to get work items (java related stuff) from adaptables
     *
     * @param element
     *            an IAdaptable object which may provide an adapter for
     *            IResource
     * @return resource object or null
     */
    @CheckForNull
    public static WorkItem getWorkItem(Object element) {
        if (element instanceof IResource) {
            IResource resource = (IResource) element;
            if (resource.getType() == IResource.FILE && !Util.isJavaArtifact(resource) || !resource.isAccessible()) {
                // Ignore non java files or deleted/closed files/projects
                return null;
            }
            return new WorkItem((IResource) element);
        }
        if (element instanceof IJavaElement) {
            return new WorkItem((IJavaElement) element);
        }

        if (element instanceof IAdaptable) {
            Object adapter = ((IAdaptable) element).getAdapter(IResource.class);
            if (adapter instanceof IResource) {
                IResource resource = (IResource) adapter;
                if (resource.getType() == IResource.FILE && !Util.isJavaArtifact(resource) || !resource.isAccessible()) {
                    // Ignore non java files or deleted/closed files/projects
                    return null;
                }
                return new WorkItem(resource);
            }
            adapter = ((IAdaptable) element).getAdapter(IPackageFragment.class);
            if (adapter instanceof IPackageFragment) {
                return new WorkItem((IPackageFragment) adapter);
            }
            adapter = ((IAdaptable) element).getAdapter(IType.class);
            if (adapter instanceof IType) {
                return new WorkItem((IType) adapter);
            }
        }
        return null;
    }

    /**
     * Convenient method to get resources from adaptables
     *
     * @param element
     *            an IAdaptable object which may provide an adapter for
     *            IResource
     * @return resource object or null
     */
    @javax.annotation.CheckForNull
    public static IResource getResource(Object element) {
        if (element instanceof IJavaElement) {
            return ((IJavaElement) element).getResource();
        }

        return Util.getAdapter(IResource.class, element);
    }
}
