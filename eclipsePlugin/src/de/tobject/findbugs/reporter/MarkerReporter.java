/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
 * Copyright (C) 2005, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package de.tobject.findbugs.reporter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.preference.IPreferenceStore;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.config.ProjectFilterSettings;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * Creates a FindBugs marker in a runnable window.
 */
public class MarkerReporter implements IWorkspaceRunnable {
    private final BugCollection collection;

    private final List<MarkerParameter> mpList;

    private final IProject project;

    public MarkerReporter(List<MarkerParameter> mpList, BugCollection theCollection, IProject project) {

        this.mpList = mpList;
        this.collection = theCollection;
        this.project = project;
    }

    public void run(IProgressMonitor monitor) throws CoreException {
        UserPreferences userPrefs = FindbugsPlugin.getUserPreferences(project);
        ProjectFilterSettings filterSettings = userPrefs.getFilterSettings();
        IPreferenceStore store = FindbugsPlugin.getPluginPreferences(project);
        for (MarkerParameter mp : mpList) {
            if (mp.markerType == null) {
                continue;
            }
            if (!MarkerUtil.shouldDisplayWarning(mp.bug, filterSettings)) {
                continue;
            }
            updateMarkerSeverity(store, mp);
            // This triggers resource update on IResourceChangeListener's
            // (BugTreeView)
            addMarker(mp);
        }
    }

    private void updateMarkerSeverity(IPreferenceStore store, MarkerParameter mp) {
        String markerSeverityStr = store.getString(mp.markerType);
        mp.markerSeverity = MarkerSeverity.get(markerSeverityStr).value;
    }

    private void addMarker(MarkerParameter mp) throws CoreException {
        IResource markerTarget = mp.resource.getMarkerTarget();
        IMarker[] existingMarkers = markerTarget.findMarkers(mp.markerType, true, IResource.DEPTH_ZERO);
        Map<String, Object> attributes = createMarkerAttributes(mp);

        // XXX Workaround for bug 2785257 (has to be solved better)
        // see
        // http://sourceforge.net/tracker/?func=detail&atid=614693&aid=2785257&group_id=96405
        // currently we can't run FB only on a subset of classes related to the
        // specific
        // source folder if source folders have same class output directory.
        // In this case the classes from BOTH source folders are examined by FB
        // and
        // new markers can be created for issues which are already reported.
        // Therefore here we check if a marker with SAME bug id is already
        // known,
        // and if yes, delete it (replacing with newer one)
        if (existingMarkers.length > 0) {
            IMarker oldMarker = findSameBug(attributes, existingMarkers);
            if (oldMarker != null) {
                oldMarker.delete();
            }
        }
        IMarker newMarker = markerTarget.createMarker(mp.markerType);
        setAttributes(newMarker, attributes);
    }

    private @CheckForNull
    IMarker findSameBug(Map<String, Object> attributes, IMarker[] existingMarkers) throws CoreException {
        Object bugId = attributes.get(FindBugsMarker.UNIQUE_ID);
        if (bugId == null) {
            return null;
        }
        Object line = attributes.get(FindBugsMarker.PRIMARY_LINE);
        for (IMarker marker : existingMarkers) {
            Object idAttribute = marker.getAttribute(FindBugsMarker.UNIQUE_ID);
            if (bugId.equals(idAttribute)) {
                // Fix for issue 3054146: we filter too much. Different bugs
                // from the same method and same type have equal hashes, as they
                // do not include source line info to the hash calculation
                // So we must compare source lines to to avoid too much
                // filtering.
                Object primaryLine = marker.getAttribute(FindBugsMarker.PRIMARY_LINE);
                if ((line == null && primaryLine == null) || (line != null && line.equals(primaryLine))) {
                    return marker;
                }
            }
        }
        return null;
    }

