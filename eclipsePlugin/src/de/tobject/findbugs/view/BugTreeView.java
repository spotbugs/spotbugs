package de.tobject.findbugs.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.Reporter;
import de.tobject.findbugs.util.Util;

/**
 * @author unascribed
 * @author Andrei Loskutov
 */
public class BugTreeView extends AbstractFindbugsView {

	/** maps project names to corresponding trees */
	public HashMap<String, Tree> projectToTreeMap;

	/**
	 * key to get the marker for tree item
	 */
	private static final String KEY_MARKER = "marker";

	private static final String KEY_GROUP = "group";

	private MarkerJob markerJob;

	volatile private boolean disposed;

	private ResourceChangeListener resourceListener;

	public BugTreeView() {
		super();
		markerJob = new MarkerJob("Bug tree update job");
		markerJob.setSystem(true);
		markerJob.setPriority(Job.DECORATE);
	}

	private static final class ResourceDeltaVisitor implements IResourceDeltaVisitor {

		List<ResourceDelta> resources;

		public ResourceDeltaVisitor() {
			super();
			resources = new ArrayList<ResourceDelta>();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
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
				return true;
			case IResource.FILE:
				resources.add(new ResourceDelta(resource, kind));
				return false;
			case IResource.PROJECT:
				boolean open = (flags & IResourceDelta.OPEN) != 0;
				if(kind == IResourceDelta.ADDED || kind == IResourceDelta.REMOVED
						|| open) {
					resources.add(new ResourceDelta(resource, kind));
					return false;
				}
				return true;
			}
			return false;
		}

