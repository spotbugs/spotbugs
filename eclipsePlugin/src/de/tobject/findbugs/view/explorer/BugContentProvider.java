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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.ResourceWorkingSetFilter;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;
import org.eclipse.ui.navigator.INavigatorContentExtension;
import org.eclipse.ui.navigator.INavigatorContentService;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;

/**
 * @author Andrei
 */
public class BugContentProvider implements ICommonContentProvider {

//	private static final String SHOW_TOP_LEVEL_WORKING_SETS = WorkingSetsContentProvider.SHOW_TOP_LEVEL_WORKING_SETS;

	public static boolean DEBUG;

	private final static IMarker[] EMPTY = new IMarker[0];

	private final IResourceChangeListener resourceListener;
	private final RefreshJob refreshJob;
	private Grouping grouping;

	private Object input;
	private boolean refreshRequested;
//	private boolean showWorkingSets;

	/**
	 * Root group, either empty OR contains BugGroups OR contains Markers (last one only
	 * if the hierarchy is plain list)
	 */
	private BugGroup rootElement;

	private ResourceWorkingSetFilter resourceFilter;

	private CommonViewer viewer;

//	private IExtensionStateModel extensionStateModel;

//	private final IPropertyChangeListener rootModeListener;

	public BugContentProvider() {
		super();
		rootElement = new BugGroup(null, null, GroupType.Undefined, null);
		refreshJob = new RefreshJob("Updating bugs in bug exporer", this);
		refreshJob.setSystem(true);
		refreshJob.setPriority(Job.DECORATE);
		resourceListener = new ResourceChangeListener(refreshJob);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener);
//		rootModeListener = new IPropertyChangeListener() {
//			public void propertyChange(PropertyChangeEvent event) {
//				if (SHOW_TOP_LEVEL_WORKING_SETS.equals(event.getProperty())) {
//					showWorkingSets = extensionStateModel
//							.getBooleanProperty(SHOW_TOP_LEVEL_WORKING_SETS);
//				}
//			}
//		};
	}

	public Object[] getChildren(Object parent) {
		if (grouping == null) {
			// on initialisation
			return EMPTY;
		}
		Object[] children = EMPTY;
		if (parent instanceof BugGroup) {
			BugGroup group = (BugGroup) parent;
			if (!refreshRequested) {
				children = group.getChildren();
			} else {
				GroupType type = group.getType();
				if (type == GroupType.Workspace || type == GroupType.WorkingSet) {
					children = createChildren(type, getResources(group.getData()), group);
				} else {
					// TODO we should refresh the group data too...
					children = group.getChildren();
				}
			}
		} else {
			if (parent instanceof IWorkspaceRoot || parent instanceof IWorkingSet) {
				BugGroup root = new BugGroup(null, parent, getType(parent), null);
				children = createChildren(grouping.getFirstType(), getResources(parent),
						root);
				if (input == parent) {
					// remember the root
					rootElement = root;
				}
			}
		}
		refreshRequested = false;
		return children;
	}

	private Set<IResource> getResources(Object parent) {
		Set<IResource> resources = new HashSet<IResource>();
		if (parent instanceof IWorkingSet) {
			IWorkingSet workingSet = (IWorkingSet) parent;
			IAdaptable[] elements = workingSet.getElements();
			// elements may contain NON-resource elements, which we have to convert to
			// resources
			for (IAdaptable adaptable : elements) {
				IResource resource = (IResource) adaptable.getAdapter(IResource.class);
				// TODO get only java projects or children of them
				if (resource != null) {
					resources.add(resource);
				}
			}
		} else if (parent instanceof IWorkspaceRoot) {
			IWorkspaceRoot workspaceRoot = (IWorkspaceRoot) parent;
			// TODO get only java projects
			IProject[] projects = workspaceRoot.getProjects();
			for (IProject project : projects) {
				resources.add(project);
			}
		}
		return resources;
	}

	public Object getParent(Object element) {
		if (element instanceof BugGroup) {
			BugGroup groupElement = (BugGroup) element;
			return groupElement.getParent();
		}
		GroupType type = getType(element);
		if (type == GroupType.Marker) {
			return findParent((IMarker) element);
		} else if (type != GroupType.Undefined) {
			// XXX do we need it at all???
			return findParents(element, type).toArray();
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		return element instanceof BugGroup || element instanceof IWorkingSet
				|| element instanceof IWorkspaceRoot;
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
		refreshJob.setViewer(null);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
//		extensionStateModel.removePropertyChangeListener(rootModeListener);
		rootElement.dispose();
	}

	public void inputChanged(Viewer viewer1, Object oldInput, Object newInput) {
		this.viewer = (CommonViewer) viewer1;
		this.input = newInput;
		refreshJob.setViewer((CommonViewer) viewer1);
//			updateTitle()
	}

	public Object getInput() {
		return input;
	}

	IWorkingSet getCurrentWorkingSet(){
		ResourceWorkingSetFilter filter = getResourceFilter();
		return filter == null? null : filter.getWorkingSet();
	}

	public void reSetInput() {
		Object oldInput = getInput();
		viewer.setInput(null);
		if(oldInput instanceof IWorkingSet || oldInput instanceof IWorkspaceRoot){
			viewer.setInput(oldInput);
		} else {
			IWorkingSet workingSet = getCurrentWorkingSet();
			if(workingSet != null) {
				viewer.setInput(workingSet);
			} else {
				viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
			}
		}
	}

	/**
	 * @param desiredType
	 *            type of the child groups
	 * @param parents
	 *            all the common parents
	 * @param parent
	 *            "empty" root node, which must be filled in with markers from set of
	 *            given parents
	 * @return
	 */
	private Object[] createChildren(GroupType desiredType, Set<IResource> parents,
			BugGroup parent) {
		Set<IMarker> markerSet = new HashSet<IMarker>();
		for (IResource resource : parents) {
			IMarker[] markers = getMarkers(resource);
			for (IMarker marker : markers) {
				markerSet.add(marker);
			}
		}
		parent.setMarkers(markerSet);
		return createGroups(parent, desiredType.getMapper());
	}

	/**
	 * @param <Identifier>
	 *            object type, like String or Integer
	 * @param mapper
	 *            maps between BugInstance and group
	 */
	private <Identifier> Object[] createGroups(BugGroup parent,
			MarkerMapper<Identifier> mapper) {
		if (mapper == MarkerMapper.NO_MAPPING) {
			return parent.getAllMarkers().toArray(new IMarker[0]);
		}
		Set<IMarker> allMarkers = parent.getAllMarkers();
		GroupType childType = grouping.getChildType(mapper.getType());
		Map<Identifier, Set<IMarker>> groupIds = new HashMap<Identifier, Set<IMarker>>();

		// first, sort all bugs to the sets with same identifier type
		for (IMarker marker : allMarkers) {
			Identifier id = mapper.getIdentifier(marker);
			if (id == null) {
				FindbugsPlugin.getDefault().logWarning(
						"BugContentProvider.createPatternGroups: "
								+ "Failed to find bug id of type " + mapper.getType()
								+ " for marker on file " + marker.getResource());
				continue;
			}
			if (!groupIds.containsKey(id)) {
				groupIds.put(id, new HashSet<IMarker>());
			}
			groupIds.get(id).add(marker);
		}

		// now create groups from the sorted bug sets
		Set<Entry<Identifier, Set<IMarker>>> typesSet = groupIds.entrySet();
		BugGroup[] children = new BugGroup[typesSet.size()];
		boolean lastlevel = grouping.getChildType(mapper.getType()) == GroupType.Marker;
		int i = 0;
		for (Entry<Identifier, Set<IMarker>> entry : typesSet) {
			Identifier groupId = entry.getKey();
			children[i] = new BugGroup(parent, groupId, mapper.getType(), mapper
					.getPrio(groupId));
			children[i].setMarkers(entry.getValue());
			i++;
		}
		if (!lastlevel) {
			for (BugGroup bugGroup : children) {
				// recursive call
				createGroups(bugGroup, childType.getMapper());
			}
		}
		return children;
	}

	private IMarker[] getMarkers(IResource resource) {
		if (resource instanceof IProject) {
			if (!((IProject) resource).isAccessible()) {
				return EMPTY;
			}
		}

		ResourceWorkingSetFilter filter = getResourceFilter();
		if(filter != null && !filter.select(null, null, resource)){
			return EMPTY;
		}
		return MarkerUtil.getAllMarkers(resource);
	}

	public void setGrouping(Grouping grouping) {
		this.grouping = grouping;
	}

	public Grouping getGrouping() {
		return grouping;
	}

	public void saveState(IMemento memento) {
		if (grouping != null) {
			grouping.saveState(memento);
		}
	}

	public void init(ICommonContentExtensionSite config) {
//		extensionStateModel = config.getExtensionStateModel();
//		extensionStateModel.addPropertyChangeListener(rootModeListener);
	}

	public void restoreState(IMemento memento) {
		grouping = Grouping.restoreFrom(memento);
	}

	/**
	 * @return list of the *visible* parents with changed chldern to refresh the viewer.
	 * Retunrs empty list if the full refresh is needed
	 */
	public Set<BugGroup> updateContent(List<DeltaInfo> deltas) {
		int oldRootSize = rootElement.getChildren().length;
		Set<BugGroup> changedParents = new HashSet<BugGroup>();
		for (DeltaInfo delta : deltas) {

			if (DEBUG) {
				System.out.println(delta);
			}

			if (delta.data == null) {
				// XXX trigger content refresh
			}
			switch (delta.changeKind) {
			case IResourceDelta.REMOVED:
				// XXX think about "go into": if the current level is removed, we have
				// to jump up the level
				Object toRemove = delta.data;
				GroupType type = getType(toRemove);
				if (type == GroupType.Marker) {
					BugGroup parent = findParent((IMarker) toRemove);
					if (parent == null) {
						continue;
					}
					BugGroup accessibleParent = getFirstAccessibleParent(parent);
					changedParents.add(accessibleParent);
					removeElement(parent, toRemove);
				} else {
					List<BugGroup> dataParents = findData(toRemove);
					for (BugGroup group : dataParents) {
						Object parent = group.getParent();
						BugGroup accessibleParent = getFirstAccessibleParent(group);
						changedParents.add(accessibleParent);
						if (parent instanceof BugGroup) {
							removeElement((BugGroup) parent, group);
						}
					}
				}
				break;
			case IResourceDelta.ADDED:
				Object toAdd = delta.data;
				addElement(toAdd, changedParents);
				break;
			}
		}
		if (rootElement.getMarkersCount() == 0 || rootElement.getChildren().length != oldRootSize) {
			changedParents.clear();
			return changedParents;
		}
		return changedParents;
	}

	private void addElement(Object toAdd, Set<BugGroup> changedParents) {
		MarkerMapper<?> mapper = grouping.getFirstType().getMapper();
		ResourceWorkingSetFilter filter = getResourceFilter();
		if (toAdd instanceof IMarker) {
			// filter through working set
			IMarker marker = (IMarker) toAdd;
			if (filter != null && !filter.select(null, null, marker.getResource())) {
				return;
			}
			addElement(marker, mapper, rootElement, changedParents);
		} else if (toAdd instanceof IResource) {
			// filter through working set
			if (filter != null && !filter.select(null, null, toAdd)) {
				return;
			}
			IMarker[] markers = getMarkers((IResource) toAdd);
			for (IMarker marker : markers) {
				addElement(marker, mapper, rootElement, changedParents);
			}
		}
	}

	private <Identifier> void addElement(IMarker marker,
			MarkerMapper<Identifier> mapper, BugGroup parent,
			Set<BugGroup> changedParents) {

		if (mapper == MarkerMapper.NO_MAPPING){
			return;
		}

		Identifier id = mapper.getIdentifier(marker);
		if (id == null) {
			FindbugsPlugin.getDefault().logWarning(
					"BugContentProvider.createPatternGroups: "
							+ "Failed to find bug id of type " + mapper.getType()
							+ " for marker on file " + marker.getResource());
			return;
		}

		// search for the right node to insert. there cannot be more then one
		BugGroup matchingChild = null;
		for (Object object : parent.getChildren()) {
			if (!(object instanceof BugGroup)) {
				break;
			}
			BugGroup group = (BugGroup) object;
			if (id.equals(group.getData())) {
				matchingChild = group;
				break;
			}
		}
//		parent.addMarker(marker);
		GroupType childType = grouping.getChildType(mapper.getType());
		boolean lastlevel = childType == GroupType.Marker;
		if (matchingChild != null) {
			// node exists? fine, add marker and update children
			matchingChild.addMarker(marker);
			// update only first visible parent element to avoid multiple refreshes
			if(isAccessible(matchingChild) && changedParents.isEmpty()) {
				changedParents.add(matchingChild);
			}
			if (!lastlevel) {
				addElement(marker, childType.getMapper(), matchingChild, changedParents);
			}
		} else {
			// if there is no node, create one and recursvely all children to the last
			// level
			BugGroup group = new BugGroup(parent, id, mapper.getType(), mapper.getPrio(id));
			group.addMarker(marker);
			createGroups(group, childType.getMapper());
//			changedParents.add(parent);
		}
	}

	private GroupType getType(Object element) {
		if (element instanceof BugGroup) {
			return ((BugGroup) element).getType();
		}
		if (element instanceof IMarker) {
			return GroupType.Marker;
		}
		if (element instanceof IProject) {
			return GroupType.Project;
		}
		if (element instanceof IWorkingSet) {
			return GroupType.WorkingSet;
		}
		if (element instanceof IWorkspaceRoot) {
			return GroupType.Workspace;
		}
		if (element instanceof IPackageFragment) {
			return GroupType.Package;
		}
		if (element instanceof IJavaElement) {
			return GroupType.Class;
		}
		return GroupType.Undefined;
	}

	/**
	 * @return true if the element is accessible from the content viewer point of view.
	 * After "go into" action parent elements can became inaccessible to the viewer
	 */
	public boolean isAccessible(BugGroup group){
		BugGroup rootGroup;
		if(!(input instanceof BugGroup)) {
			rootGroup = rootElement;
		} else {
			rootGroup = (BugGroup) input;
		}

		if(grouping.compare(rootGroup.getType(), group.getType()) < 0){
			return true;
		}
		return false;
	}

	private void removeElement(BugGroup parent, Object child) {
		if (child instanceof IMarker) {
			BugGroup currParent = parent;
			while (currParent.getParent() instanceof BugGroup) {
				BugGroup prevParent = currParent;
				prevParent.removeMarker((IMarker) child);
				currParent = (BugGroup) currParent.getParent();
				if (prevParent.getMarkersCount() == 0) {
					removeElement(currParent, prevParent);
				}
			}
		} else {
			if (parent.removeChild((BugGroup) child)) {
				if (parent.getMarkersCount() == 0 && parent.getParent() instanceof BugGroup) {
					removeElement((BugGroup) parent.getParent(), parent);
				}
			}
		}
	}

	private BugGroup getFirstAccessibleParent(BugGroup element){
		if(element.getParent() instanceof BugGroup){
			BugGroup parent = (BugGroup) element.getParent();
			if(!isAccessible(parent)){
				return element;
			}
			return getFirstAccessibleParent(parent);
		}
		return element;
	}

	private List<BugGroup> findParents(Object element, GroupType type) {
		List<BugGroup> groups = findData(element);
		List<BugGroup> parents = new ArrayList<BugGroup>();
		for (BugGroup group : groups) {
			Object parent = group.getParent();
			if (parent instanceof BugGroup) {
				parents.add(group);
			}
		}
		return parents;
	}

	private BugGroup findParent(IMarker marker) {

		Object[] rootObjects = rootElement.getChildren();

		GroupType parentType = grouping.getParentType(GroupType.Marker);
		if (parentType == GroupType.Undefined) {
			return null;
		}
		// traverse through all groups looking for one with marker
		for (int i = 0; i < rootObjects.length; i++) {
			Object object = rootObjects[i];
			if (!(object instanceof BugGroup)) {
				break;
			}
			BugGroup group = (BugGroup) object;
			if (group.contains(marker)) {
				if (group.getType() == parentType) {
					return group;
				}
				rootObjects = group.getChildren();
				i = -1;
			}
		}
		return null;
	}

	private List<BugGroup> findData(Object element) {
		GroupType type = getType(element);
		return findData(element, type);
	}

	private List<BugGroup> findData(Object element, GroupType type) {
		if (!grouping.contains(type)) {
			return null;
		}

		List<BugGroup> groups = new ArrayList<BugGroup>();
		if (rootElement.getData() == element) {
			groups.add(rootElement);
			return groups;
		}

		Object[] rootObjects = rootElement.getChildren();
		Iterator<GroupType> iterator = grouping.iterator();

		// collect all the elements with given type from all the branches
		while (iterator.hasNext() && type != iterator.next()) {
			Set<Object> children = new HashSet<Object>();
			for (Object object : rootObjects) {
				Object[] children2 = getChildren(object);
				children.addAll(Arrays.asList(children2));
			}
			rootObjects = children.toArray();
		}

		// rootObjects contains all the BugGroup's with requested type
		for (int i = 0; i < rootObjects.length; i++) {
			BugGroup group = (BugGroup) rootObjects[i];
			if (group.getData() == element) {
				groups.add(group);
			}
		}
		return groups;
	}

	public void setRefreshRequested(boolean refreshRequested) {
		this.refreshRequested = refreshRequested;
	}

	public static BugContentProvider getProvider(INavigatorContentService service) {
		INavigatorContentExtension extensionById = service
				.getContentExtensionById(FindbugsPlugin.BUG_CONTENT_PROVIDER_ID);
		IContentProvider provider = extensionById.getContentProvider();
		if (provider instanceof BugContentProvider) {
			return (BugContentProvider) provider;
		}
		return null;
	}

	ResourceWorkingSetFilter getResourceFilter() {
		if(resourceFilter != null){
			return resourceFilter;
		}
		ViewerFilter[] filters = viewer.getFilters();
		for (ViewerFilter filter : filters) {
			if(filter instanceof ResourceWorkingSetFilter){
				resourceFilter = (ResourceWorkingSetFilter) filter;
				break;
			}
		}
		return resourceFilter;
	}

}