    /**
     * @param mp
     * @return attributes map which should be assigned to the given marker
     */
    private Map<String, Object> createMarkerAttributes(MarkerParameter mp) {
        Map<String, Object> attributes = new HashMap<String, Object>(23);
        attributes.put(IMarker.LINE_NUMBER, mp.startLine);
        attributes.put(FindBugsMarker.PRIMARY_LINE, mp.primaryLine);
        attributes.put(FindBugsMarker.BUG_TYPE, mp.bug.getType());
        attributes.put(FindBugsMarker.PATTERN_TYPE, mp.bug.getAbbrev());
        attributes.put(FindBugsMarker.RANK, Integer.valueOf(mp.bug.getBugRank()));

        long seqNum = mp.bug.getFirstVersion();
        if (seqNum == 0) {
            attributes.put(FindBugsMarker.FIRST_VERSION, "-1");
        } else {
            AppVersion theVersion = collection.getAppVersionFromSequenceNumber(seqNum);
            if (theVersion == null) {
                attributes.put(FindBugsMarker.FIRST_VERSION, "Cannot find AppVersion: seqnum=" + seqNum + "; collection seqnum="
                        + collection.getSequenceNumber());
            } else {
                attributes.put(FindBugsMarker.FIRST_VERSION, Long.toString(theVersion.getTimestamp()));
            }
        }
        try {
            attributes.put(IMarker.MESSAGE, mp.bug.getMessageWithoutPrefix());
        } catch (RuntimeException e) {
            FindbugsPlugin.getDefault().logException(e, "Error generating msg for " + mp.bug.getType());
            attributes.put(IMarker.MESSAGE, "??? " + mp.bug.getType());
        }
        attributes.put(IMarker.SEVERITY, mp.markerSeverity);
        attributes.put(FindBugsMarker.PRIORITY_TYPE, mp.bug.getPriorityTypeString());

        switch (mp.bug.getPriority()) {
        case Priorities.HIGH_PRIORITY:
            attributes.put(IMarker.PRIORITY, Integer.valueOf(IMarker.PRIORITY_HIGH));
            break;
        case Priorities.NORMAL_PRIORITY:
            attributes.put(IMarker.PRIORITY, Integer.valueOf(IMarker.PRIORITY_NORMAL));
            break;
        default:
            attributes.put(IMarker.PRIORITY, Integer.valueOf(IMarker.PRIORITY_LOW));
            break;
        }

        // Set unique id of warning, so we can easily refer back
        // to it later: for example, when the user classifies the warning.
        String uniqueId = mp.bug.getInstanceHash();
        if (uniqueId != null) {
            attributes.put(FindBugsMarker.UNIQUE_ID, uniqueId);
        }

        // Set unique id of the plugin, so we can easily refer back
        // to it later: for example, when the user group markers by plugin.
        DetectorFactory detectorFactory = mp.bug.getDetectorFactory();
        if(detectorFactory != null) {
            String pluginId = detectorFactory.getPlugin().getPluginId();
            if (pluginId != null) {
                attributes.put(FindBugsMarker.DETECTOR_PLUGIN_ID, pluginId);
            } else {
                // XXX to avoid errors. Don't know what to do if the plugin is missing
                attributes.put(FindBugsMarker.DETECTOR_PLUGIN_ID, "edu.umd.cs.findbugs.plugins.core");
            }
        } else {
            // XXX to avoid errors. Don't know what to do if the plugin is missing
            attributes.put(FindBugsMarker.DETECTOR_PLUGIN_ID, "edu.umd.cs.findbugs.plugins.core");
        }

        IJavaElement javaElt = mp.resource.getCorespondingJavaElement();
        if (javaElt != null) {
            attributes.put(FindBugsMarker.UNIQUE_JAVA_ID, javaElt.getHandleIdentifier());
            // Eclipse markers model doesn't allow to have markers
            // attached to the (non-resource) part of the resource (like jar
            // entry inside the jar)
            // TODO we should add annotations to opened class file editors to
            // show (missing)
            // markers for single class file inside the jar. Otherwise we will
            // show markers
            // in the bug explorer view but NOT inside the class file editor
        }
        return attributes;
    }

    /**
     * Set all the attributes to marker in one 'workspace transaction'
     *
     * @param marker
     *            non null
     * @throws CoreException
     */
    private void setAttributes(IMarker marker, Map<String, Object> attributes) throws CoreException {
        marker.setAttributes(attributes);
    }

}
