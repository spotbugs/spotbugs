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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;

import de.tobject.findbugs.marker.FindBugsMarker;

/**
 * Listener which reports changes on projects or on FindBug markers
 *
 * @author Andrei
 */
public final class ResourceChangeListener implements IResourceChangeListener {

    public static final int SHORT_DELAY = 250;

    public static final int LONG_DELAY = 750;

    final IViewerRefreshJob refreshJob;

    ResourceChangeListener(IViewerRefreshJob refreshJob) {
        this.refreshJob = refreshJob;
    }

    public void resourceChanged(IResourceChangeEvent event) {

        boolean postBuild = event.getType() == IResourceChangeEvent.POST_BUILD;
        boolean accepted = false;

        /*
         * gather all marker changes from the delta. be sure to do this in the
         * calling thread, as the delta is destroyed when this method returns
         */
        IMarkerDelta[] markerDeltas = event.findMarkerDeltas(FindBugsMarker.NAME, true);
        for (IMarkerDelta mdelta : markerDeltas) {
            IMarker marker = mdelta.getMarker();
            if (marker == null) {
                continue;
            }
            DeltaInfo deltaInfo = new DeltaInfo(marker, mdelta.getKind());
            if (BugContentProvider.DEBUG) {
                System.out.println("resource change for: " + deltaInfo);
            }

            accepted |= refreshJob.addToQueue(deltaInfo);
        }

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

    void scheduleRefreshJob(int delay) {
        refreshJob.schedule(delay);
    }
}
