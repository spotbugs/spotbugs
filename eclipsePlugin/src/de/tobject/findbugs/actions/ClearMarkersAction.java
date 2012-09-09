/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
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

package de.tobject.findbugs.actions;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.IWorkbenchPart;

import de.tobject.findbugs.FindBugsJob;
import de.tobject.findbugs.builder.WorkItem;

/**
 * Remove all bug markers for the given selection.
 *
 * @author Peter Friese
 * @author Phil Crosby
 * @author Andrei Loskutov
 * @version 2.0
 * @since 25.09.2003
 */
public class ClearMarkersAction extends FindBugsAction {

    /**
     * Clear the FindBugs markers on each project in the given selection,
     * displaying a progress monitor.
     */
    @Override
    protected void work(final IWorkbenchPart part, IResource resource, final List<WorkItem> resources) {
        FindBugsJob clearMarkersJob = new ClearMarkersJob(resource, resources);
        clearMarkersJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                refreshViewer(part, resources);
            }
        });
        clearMarkersJob.scheduleInteractive();
    }
}

final class ClearMarkersJob extends FindBugsJob {
    private final List<WorkItem> resources;

    ClearMarkersJob(IResource resource, List<WorkItem> resources) {
        super("Removing FindBugs markers", resource);
        setRule(resource);
        this.resources = resources;
    }

    @Override
    protected void runWithProgress(IProgressMonitor monitor) throws CoreException {
        monitor.beginTask(getName(), resources.size());
        for (WorkItem res : resources) {
            monitor.subTask(res.getName());
            res.clearMarkers();
            monitor.worked(1);
        }
    }
}
