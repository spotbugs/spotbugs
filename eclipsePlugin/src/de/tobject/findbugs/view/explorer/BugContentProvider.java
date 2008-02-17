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
package de.tobject.findbugs.view.explorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonViewer;

import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.util.Util;

/**
 * @author Andrei
 *
 */
public class BugContentProvider implements ITreeContentProvider {

	private CommonViewer viewer;
	private IResourceChangeListener resourceListener;
	private Map<String, BugPatternGroup> groups;

	public BugContentProvider() {
		super();
		groups = new HashMap<String, BugPatternGroup>();
		resourceListener = new IResourceChangeListener() {

			public void resourceChanged(IResourceChangeEvent event) {
				if (viewer != null && !viewer.getControl().isDisposed()) {
					// TODO the code below has to be replaced with the marker update job
					// and should trigger incremental update of viewer
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							groups.clear();
							viewer.refresh();
						}
					});
				}
			}
		};
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		return getElements(parentElement);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			IProject project = resource.getProject();
			return project == resource? project.getParent() : project;
		}
		if(element instanceof BugPatternGroup) {
			BugPatternGroup groupElement = (BugPatternGroup) element;
			return groupElement.getParent();
		}
		if (element instanceof IMarker) {
			IMarker marker = (IMarker) element;
			String patternDescr = marker.getAttribute(FindBugsMarker.PATTERN_DESCR_SHORT, "");
			BugPatternGroup groupElement = groups.get(patternDescr);
			if(groupElement != null) {
				return groupElement;
			}
			return marker.getResource().getProject();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		return element instanceof IResource || element instanceof BugPatternGroup;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IWorkspaceRoot) {
			return getWorkspaceRootChildren((IWorkspaceRoot) inputElement);
		}
		if(inputElement instanceof IResource) {
			IResource resource = (IResource) inputElement;
			return getResourceChildren(resource);
		}
		if(inputElement instanceof BugPatternGroup) {
			BugPatternGroup groupElement = (BugPatternGroup) inputElement;
			return groupElement.getChildren();
		}
		return null;
	}

	private Object[] getResourceChildren(IResource resource) {
		if (resource instanceof IProject) {
			IProject project = (IProject) resource;
			if (!project.isAccessible()) {
				return null;
			}
		}
		// TODO use preference
		if(true) {
			return BugPatternGroup.createGroups(resource);
		}
		return BugPatternGroup.getMarkers(resource);
	}

	private Object[] getWorkspaceRootChildren(IWorkspaceRoot workspaceRoot) {
		// get only java projects
		IProject[] projects = workspaceRoot.getProjects();
		List<IProject> projList = new ArrayList<IProject>();
		for (IProject project : projects) {
			if (project.isOpen() && Util.isJavaProject(project)) {
				projList.add(project);
			}
		}
		return projList.toArray();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		viewer = null;
		groups.clear();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
	 *      java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer2, Object oldInput, Object newInput) {
		this.viewer = (CommonViewer) viewer2;
		// TODO Auto-generated method stub

	}


}
