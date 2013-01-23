/*
 * Contributions to FindBugs
 * Copyright (C) 2012, Andrey Loskutov
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;
import org.eclipse.ui.navigator.INavigatorContentExtension;
import org.eclipse.ui.navigator.INavigatorContentService;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * @author Andrei
 */
public class BugContentProvider implements ICommonContentProvider {

    public static boolean DEBUG;

    private final static IMarker[] EMPTY = new IMarker[0];

    private final RefreshJob refreshJob;

    private Grouping grouping;

    private Object input;

    /**
     * Root group, either empty OR contains BugGroups OR contains Markers (last
     * one only if the hierarchy is plain list)
     */
    private BugGroup rootElement;

    private final WorkingSetsFilter resourceFilter;

    private CommonViewer viewer;

    private ICommonContentExtensionSite site;

    private final Map<BugGroup, Integer> filteredMarkersMap;

    private final HashSet<IMarker> filteredMarkers;

    private boolean bugFilterActive;

    public BugContentProvider() {
        super();
        filteredMarkersMap = new HashMap<BugGroup, Integer>();
        filteredMarkers = new HashSet<IMarker>();
        resourceFilter = new WorkingSetsFilter();
        rootElement = new BugGroup(null, null, GroupType.Undefined);
        refreshJob = new RefreshJob("Updating bugs in bug explorer", this);
        IPreferenceStore store = FindbugsPlugin.getDefault().getPreferenceStore();
        String saved = store.getString(FindBugsConstants.LAST_USED_GROUPING);
        setGrouping(Grouping.restoreFrom(saved));
        saved = store.getString(FindBugsConstants.LAST_USED_WORKING_SET);
        initWorkingSet(saved);
        // make sure it's initialized
        FindbugsPlugin.applyCustomDetectors(false);
    }

