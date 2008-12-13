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
import java.util.HashSet;
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
import org.eclipse.jface.preference.IPreferenceStore;
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
import de.tobject.findbugs.preferences.FindBugsConstants;
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
//	private boolean showWorkingSets;

	/**
	 * Root group, either empty OR contains BugGroups OR contains Markers (last one only
	 * if the hierarchy is plain list)
	 */
	private BugGroup rootElement;

	private ResourceWorkingSetFilter resourceFilter;

	private CommonViewer viewer;

	private ICommonContentExtensionSite site;

	private final Map<BugGroup, Integer> filteredMarkersMap;

	private final HashSet<IMarker> filteredMarkers;

	private boolean bugFilterActive;

	public BugContentProvider() {
		super();
		filteredMarkersMap = new HashMap<BugGroup, Integer>();
		filteredMarkers = new HashSet<IMarker>();
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
			children = group.getChildren();
		} else {
			if (parent instanceof IWorkspaceRoot || parent instanceof IWorkingSet) {
				if(input == parent){
					Object[] objects = rootElement.getChildren();
					if(objects.length > 0) {
						if(bugFilterActive != isBugFilterActive()){
							refreshFilters();
						}
						return objects;
					}
				}
				BugGroup root = new BugGroup(null, parent, GroupType.getType(parent), null);
				clearFilters();
				children = createChildren(grouping.getFirstType(), getResources(parent),
						root);
				if (input == parent) {
					// remember the root
					rootElement = root;
				}
			}
		}
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
		GroupType type = GroupType.getType(element);
		if (type == GroupType.Marker) {
			return findParent((IMarker) element);
		}
		return null;
	}

	public boolean hasChildren(Object element) {
//		if(element instanceof BugGroup){
//			BugGroup group = (BugGroup) element;
//			return group.size() > 0 || group.getMarkersCount() != getFilteredMarkersCount(group);
//		}
		return element instanceof BugGroup || element instanceof IWorkingSet || element instanceof IWorkspaceRoot;
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
		refreshJob.setViewer(null);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
		rootElement.dispose();
		clearFilters();
	}

	public void inputChanged(Viewer viewer1, Object oldInput, Object newInput) {
		this.viewer = (CommonViewer) viewer1;
		this.input = newInput;
		refreshJob.setViewer((CommonViewer) viewer1);
		bugFilterActive = isBugFilterActive();
		clearFilters();
	}

	public void reSetInput() {
		clearFilters();
		Object oldInput = getInput();
		viewer.setInput(null);
		rootElement.dispose();
		rootElement = new BugGroup(null, null, GroupType.Undefined, null);
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

	public void clearFilters() {
		if(DEBUG){
			System.out.println("Clear filters!");
		}
		filteredMarkers.clear();
		filteredMarkersMap.clear();
	}

	public void refreshFilters() {
		clearFilters();
		bugFilterActive = isBugFilterActive();
		if(!bugFilterActive){
			return;
		}
		if(DEBUG){
			System.out.println("Refreshing filters!");
		}
		String filter = getFilter();
		for (IMarker marker : rootElement.getAllMarkers()) {
			if(MarkerUtil.isFiltered(marker, filter)) {
				filteredMarkers.add(marker);
			}
		}
	}

	public synchronized int getFilteredMarkersCount(BugGroup bugGroup){
		if(!isBugFilterActive()){
			return 0;
		}
		Integer bugCount;
		bugCount = filteredMarkersMap.get(bugGroup);
		if(bugCount == null){
			int count = 0;
			for (IMarker marker : bugGroup.getAllMarkers()){
				if(isFiltered(marker)){
					count ++;
				}
			}
			bugCount = Integer.valueOf(count);
			filteredMarkersMap.put(bugGroup, bugCount);
		}
		return bugCount.intValue();
	}


	private String getFilter() {
		final IPreferenceStore store = FindbugsPlugin.getDefault().getPreferenceStore();
		return store.getString(FindBugsConstants.LAST_USED_EXPORT_FILTER);
	}

	private boolean isFiltered(IMarker marker){
		return filteredMarkers.contains(marker);
	}

	public Object getInput() {
		return input;
	}

	private IWorkingSet getCurrentWorkingSet(){
		ResourceWorkingSetFilter filter = getResourceFilter();
		return filter == null? null : filter.getWorkingSet();
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
	private synchronized Object[] createChildren(GroupType desiredType, Set<IResource> parents,
			BugGroup parent) {
		Set<IMarker> markerSet = new HashSet<IMarker>();
		boolean filterActive = isBugFilterActive();
		String filter = getFilter();
		for (IResource resource : parents) {
			IMarker[] markers = getMarkers(resource);
			for (IMarker marker : markers) {
				boolean added = markerSet.add(marker);
				if(filterActive && added && MarkerUtil.isFiltered(marker, filter)) {
					filteredMarkers.add(marker);
				}
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
	private synchronized <Identifier> Object[] createGroups(BugGroup parent,
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
		this.site = config;
	}

	public void restoreState(IMemento memento) {
		grouping = Grouping.restoreFrom(memento);
	}

	/**
	 * @return list of the *visible* parents with changed chldern to refresh the viewer.
	 * Retunrs empty list if the full refresh is needed
	 */
	public synchronized Set<BugGroup> updateContent(List<DeltaInfo> deltas) {
		int oldRootSize = rootElement.getChildren().length;
		Set<BugGroup> changedParents = new HashSet<BugGroup>();
		boolean bugFilterActive = isBugFilterActive();
		for (DeltaInfo delta : deltas) {
			if (DEBUG) {
				System.out.println(delta);
			}
			IMarker changedMarker = delta.marker;
			switch (delta.changeKind) {
			case IResourceDelta.REMOVED:
				BugGroup parent = findParent(changedMarker);
				if (parent == null) {
					continue;
				}
				removeMarker(parent, changedMarker, changedParents);
				break;
			case IResourceDelta.ADDED:
				addMarker(changedMarker, changedParents, bugFilterActive);
				break;
			}
		}
		if (rootElement.getMarkersCount() == 0 || rootElement.getChildren().length != oldRootSize) {
			if(oldRootSize == 0 || rootElement.getChildren().length == 0){
				changedParents.clear();
			}
			return changedParents;
		}
		// XXX this is a fix for not updating of children on incremental build and
		// for "empty" groups which can never be empty... I don't know where the bug is...
		changedParents.add(rootElement);
		return changedParents;
	}

	private void removeMarker(BugGroup parent, IMarker marker, Set<BugGroup> changedParents) {
		BugGroup accessibleParent = getFirstAccessibleParent(parent);
		changedParents.add(accessibleParent);
		removeMarker(parent, marker);
	}

	private void addMarker(IMarker toAdd, Set<BugGroup> changedParents, boolean bugFilterActive) {
		MarkerMapper<?> mapper = grouping.getFirstType().getMapper();
		ResourceWorkingSetFilter filter = getResourceFilter();
		// filter through working set
		IMarker marker = toAdd;
		if (filter != null && !filter.select(null, null, marker.getResource())) {
			return;
		}
		rootElement.addMarker(marker);
		addMarker(marker, mapper, rootElement, changedParents, bugFilterActive);
	}

	private <Identifier> void addMarker(IMarker marker, MarkerMapper<Identifier> mapper,
			BugGroup parent, Set<BugGroup> changedParents, boolean bugFilterActive) {

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

		GroupType childType = grouping.getChildType(mapper.getType());
		boolean filtered = bugFilterActive && MarkerUtil.isFiltered(marker, getFilter());
		if(filtered) {
			filteredMarkers.add(marker);
		}

		if (matchingChild != null) {
			// node exists? fine, add marker and update children
			matchingChild.addMarker(marker);
			if(filtered){
				Integer count = filteredMarkersMap.get(matchingChild);
				if(count == null) {
					count = Integer.valueOf(0);
				}
				filteredMarkersMap.put(matchingChild, Integer.valueOf(count.intValue() + 1));
			}

			if(isAccessible(matchingChild)) {
				// update only first visible parent element to avoid multiple refreshes
				changedParents.add(getFirstAccessibleParent(matchingChild));
			}
			boolean lastlevel = childType == GroupType.Marker;
			if (!lastlevel) {
				addMarker(marker, childType.getMapper(), matchingChild, changedParents, bugFilterActive);
			}
		} else {
			// if there is no node, create one and recursvely all children to the last
			// level
			BugGroup group = new BugGroup(parent, id, mapper.getType(), mapper.getPrio(id));
			group.addMarker(marker);
			if(filtered){
				filteredMarkersMap.put(group, Integer.valueOf(1));
			}
			createGroups(group, childType.getMapper());
		}
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

	private void removeMarker(BugGroup parent, IMarker marker) {
		parent.removeMarker(marker);
		List<BugGroup> parents = getSelfAndParents(parent);
		boolean filtered = isFiltered(marker);
		for (BugGroup group : parents) {
			if(group.getMarkersCount() == 0 && group.getParent() instanceof BugGroup){
				removeGroup((BugGroup) group.getParent(), group);
			}
			if(filtered){
				Integer count = filteredMarkersMap.get(group);
				if(count != null && count.intValue() > 0) {
					filteredMarkersMap.put(group, Integer.valueOf(count.intValue() - 1));
				}
			}
		}
		filteredMarkers.remove(marker);
	}

	private void removeGroup(BugGroup parent, BugGroup child) {
		if (parent.removeChild(child)) {
			filteredMarkersMap.remove(child);
			if (parent.getMarkersCount() == 0 && parent.getParent() instanceof BugGroup) {
				removeGroup((BugGroup) parent.getParent(), parent);
			}
		}
	}

	private List<BugGroup> getSelfAndParents(BugGroup child){
		List<BugGroup> parents = new ArrayList<BugGroup>();
		parents.add(child);
		while(child.getParent() instanceof BugGroup){
			child = (BugGroup) child.getParent();
			parents.add(child);
		}
		return parents;
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

	public static BugContentProvider getProvider(INavigatorContentService service) {
		INavigatorContentExtension extensionById = service
				.getContentExtensionById(FindbugsPlugin.BUG_CONTENT_PROVIDER_ID);
		IContentProvider provider = extensionById.getContentProvider();
		if (provider instanceof BugContentProvider) {
			return (BugContentProvider) provider;
		}
		return null;
	}

	private ResourceWorkingSetFilter getResourceFilter() {
		if(resourceFilter != null){
			return resourceFilter;
		}

		ViewerFilter[] filters = site.getService().getFilterService().getVisibleFilters(true); // viewer.getFilters();
		for (ViewerFilter filter : filters) {
			if(filter instanceof ResourceWorkingSetFilter){
				resourceFilter = (ResourceWorkingSetFilter) filter;
				break;
			}
		}
		return resourceFilter;
	}

	public boolean isBugFilterActive(){
		return isBugFilterActive(site);
	}

	public static boolean isBugFilterActive(ICommonContentExtensionSite site){
		ViewerFilter[] visibleFilters = site.getService().getFilterService().getVisibleFilters(true);
		for (ViewerFilter filter : visibleFilters) {
			if(filter instanceof BugByIdFilter){
				return true;
			}
		}
		return false;
	}
}
