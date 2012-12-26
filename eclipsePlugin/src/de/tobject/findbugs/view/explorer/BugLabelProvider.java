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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker.MarkerConfidence;
import de.tobject.findbugs.marker.FindBugsMarker.MarkerRank;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.Cloud;

/**
 * @author Andrei
 */
public class BugLabelProvider implements /* IStyledLabelProvider, */ ICommonLabelProvider,
        IColorProvider {

    private final WorkbenchLabelProvider wbProvider;

    private BugContentProvider provider;

    public BugLabelProvider() {
        super();
        wbProvider = new WorkbenchLabelProvider();
    }

    public @CheckForNull Image getImage(Object element) {
        if (element instanceof BugGroup) {
            BugGroup group = (BugGroup) element;
            switch(group.getType()) {
            case Class:
            case Package:
            case Project:
            case Marker:
                return wbProvider.getImage(group.getData());
            case BugRank: {
                MarkerRank rank = (MarkerRank) group.getData();
                if(rank == null){
                    return null;
                }
                ImageRegistry imageRegistry = FindbugsPlugin.getDefault().getImageRegistry();
                return imageRegistry.get(rank.iconName());
            }
            case Confidence: {
                MarkerConfidence markerConfidence = (MarkerConfidence) group.getData();
                if(markerConfidence == null){
                    return null;
                }
                ImageRegistry imageRegistry = FindbugsPlugin.getDefault().getImageRegistry();
                return imageRegistry.get(markerConfidence.iconName());
            }
            default:
                ImageRegistry imageRegistry = FindbugsPlugin.getDefault().getImageRegistry();
                return imageRegistry.get(FindbugsPlugin.ICON_DEFAULT);
            }

        }
        if (element instanceof IMarker) {
            if (!((IMarker) element).exists()) {
                return null;
            }
        }
        return wbProvider.getImage(element);
    }

    boolean isStandalone() {
        return provider == null;
    }

    public String getText(Object element) {
        if (element instanceof BugGroup) {
            BugGroup group = (BugGroup) element;
            String cloudName = null;
            Object data = group.getData();
            if (group.getType() == GroupType.Project && data != null) {
                try {
                    SortedBugCollection bc = FindbugsPlugin.getBugCollection((IProject) data, null);
                    Cloud cloud = bc.getCloud();
                    if (cloud.isOnlineCloud()) {
                        cloudName = cloud.getCloudName();
                    }
                } catch (CoreException e) {
                    FindbugsPlugin.getDefault().logException(e, "Failed to load bug collection");
                }
            }
            if (isStandalone()) {
                return group.getShortDescription();
            }
            int filtered = getFilteredMarkersCount(group);
            String filterCount = filtered > 0 ? "/" + filtered + " filtered" : "";
            String str = group.getShortDescription() + " (" + (group.getMarkersCount() - filtered) + filterCount + ")";
            if (cloudName != null) {
                str += " - " + cloudName;
            }
            return str;
        }
        if (element instanceof IMarker) {
            IMarker marker = (IMarker) element;
            if (!marker.exists()) {
                return null;
            }
        }
        if (element instanceof IStructuredSelection) {
            return getDescriptionAndBugCount(((IStructuredSelection) element).toArray());
        }
        if (element instanceof Object[]) {
            return getDescriptionAndBugCount((Object[]) element);
        }
        return wbProvider.getText(element);
    }

    private int getFilteredMarkersCount(BugGroup group) {
        if (isStandalone() || !provider.isBugFilterActive()) {
            return 0;
        }
        return provider.getFilteredMarkersCount(group);
    }

    private String getDescriptionAndBugCount(Object[] objects) {
        if (objects.length == 0) {
            return "Nothing...";
        }
        if (objects.length == 1) {
            return getText(objects[0]);
        }
        int count = getBugCountsSum(objects);
        StringBuilder sb = new StringBuilder("Selection contains ");
        if (count == 1) {
            sb.append("exactly one single bug");
        } else if (count == 0) {
            sb.append("zero bugs (change filter settings to see more...)");
        } else {
            sb.append(count).append(" bugs");
        }
        if (isStandalone()) {
            sb.append(" (not filtered)");
        }
        return sb.toString();
    }

    private int getBugCountsSum(Object[] objects) {
        List<BugGroup> groups = new ArrayList<BugGroup>();
        List<IMarker> markers = new ArrayList<IMarker>();
        for (Object object : objects) {
            if (object instanceof BugGroup) {
                groups.add((BugGroup) object);
            } else if (object instanceof IMarker) {
                markers.add((IMarker) object);
            }
        }
        if (groups.size() > 1 && !isStandalone()) {
            Collections.sort(groups, new Comparator<BugGroup>() {
                Grouping grouping = getGrouping();

                public int compare(BugGroup o1, BugGroup o2) {
                    return grouping.compare(o1.getType(), o2.getType());
                }

            });
        }
        Set<BugGroup> finalGroups = new HashSet<BugGroup>();
        int count = 0;
        while (!groups.isEmpty()) {
            BugGroup g1 = groups.remove(groups.size() - 1);
            boolean keepIt = true;
            for (BugGroup g2 : groups) {
                Object parent = g1.getParent();
                while (g2 != parent && parent instanceof BugGroup) {
                    parent = ((BugGroup) parent).getParent();
                }
                if (g2 == parent) {
                    keepIt = false;
                    break;
                }
            }
            if (keepIt) {
                finalGroups.add(g1);
                count += g1.getMarkersCount() - getFilteredMarkersCount(g1);
            }
        }
        while (!markers.isEmpty()) {
            IMarker marker = markers.remove(markers.size() - 1);
            boolean keepIt = true;
            for (BugGroup group : finalGroups) {
                if (group.contains(marker)) {
                    keepIt = false;
                    break;
                }
            }
            if (keepIt) {
                count++;
            }
        }
        return count;
    }

    public void init(ICommonContentExtensionSite config) {
        provider = BugContentProvider.getProvider(config.getService());
    }

    Grouping getGrouping() {
        return provider == null ? null : provider.getGrouping();
    }

    public void addListener(ILabelProviderListener listener) {
        // noop
    }

    public void dispose() {
        // noop
    }

    public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    public void removeListener(ILabelProviderListener listener) {
        // noop
    }

    public String getDescription(Object anElement) {
        return getText(anElement);
    }

    public void restoreState(IMemento memento) {
        // noop
    }

    public void saveState(IMemento memento) {
        // noop
    }

    public Color getBackground(Object element) {
        return null;
    }

    public Color getForeground(Object element) {
        if (element instanceof BugGroup) {
            BugGroup group = (BugGroup) element;
            if (getFilteredMarkersCount(group) == group.getMarkersCount()) {
                return Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW);
            }
        }
        return null;
    }

}