		/**
		 * @return the delta containing either only removed/added projects or files.
		 * Projects which are only "changed" would be not recorded
		 */
		public ResourceDelta[] getDeltas() {
			return resources.toArray(new ResourceDelta[0]);
		}
	}

	/** contains either projects or files, no other resources */
	private static final class ResourceDelta {
		IResource resource;
		/** see {@link IResourceDelta#getKind()} */
		int kind;
		public ResourceDelta(IResource resource, int kind) {
			this.resource = resource;
			this.kind = kind;
		}

		@Override
		public int hashCode() {
			return kind * 31 + ((resource == null) ? 0 : resource.hashCode());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof ResourceDelta)) {
				return false;
			}
			final ResourceDelta other = (ResourceDelta) obj;
			if (kind != other.kind) {
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

		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer("kind: ");
			switch (kind) {
			case IResourceDelta.ADDED:
				sb.append("added: ");
				break;
			case IResourceDelta.REMOVED:
				sb.append("removed: ");
				break;
			case IResourceDelta.CHANGED:
				sb.append("changed: ");
				break;
			}
			return sb.append(resource).toString();
		}
	}

	/**
	 * @author Andrei
	 */
	private class MarkerJob extends Job {

		volatile Vector<ResourceDelta> resourcesToRefresh;
		private final IMarker [] EMPTY_MARKER = new IMarker[0];

		public MarkerJob(String name) {
			super(name);
			resourcesToRefresh = new Vector<ResourceDelta>(13);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Updating bug markers", resourcesToRefresh.size());
			while(!resourcesToRefresh.isEmpty() && !monitor.isCanceled() && !disposed) {
				ResourceDelta delta = resourcesToRefresh.remove(0);
				IResource resource = delta.resource;
				monitor.subTask("Update bug markers for " + delta.resource);
				boolean exists = resource.exists();
				if (resource.getType() == IResource.PROJECT) {
					exists &= ((IProject) resource).isOpen();
				}
				IMarker[] markerArr = EMPTY_MARKER;
				if (exists) {
					try {
						markerArr = resource.findMarkers(FindBugsMarker.NAME, true,
								IResource.DEPTH_INFINITE);
					} catch (CoreException e) {
						FindbugsPlugin.getDefault().logException(e,
								"Core exception on update marker job");
					}
				}
				IProject project = resource.getProject();
				boolean removeProject = resource instanceof IProject
					&& delta.kind == IResourceDelta.REMOVED;
				removeMarker(project, removeProject, monitor, resource);
				exists &= markerArr.length > 0 && delta.kind != IResourceDelta.REMOVED;
				if (exists) {
					addMarker(project, monitor, markerArr);
				}
				monitor.worked(1);
			}
			// trigger the view title "updated" state
//			setContentDescription(getContentDescription());
			if(disposed) {
				resourcesToRefresh.clear();
			}
			monitor.done();
			return monitor.isCanceled()? Status.CANCEL_STATUS : Status.OK_STATUS;
		}

		boolean addToQueue(ResourceDelta res) {
			synchronized (resourcesToRefresh) {
				if(!resourcesToRefresh.contains(res)) {
					resourcesToRefresh.add(res);
					return true;
				}
			}
			return false;
		}

	}

	/**
	 * @author Andrei
	 */
	private final class ResourceChangeListener implements
			IResourceChangeListener {

		private static final int SHORT_DELAY = 100;
		private static final int LONG_DELAY = 1000;

		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta delta = event.getDelta();
			boolean shortDelay = event.getType() == IResourceChangeEvent.POST_BUILD;
			if(delta == null) {
				resourceChanged(shortDelay, new ResourceDelta(event.getResource(),
						IResourceDelta.REMOVED));
			} else {
				ResourceDeltaVisitor visitor = new ResourceDeltaVisitor();
				try {
					delta.accept(visitor);
				} catch (CoreException e) {
					FindbugsPlugin.getDefault().logException(e,
							"Error visiting resource delta: " + delta);
				}
				resourceChanged(shortDelay, visitor.getDeltas());
			}
		}

		/**
		 * @param resource
		 * @param shortDelay
		 */
		private void resourceChanged(boolean shortDelay, ResourceDelta... resource) {
			boolean accepted = false;
			for (int i = 0; i < resource.length; i++) {
				if(Reporter.DEBUG) {
					System.out.println("event res: " + resource[i]);
				}
				accepted |= markerJob.addToQueue(resource[i]);
			}
			if(!accepted) {
				return;
			}
			if (shortDelay) {
				scheduleMarkerJob(SHORT_DELAY);
			} else {
				// After some time do updates anyways
				scheduleMarkerJob(LONG_DELAY);
			}
		}

		void scheduleMarkerJob(int delay) {
			markerJob.cancel();
			IWorkbenchSiteProgressService progressService = getProgressService();
			if (progressService != null) {
				progressService.schedule(markerJob, delay);
			} else {
				markerJob.schedule(delay);
			}
		}
	}

	private class BugTreeSelectionListener extends SelectionAdapter {
		private Tree theTree;

		public BugTreeSelectionListener(Tree theTree) {
			this.theTree = theTree;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			TreeItem theItem = theTree.getSelection()[0];
			IMarker myMarker = getMarkerForTreeItem(theItem);
			IStatusLineManager manager = getViewSite().getActionBars()
					.getStatusLineManager();
			if (myMarker == null || !(myMarker.getResource().getProject().isOpen())) {
				manager.setMessage("");
				return;
			}
			manager.setMessage(myMarker.getAttribute(IMarker.MESSAGE, ""));
			/*
			 * XXX followed code must be refactored to support selection service
			 */
			IViewPart viewPart = getSite().getPage().findView(
					FindbugsPlugin.DETAILS_VIEW_ID);
			if ((viewPart instanceof DetailsView) && ((DetailsView) viewPart).isVisible()) {
				FindbugsPlugin.showMarker(myMarker);
			}
			viewPart = getSite().getPage().findView(
					FindbugsPlugin.USER_ANNOTATIONS_VIEW_ID);
			if ((viewPart instanceof UserAnnotationsView)
					&& ((UserAnnotationsView) viewPart).isVisible()) {
				UserAnnotationsView.showMarker(myMarker);
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// ignored
		}
	}

	/**
	 *
	 * @param theItem
	 * @return null if nothing found
	 */
	static IMarker getMarkerForTreeItem(TreeItem theItem) {
		if (theItem != null) {
			return (IMarker) theItem.getData(KEY_MARKER);
		}
		return null;
	}

	@Override
	public Composite createRootControl(Composite parent) {
		Composite theFolder = new TabFolder(parent, SWT.LEFT);
		projectToTreeMap = new HashMap<String, Tree>();

		resourceListener = new ResourceChangeListener();
		IProject[] projectList = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject proj : projectList) {
			if (proj.isOpen() && Util.isJavaProject(proj)) {
				resourceListener.resourceChanged(true,
						new ResourceDelta(proj, IResourceDelta.ADDED));
			}
		}
		// add resource change listener to check for project cloure (bug 1674457)
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener);
		return theFolder;
	}

	@Override
	public void dispose() {
		disposed = true;
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
		markerJob.cancel();
		projectToTreeMap.clear();
		super.dispose();
	}

	private void addMarker(final IProject theProject, final IProgressMonitor monitor,
			final IMarker... markerArr) {
		if(theProject == null) {
			return;
		}
		final String projectName = theProject.getName();
		Display.getDefault().asyncExec(new Runnable() {
			@SuppressWarnings("unchecked")
			public void run() {
				for (int i = 0; i < markerArr.length && !monitor.isCanceled()
						&& !disposed; i++) {
					IMarker theMarker = markerArr[i];
					addMarker(theMarker, projectName);
				}
			}
		});
	}

	private void removeMarker(final IProject theProject, final boolean
			removeProject, final IProgressMonitor monitor, final IResource... resources) {
		if(theProject == null) {
			// TODO check for new projects in workspace
			return;
		}
		final String projectName = theProject.getName();
		Display.getDefault().asyncExec(new Runnable() {
			@SuppressWarnings("unchecked")
			public void run() {
				if(removeProject) {
					removeMarker(null, projectName, true);
				} else {
					for (int i = 0; i < resources.length && !monitor.isCanceled()
							&& !disposed; i++) {
						removeMarker(resources[i], projectName, removeProject);
					}
				}
			}
		});
	}

	/**
	 * @param theMarker
	 * @param bug
	 * @param projectName
	 */
	@SuppressWarnings("unchecked")
	private synchronized void removeMarker(final IResource resource,
			String projectName, boolean removeProject) {

		Tree theTree = projectToTreeMap.get(projectName);
		if (disposed || theTree == null || theTree.isDisposed()) {
			return;
		}

		if(removeProject) {
			projectToTreeMap.remove(projectName);
			TabFolder rootControl = (TabFolder) getRootControl();
			TabItem[] items = rootControl.getItems();
			for (int i = 0; i < items.length; i++) {
				if(items[i].getText().equals(projectName)) {
					items[i].getControl().dispose();
					items[i].setControl(null);
					items[i].dispose();
					break;
				}
			}
			theTree.dispose();
			return;
		}
		HashMap<String, TreeItem> groupMap = (HashMap<String, TreeItem>) theTree
			.getData(KEY_GROUP);
		TreeItem[] groupItems = theTree.getItems();
		for (int i = 0; i < groupItems.length; i++) {
			TreeItem[] items = groupItems[i].getItems();
			for (int j = 0; j < items.length; j++) {
				IMarker marker = (IMarker) items[j].getData(KEY_MARKER);
				if (!resource.equals(marker.getResource())) {
					continue;
				}
				items[j].dispose();
				if (groupItems[i].getItemCount() == 0) {
					String groupName = marker
							.getAttribute(FindBugsMarker.PATTERN_DESCR_SHORT,
									"Unknown pattern");
					groupMap.remove(groupName);
					groupItems[i].dispose();
				}
			}
		}
	}

	/**
	 * @param theMarker
	 * @param bug
	 * @param projectName
	 */
	@SuppressWarnings("unchecked")
	private synchronized void addMarker(final IMarker theMarker, String projectName) {
		if(disposed) {
			return;
		}
		Tree theTree = projectToTreeMap.get(projectName);

		if (theTree == null || theTree.isDisposed()) {
			theTree = createNewProjectTree(projectName);
			projectToTreeMap.put(projectName, theTree);
		}
		HashMap<String, TreeItem> groupMap = (HashMap<String, TreeItem>) theTree
				.getData(KEY_GROUP);
		String groupName = theMarker.getAttribute(FindBugsMarker.PATTERN_DESCR_SHORT, "Unknown pattern");
		TreeItem groupItem = groupMap.get(groupName);
		if (groupItem == null || groupItem.isDisposed()) {
			groupItem = createNewGroup(theTree, groupName);
			if(groupItem != null) {
				groupMap.put(groupName, groupItem);
			}
		}
		createNewInstanceItem(theMarker, groupItem);
	}

	/**
	 * @param project
	 * @param theTree
	 * @return
	 */
	private Tree createNewProjectTree(final String projectName) {
		TabItem newProjectTab = new TabItem((TabFolder) getRootControl(), SWT.LEFT);
		newProjectTab.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(
				SharedImages.IMG_OBJ_PROJECT));
		final Tree newProjectTree = new Tree(getRootControl(), SWT.LEFT | SWT.BORDER);
		newProjectTree.setData(KEY_GROUP, new HashMap<String, TreeItem>());
		newProjectTree.addSelectionListener(new BugTreeSelectionListener(newProjectTree));
		newProjectTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				TreeItem theItem = newProjectTree.getSelection()[0];
				IMarker myMarker = getMarkerForTreeItem(theItem);
				if (myMarker == null) {
					return;
				}
				FindbugsPlugin.showMarker(myMarker);
				try {
					IDE.openEditor(getSite().getPage(), myMarker, false);
				} catch (PartInitException ex) {
					FindbugsPlugin.getDefault().logException(ex,
							"Exception on opening editor");
				}
			}
		});
		newProjectTab.setControl(newProjectTree);
		newProjectTab.setText(projectName);
		return newProjectTree;
	}

	/**
	 * @param projectTree
	 *            non null project tree
	 * @param pattern
	 *            non null group name (bug short description)
	 */
	private TreeItem createNewGroup(Tree projectTree, String pattern) {
		int nextIdx = 0;
		for (; nextIdx < projectTree.getItemCount(); nextIdx++) {
			if (projectTree.getItem(nextIdx).getText().compareTo(pattern) > 0) {
				break;
			}
		}
		TreeItem groupItem = new TreeItem(projectTree, SWT.LEFT, nextIdx);
		groupItem.setText(pattern);
		return groupItem;
	}

	/**
	 * @param marker
	 *            non null bug marker
	 * @param groupItem
	 *            parent group
	 */
	private TreeItem createNewInstanceItem(final IMarker marker, TreeItem groupItem) {
		String id = marker.getAttribute(FindBugsMarker.UNIQUE_ID, "");
		if(disposed) {
			return null;
		}
		TreeItem[] treeItems = groupItem.getItems();
		for (int i = 0; i < treeItems.length; i++) {
			IMarker existingMarker = (IMarker) treeItems[i].getData(KEY_MARKER);
			if(existingMarker.getAttribute(FindBugsMarker.UNIQUE_ID, "").equals(id)) {
				return null;
			}
		}
		TreeItem instanceItem = new TreeItem(groupItem, SWT.LEFT);
		instanceItem.setData(KEY_MARKER, marker);
		instanceItem.setText(marker.getAttribute(IMarker.MESSAGE,
				"Error retrieving message"));
		return instanceItem;
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		// noop
	}


}
