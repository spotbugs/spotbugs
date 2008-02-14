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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.core.subscribers.ChangeSet;

import de.tobject.findbugs.util.Util;
import edu.umd.cs.findbugs.Project;

/**
 * @author Andrei
 */
public class ResourceUtils {

	/**
	 * Convenience empty array of resources.
	 */
	private static final IResource [] EMPTY = new IResource[0];

	private ResourceUtils() {
		// forbidden
	}

	public static final class RecurseFileCollector implements FileFilter {
		private final Pattern pat;
		private final Project findBugsProject;

		private RecurseFileCollector(Pattern pat, Project findBugsProject) {
			this.pat = pat;
			this.findBugsProject = findBugsProject;
		}

		public boolean accept(File file) {
			if (file.isDirectory()) {
				addFiles(findBugsProject, file, pat);
			} else {
				// add the clzs to the list of files to be analysed
				if (pat.matcher(file.getName()).matches()) {
					findBugsProject.addFile(file.getAbsolutePath());
				}
			}
			return false;
		}
	}

	/**
	 * recurse add all the files matching given name pattern inside the given directory
	 * and all subdirectories
	 */
	public static void addFiles(final Project findBugsProject, File clzDir,
			final Pattern pat) {
		if (clzDir.isDirectory()) {
			clzDir.listFiles(new RecurseFileCollector(pat, findBugsProject));
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
	 * Returns a list of all files in a resource delta. This is of help when performing an
	 * incremental build.
	 *
	 * @see #getFiles()
	 * @see #getFiles(IContainer)
	 * @return Collection A list of all files to be built.
	 */
	public static List<IResource> collectIncremental(IResourceDelta delta) {
		// XXX deleted packages should be considered to remove markers
		List<IResource> result = new ArrayList<IResource>();
		List<IResourceDelta> foldersDelta = new ArrayList<IResourceDelta>();
		IResourceDelta affectedChildren[] = delta.getAffectedChildren();
		for (int i = 0; i < affectedChildren.length; i++) {
			IResourceDelta childDelta = affectedChildren[i];
			IResource child = childDelta.getResource();
			if(child.isDerived()) {
				continue;
			}
			int childType = child.getType();
			int deltaKind = childDelta.getKind();
			if (childType == IResource.FILE) {
				if ((deltaKind == IResourceDelta.ADDED || deltaKind == IResourceDelta.CHANGED)
						&& Util.isJavaArtifact(child)) {
					result.add(child);
				}
			} else if (childType == IResource.FOLDER) {
				if(deltaKind == IResourceDelta.ADDED) {
					result.add(child);
				} else if(deltaKind == IResourceDelta.REMOVED) {
					// TODO should just remove markers....
					IContainer parent = child.getParent();
					if(parent instanceof IProject) {
						// have to recompute entire project if one of root folders is removed
						result.clear();
						result.add(parent);
						return result;
					}
					result.add(parent);
				} else if(deltaKind != IResourceDelta.REMOVED) {
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
	 * Collects and combines the selection which may contain sources from different
	 * projects and / or multiple sources from same project.
	 * <p>
	 * If selection contains hierarchical data (like file and it's parent directory), the
	 * only topmost element is returned (same for directories from projects).
	 * <p>
	 * The children from selected parents are not resolved, so that the return value
	 * contains the 'highest' possible hierarchical elements without children.
	 *
	 * @param structuredSelection
	 * @return a map with the project as a key and selected resources as value. If project
	 *         itself was selected, then key is the same as value.
	 */
	public static Map<IProject, List<IResource>> getResourcesPerProject(
			IStructuredSelection structuredSelection) {
		Map<IProject, List<IResource>> projectsMap = new HashMap<IProject, List<IResource>>();
		for (Iterator<?> iter = structuredSelection.iterator(); iter.hasNext();) {
			Object element = iter.next();
			IResource resource = getResource(element);
			if (resource == null) {
				// Support for active changesets
				ChangeSet set = (ChangeSet) ((IAdaptable) element)
						.getAdapter(ChangeSet.class);
				for (IResource change : getResources(set)) {
					mapResource(change, projectsMap, true);
				}
				continue;
			}
			mapResource(resource, projectsMap, false);
		}
		return projectsMap;
	}

	/**
	 * Maps the resource into its project
	 * @param resource
	 * @param projectsMap
	 */
	private static void mapResource(IResource resource, Map<IProject,
			List<IResource>> projectsMap, boolean checkJavaProject) {
		if (!Util.isJavaArtifact(resource)) {
			// Ignore non java files
			return;
		}
		IProject project = resource.getProject();
		if (checkJavaProject && !Util.isJavaProject(project)) {
			// non java projects: can happen only for changesets
			return;
		}
		List<IResource> resources = projectsMap.get(project);
		if (resources == null) {
			resources = new ArrayList<IResource>();
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
	 * @param set
	 * @return
	 */
	public static IResource[] getResources(ChangeSet set) {
		if (set != null && !set.isEmpty()) {
			return set.getResources();
		}
		return EMPTY;
	}

	/**
	 * @param resources
	 * @param candidate
	 * @return true if the given list contains at least one parent of the given candidate
	 */
	private static boolean containsParents(List<IResource> resources, IResource candidate) {
		IPath location = candidate.getLocation();
		for (IResource resource : resources) {
			if (resource.getType() == IResource.FILE) {
				continue;
			}
			IContainer parent = (IContainer) resource;
			IPath parentLoc = parent.getLocation();
			if (parentLoc != null && parentLoc.isPrefixOf(location)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Convinient method to get resources from adaptables
	 * @param element an IAdaptable object which may provide an adapter for IResource
	 * @return resource object or null
	 */
	public static IResource getResource(Object element) {
		if(element instanceof IResource) {
			return (IResource) element;
		}

		if(element instanceof IAdaptable) {
			return (IResource) ((IAdaptable) element).getAdapter(IResource.class);
		}

		return null;
	}
}
