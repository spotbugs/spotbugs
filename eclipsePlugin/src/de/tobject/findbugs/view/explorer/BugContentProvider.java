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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonViewer;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;

/**
 * @author Andrei
 *
 */
public class BugContentProvider implements ITreeContentProvider {

	private CommonViewer viewer;
	private IResourceChangeListener resourceListener;
	private Map<String, BugPatternGroup> groups;
	private final static Object[] EMPTY = new Object[0];

	private static final int SHORT_DELAY = 100;
	private static final int LONG_DELAY = 1000;
	private RefreshJob refreshJob;

	public BugContentProvider() {
		super();
		groups = new HashMap<String, BugPatternGroup>();
		resourceListener = new MyResourceChangeListener();
		refreshJob = new RefreshJob("Updating bugs in bug exporer");
		refreshJob.setSystem(true);
		refreshJob.setPriority(Job.DECORATE);
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
			return project == resource ? project.getParent() : project;
		}
		if (element instanceof BugPatternGroup) {
			BugPatternGroup groupElement = (BugPatternGroup) element;
			return groupElement.getParent();
		}
		if (element instanceof IMarker) {
			IMarker marker = (IMarker) element;
			String patternDescr = marker.getAttribute(FindBugsMarker.PATTERN_DESCR_SHORT,
					"");
			BugPatternGroup groupElement = groups.get(patternDescr);
			if (groupElement != null) {
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
		if (inputElement instanceof IResource) {
			IResource resource = (IResource) inputElement;
			return getResourceChildren(resource);
		}
		if (inputElement instanceof BugPatternGroup) {
			BugPatternGroup groupElement = (BugPatternGroup) inputElement;
			return groupElement.getChildren();
		}
		return EMPTY;
	}

	private Object[] getResourceChildren(IResource resource) {
		if (resource instanceof IProject) {
			IProject project = (IProject) resource;
			if (!project.isAccessible()) {
				return EMPTY;
			}
		}
		// TODO use preference
		if (true) {
			return BugPatternGroup.createGroups(resource);
		}
		return BugPatternGroup.getMarkers(resource);
	}

	private Object[] getWorkspaceRootChildren(IWorkspaceRoot workspaceRoot) {
		// get only java projects
		IProject[] projects = workspaceRoot.getProjects();
		List<IProject> projList = new ArrayList<IProject>();
		for (IProject project : projects) {
			projList.add(project);
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
		if (viewer2 instanceof CommonViewer) {
			this.viewer = (CommonViewer) viewer2;
		}
	}

	void scheduleRefreshJob(int delay) {
		refreshJob.cancel();
		refreshJob.schedule(delay);
	}

	/**
	 * @author Andrei
	 *
	 */
	private final class MyResourceChangeListener implements IResourceChangeListener {

		public void resourceChanged(IResourceChangeEvent event) {

			IResourceDelta delta = event.getDelta();
			boolean postBuild = event.getType() == IResourceChangeEvent.POST_BUILD;

			final IResource resource;
			if (delta == null) {
				resource = null;
			} else {
				ResourceDeltaVisitor visitor = new ResourceDeltaVisitor();
				try {
					delta.accept(visitor);
				} catch (CoreException e) {
					FindbugsPlugin.getDefault().logException(e,
							"Error visiting changed resources: " + delta);
				}
				resource = visitor.project;
			}

//			System.out.println("delta: " + delta);

			boolean accepted = refreshJob.addToQueue(resource);

			if (!accepted) {
				return;
			}
			if (postBuild) {
				scheduleRefreshJob(SHORT_DELAY);
			} else {
				// After some time do updates anyways
				scheduleRefreshJob(LONG_DELAY);
			}
		}
	}

	private static final class ResourceDeltaVisitor implements IResourceDeltaVisitor {

		IResource project;

		public ResourceDeltaVisitor() {
			super();
		}

		public boolean visit(IResourceDelta delta) {
			if (delta == null) {
				return false;
			}
			IResource resource = delta.getResource();
			int kind = delta.getKind();
			int flags = delta.getFlags();
			if (kind == IResourceDelta.ADDED_PHANTOM
					|| kind == IResourceDelta.REMOVED_PHANTOM) {
				return false;
			}
			switch (resource.getType()) {
			case IResource.ROOT:
				return true;
			case IResource.FOLDER:
				// intended
			case IResource.FILE:
				project = resource.getProject();
				return false;
			case IResource.PROJECT:
				boolean open = (flags & IResourceDelta.OPEN) != 0;
				boolean refreshRoot = (kind == IResourceDelta.ADDED
						|| kind == IResourceDelta.REMOVED
						|| kind == IResourceDelta.COPIED_FROM || open);

				if (refreshRoot) {
					project = null;
				} else {
					project = resource.getProject();
					try {
						IMarker[] markerArr = resource.findMarkers(FindBugsMarker.NAME,
								true, IResource.DEPTH_INFINITE);
						if (markerArr.length == 0) {
							project = null;
							return false;
						}
					} catch (CoreException e) {
						FindbugsPlugin.getDefault().logException(e,
								"Core exception on decorateText() for: " + resource);
					}
				}
				return !refreshRoot;
			}
			return false;
		}

	}

	/**
	 * @author Andrei
	 */
	private class RefreshJob extends Job {

		volatile LinkedList<IResource> resourcesToRefresh;

		public RefreshJob(String name) {
			super(name);
			resourcesToRefresh = new LinkedList<IResource>();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Updating bug markers", resourcesToRefresh.size());
			while (!resourcesToRefresh.isEmpty() && !monitor.isCanceled()
					&& viewer != null) {
				final IResource resource = resourcesToRefresh.poll();
				monitor.subTask("Update bug markers for " + resource);

				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						if (viewer != null && !viewer.getControl().isDisposed()) {
							groups.clear();
//							if (resource == null) {
//								System.out.println("refresh root!");
//							} else {
//								System.out.println("will refresh: " + resource);
//							}
							if (resource == null) {
								viewer.refresh();
							} else {
								if (viewer.testFindItem(resource) == null) {
									viewer.refresh();
								} else {
									viewer.refresh(resource);
								}
							}
						}
					}
				});

				monitor.worked(1);
			}
			if (viewer == null) {
				resourcesToRefresh.clear();
			}
			monitor.done();
			return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
		}

		boolean addToQueue(IResource res) {
			if (!resourcesToRefresh.contains(res)) {
				resourcesToRefresh.add(res);
				return true;
			}
			return false;
		}

	}
}