    public Object[] getChildren(Object parent) {
        if (grouping == null) {
            // on initialization
            return EMPTY;
        }
        Object[] children = EMPTY;
        if (parent instanceof BugGroup) {
            BugGroup group = (BugGroup) parent;
            children = group.getChildren();
            if (rootElement == parent && rootElement.size() == 0) {
                if (DEBUG) {
                    System.out.println("Root is empty...");
                }
            }
        } else {
            if (parent instanceof IWorkspaceRoot || parent instanceof IWorkingSet) {
                if (input == parent) {
                    Object[] objects = rootElement.getChildren();
                    if (objects.length > 0) {
                        if (bugFilterActive != isBugFilterActive()) {
                            refreshFilters();
                        }
                        return objects;
                    }
                }
                BugGroup root = new BugGroup(null, parent, GroupType.getType(parent));
                clearFilters();
                children = createChildren(grouping.getFirstType(), getResources(parent), root);
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
            // elements may contain NON-resource elements, which we have to
            // convert to
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
        if (element instanceof IMarker) {
            return findParent((IMarker) element);
        }
        return null;
    }

    public boolean hasChildren(Object element) {
        // if(element instanceof BugGroup){
        // BugGroup group = (BugGroup) element;
        // return group.size() > 0 || group.getMarkersCount() !=
        // getFilteredMarkersCount(group);
        // }
        return element instanceof BugGroup || element instanceof IWorkingSet || element instanceof IWorkspaceRoot;
    }

    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    public void dispose() {
        if (DEBUG) {
            System.out.println("Disposing content provider!");
        }
        refreshJob.dispose();
        rootElement.dispose();
        clearFilters();

        IPreferenceStore store = FindbugsPlugin.getDefault().getPreferenceStore();
        store.setValue(FindBugsConstants.LAST_USED_GROUPING, getGrouping().toString());
        IWorkingSet workingSet = getCurrentWorkingSet();
        String name = workingSet != null ? workingSet.getName() : "";
        store.setValue(FindBugsConstants.LAST_USED_WORKING_SET, name);
    }

    public void inputChanged(Viewer newViewer, Object oldInput, Object newInput) {
        viewer = (CommonViewer) newViewer;
        if (newInput == null || newInput instanceof IWorkingSet || newInput instanceof IWorkspaceRoot) {
            rootElement.dispose();
        }
        input = newInput;
        if (newInput == null) {
            refreshJob.setViewer(null);
        } else {
            refreshJob.setViewer((CommonViewer) newViewer);
            bugFilterActive = isBugFilterActive();
        }
        clearFilters();
    }

    public void reSetInput() {
        clearFilters();
        Object oldInput = getInput();
        viewer.setInput(null);
        rootElement.dispose();
        rootElement = new BugGroup(null, null, GroupType.Undefined);
        if (oldInput instanceof IWorkingSet || oldInput instanceof IWorkspaceRoot) {
            viewer.setInput(oldInput);
        } else {
            IWorkingSet workingSet = getCurrentWorkingSet();
            if (workingSet != null) {
                viewer.setInput(workingSet);
            } else {
                viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
            }
        }
    }

    public void clearFilters() {
        if (DEBUG) {
            System.out.println("Clear filters!");
        }
        filteredMarkers.clear();
        filteredMarkersMap.clear();
    }

    public void refreshFilters() {
        clearFilters();
        bugFilterActive = isBugFilterActive();
        if (!bugFilterActive) {
            return;
        }
        if (DEBUG) {
            System.out.println("Refreshing filters!");
        }
        Set<String> patternFilter = getPatternFilter();
        for (IMarker marker : rootElement.getAllMarkers()) {
            if (MarkerUtil.isFiltered(marker, patternFilter)) {
                filteredMarkers.add(marker);
            }
        }
    }

    public synchronized int getFilteredMarkersCount(BugGroup bugGroup) {
        if (!isBugFilterActive()) {
            return 0;
        }
        Integer bugCount;
        bugCount = filteredMarkersMap.get(bugGroup);
        if (bugCount == null) {
            int count = 0;
            for (IMarker marker : bugGroup.getAllMarkers()) {
                if (isFiltered(marker)) {
                    count++;
                }
            }
            bugCount = Integer.valueOf(count);
            filteredMarkersMap.put(bugGroup, bugCount);
        }
        return bugCount.intValue();
    }

    private Set<String> getPatternFilter() {
        return FindbugsPlugin.getFilteredIds();
    }

    public boolean isFiltered(IMarker marker) {
        return filteredMarkers.contains(marker);
    }

    public Object getInput() {
        return input;
    }

    IWorkingSet getCurrentWorkingSet() {
        return resourceFilter.getWorkingSet();
    }

    void setCurrentWorkingSet(IWorkingSet workingSet) {
        resourceFilter.setWorkingSet(workingSet);
    }

    /**
     * @param desiredType
     *            type of the child groups
     * @param parents
     *            all the common parents
     * @param parent
     *            "empty" root node, which must be filled in with markers from
     *            set of given parents
     * @return
     */
    private synchronized Object[] createChildren(GroupType desiredType, Set<IResource> parents, BugGroup parent) {
        Set<IMarker> markerSet = new HashSet<IMarker>();
        boolean filterActive = isBugFilterActive();
        Set<String> patternFilter = getPatternFilter();
        for (IResource resource : parents) {
            IMarker[] markers = getMarkers(resource);
            for (IMarker marker : markers) {
                boolean added = markerSet.add(marker);
                if (filterActive && added && MarkerUtil.isFiltered(marker, patternFilter)) {
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
    private synchronized <Identifier> Object[] createGroups(BugGroup parent, MarkerMapper<Identifier> mapper) {
        if (mapper == MarkerMapper.NO_MAPPING) {
            return parent.getAllMarkers().toArray(new IMarker[0]);
        }
        Set<IMarker> allMarkers = parent.getAllMarkers();
        GroupType childType = grouping.getChildType(mapper.getType());
        Map<Identifier, Set<IMarker>> groupIds = new HashMap<Identifier, Set<IMarker>>();
        UserPreferences prefs = FindbugsPlugin.getCorePreferences(null, false);
        Set<String> disabledPlugins = prefs.getCustomPlugins(false);

        // first, sort all bugs to the sets with same identifier type
        Set<String> errorMessages = new HashSet<String>();
        for (IMarker marker : allMarkers) {
            Identifier id = mapper.getIdentifier(marker);
            if (id == null) {
                String pluginId = MarkerUtil.getPluginId(marker);
                if(pluginId.length() == 0 || disabledPlugins.contains(pluginId)){
                    // do not report errors for disabled plugins
                    continue;
                }
                try {
                    String debugDescription = mapper.getDebugDescription(marker);
                    if(errorMessages.contains(debugDescription)) {
                        continue;
                    }
                    errorMessages.add(debugDescription);
                    if(FindbugsPlugin.getDefault().isDebugging()){
                        FindbugsPlugin.getDefault().logWarning(
                            "BugContentProvider.createGroups: failed to find " + debugDescription + " for marker on file "
                                    + marker.getResource());
                    }
                } catch (CoreException e) {
                    FindbugsPlugin.getDefault().logException(e, "Exception on retrieving debug data for: " + mapper.getType());
                }
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
            children[i] = new BugGroup(parent, groupId, mapper.getType());
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
        if (!resourceFilter.contains(resource)) {
            return EMPTY;
        }
        return MarkerUtil.getAllMarkers(resource);
    }

    public void setGrouping(Grouping grouping) {
        this.grouping = grouping;
        if (viewer != null) {
            // will start listening on resource changes, if not yet started
            refreshJob.setViewer(viewer);
        }
    }

    public Grouping getGrouping() {
        return grouping;
    }

    public void saveState(IMemento memento) {
        if (DEBUG) {
            System.out.println("Save state!");
        }
    }

    public void init(ICommonContentExtensionSite config) {
        this.site = config;
    }

    public void restoreState(IMemento memento) {
        if (DEBUG) {
            System.out.println("Restore state!");
        }
    }

    protected void initWorkingSet(String workingSetName) {
        IWorkingSet workingSet = null;

        if (workingSetName != null && workingSetName.length() > 0) {
            IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
            workingSet = workingSetManager.getWorkingSet(workingSetName);
        } /*
           * else if (PlatformUI.getPreferenceStore().getBoolean(
           * IWorkbenchPreferenceConstants.USE_WINDOW_WORKING_SET_BY_DEFAULT)) {
           * // use the window set by default if the global preference is set
           * workingSet =
           * PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage
           * ().getAggregateWorkingSet(); }
           */
        if (workingSet != null) {
            setCurrentWorkingSet(workingSet);
        }
    }

    /**
     * @return list of the *visible* parents with changed children to refresh
     *         the viewer. Returns empty list if the full refresh is needed
     */
    public synchronized Set<BugGroup> updateContent(List<DeltaInfo> deltas) {
        int oldRootSize = rootElement.getChildren().length;
        Set<BugGroup> changedParents = new HashSet<BugGroup>();
        bugFilterActive = isBugFilterActive();
        Set<String> patternFilter = getPatternFilter();
        for (DeltaInfo delta : deltas) {
            if (DEBUG) {
                System.out.println(delta + " (contentProvider.updateContent)");
            }
            IMarker changedMarker = delta.marker;
            switch (delta.changeKind) {
            case IResourceDelta.REMOVED:
                BugGroup parent = findParent(changedMarker);
                if (parent == null) {
                    if (DEBUG) {
                        System.out.println(delta + " IGNORED because marker does not have parent!");
                    }
                    continue;
                }
                removeMarker(parent, changedMarker, changedParents);
                break;
            case IResourceDelta.ADDED:
                if (!changedMarker.exists()) {
                    if (DEBUG) {
                        System.out.println(delta + " IGNORED because marker does not exists anymore!");
                    }
                    continue;
                }
                addMarker(changedMarker, changedParents, patternFilter);
                break;
            default:
                    FindbugsPlugin.getDefault()
                    .logWarning("UKNOWN delta change kind" + delta.changeKind);

            }
        }
        if (rootElement.getMarkersCount() == 0 || rootElement.getChildren().length != oldRootSize) {
            if (oldRootSize == 0 || rootElement.getChildren().length == 0) {
                changedParents.clear();
            } else {
                changedParents.add(rootElement);
            }
            return changedParents;
        }
        // XXX this is a fix for not updating of children on incremental build
        // and
        // for "empty" groups which can never be empty... I don't know where the
        // bug is...
        changedParents.add(rootElement);
        return changedParents;
    }

    private void removeMarker(BugGroup parent, IMarker marker, Set<BugGroup> changedParents) {
        BugGroup accessibleParent = getFirstAccessibleParent(parent);
        changedParents.add(accessibleParent);
        removeMarker(parent, marker);
    }

    private void addMarker(IMarker toAdd, Set<BugGroup> changedParents, Set<String> patternFilter) {
        MarkerMapper<?> mapper = grouping.getFirstType().getMapper();
        // filter through working set
        IMarker marker = toAdd;
        if (!resourceFilter.contains(marker.getResource())) {
            return;
        }
        rootElement.addMarker(marker);
        addMarker(marker, mapper, rootElement, changedParents, patternFilter);
    }

    private <Identifier> void addMarker(IMarker marker, MarkerMapper<Identifier> mapper, BugGroup parent,
            Set<BugGroup> changedParents, Set<String> patternFilter) {

        if (mapper == MarkerMapper.NO_MAPPING) {
            return;
        }

        Identifier id = mapper.getIdentifier(marker);
        if (id == null) {
            if(FindbugsPlugin.getDefault().isDebugging()){
                FindbugsPlugin.getDefault().logWarning(
                    "BugContentProvider.createPatternGroups: " + "Failed to find bug id of type " + mapper.getType()
                            + " for marker on file " + marker.getResource());
            }
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
        boolean filtered = bugFilterActive && MarkerUtil.isFiltered(marker, patternFilter);
        if (filtered) {
            filteredMarkers.add(marker);
        }

        if (matchingChild != null) {
            // node exists? fine, add marker and update children
            matchingChild.addMarker(marker);
            if (filtered) {
                Integer count = filteredMarkersMap.get(matchingChild);
                if (count == null) {
                    count = Integer.valueOf(0);
                }
                filteredMarkersMap.put(matchingChild, Integer.valueOf(count.intValue() + 1));
            }

            if (isAccessible(matchingChild)) {
                // update only first visible parent element to avoid multiple
                // refreshes
                changedParents.add(getFirstAccessibleParent(matchingChild));
            }
            boolean lastlevel = childType == GroupType.Marker;
            if (!lastlevel) {
                addMarker(marker, childType.getMapper(), matchingChild, changedParents, patternFilter);
            }
        } else {
            // if there is no node, create one and recursively all children to
            // the last
            // level
            BugGroup group = new BugGroup(parent, id, mapper.getType());
            group.addMarker(marker);
            if (filtered) {
                filteredMarkersMap.put(group, Integer.valueOf(1));
            }
            createGroups(group, childType.getMapper());
        }
    }

    /**
     * @return true if the element is accessible from the content viewer point
     *         of view. After "go into" action parent elements can became
     *         inaccessible to the viewer
     */
    public boolean isAccessible(BugGroup group) {
        BugGroup rootGroup;
        if (!(input instanceof BugGroup)) {
            rootGroup = rootElement;
        } else {
            rootGroup = (BugGroup) input;
        }

        if (grouping.compare(rootGroup.getType(), group.getType()) < 0) {
            return true;
        }
        return false;
    }

    private void removeMarker(BugGroup parent, IMarker marker) {
        parent.removeMarker(marker);
        List<BugGroup> parents = getSelfAndParents(parent);
        boolean filtered = isFiltered(marker);
        for (BugGroup group : parents) {
            if (group.getMarkersCount() == 0 && group.getParent() instanceof BugGroup) {
                removeGroup((BugGroup) group.getParent(), group);
            }
            if (filtered) {
                Integer count = filteredMarkersMap.get(group);
                if (count != null && count.intValue() > 0) {
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

    private List<BugGroup> getSelfAndParents(BugGroup child) {
        List<BugGroup> parents = new ArrayList<BugGroup>();
        parents.add(child);
        while (child.getParent() instanceof BugGroup) {
            child = (BugGroup) child.getParent();
            parents.add(child);
        }
        return parents;
    }

    private BugGroup getFirstAccessibleParent(BugGroup element) {
        if (element.getParent() instanceof BugGroup) {
            BugGroup parent = (BugGroup) element.getParent();
            if (!isAccessible(parent)) {
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
        INavigatorContentExtension extensionById = service.getContentExtensionById(FindbugsPlugin.BUG_CONTENT_PROVIDER_ID);
        IContentProvider provider = extensionById.getContentProvider();
        if (provider instanceof BugContentProvider) {
            return (BugContentProvider) provider;
        }
        return null;
    }

    public boolean isBugFilterActive() {
        return isBugFilterActive(site);
    }

    public static boolean isBugFilterActive(ICommonContentExtensionSite site) {
        ViewerFilter[] visibleFilters = site.getService().getFilterService().getVisibleFilters(true);
        for (ViewerFilter filter : visibleFilters) {
            if (filter instanceof BugByIdFilter) {
                return true;
            }
        }
        return false;
    }

    public Set<Object> getShowInTargets(Object obj) {
        Set<Object> supported = new HashSet<Object>();
        if (obj instanceof BugGroup) {
            supported.add(obj);
        } else if (obj instanceof IMarker) {
            supported.add(obj);
        } else if (obj instanceof IJavaProject) {
            return getShowInTargets(((IJavaProject) obj).getProject());
        } else if (obj instanceof IClassFile) {
            return getShowInTargets(((IClassFile) obj).getType());
        } else if (obj instanceof IFile) {
            IJavaElement javaElement = JavaCore.create((IFile) obj);
            return getShowInTargets(javaElement);
        } else if (obj instanceof IFolder) {
            IJavaElement javaElement = JavaCore.create((IFolder) obj);
            return getShowInTargets(javaElement);
        } else if (obj instanceof IStructuredSelection) {
            IStructuredSelection selection = (IStructuredSelection) obj;
            Iterator<?> iter = selection.iterator();
            while (iter.hasNext()) {
                Object object = iter.next();
                supported.add(getShowInTargets(object));
            }
        } else {
            // TODO think how improve performance for project/package objects?
            Set<IMarker> markers = MarkerUtil.getMarkers(obj);
            boolean found = false;
            main: for (IMarker marker : markers) {
                BugGroup group = findParent(marker);
                if (group == null) {
                    continue;
                }
                List<BugGroup> selfAndParents = getSelfAndParents(group);
                for (BugGroup bugGroup : selfAndParents) {
                    if (obj.equals(bugGroup.getData())) {
                        supported.add(bugGroup);
                        found = true;
                        break main;
                    }
                }
            }
            if (!found) {
                supported.addAll(markers);
            }
        }
        return supported;
    }

}
