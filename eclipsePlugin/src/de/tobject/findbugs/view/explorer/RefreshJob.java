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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonViewer;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * @author Andrei
 */
class RefreshJob extends Job implements IViewerRefreshJob {

    private final RemovedFirstComparator deltaComparator;

    private final List<DeltaInfo> deltaToRefresh;

    private volatile CommonViewer viewer;

    private final BugContentProvider contentProvider;

    private final ResourceChangeListener resourceListener;

    public RefreshJob(String name, BugContentProvider provider) {
        super(name);
        setSystem(true);
        setPriority(Job.DECORATE);
        contentProvider = provider;
        deltaComparator = new RemovedFirstComparator();
        deltaToRefresh = new ArrayList<DeltaInfo>();
        resourceListener = new ResourceChangeListener(this);
    }

    @Override
    public boolean belongsTo(Object family) {
        return FindbugsPlugin.class == family;
    }

    private void startListening() {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener);
    }

    private void stopListening() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
    }

    public void dispose() {
        cancel();
        setViewer(null);
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        List<DeltaInfo> deltas = fetchDeltas();
        int totalWork = deltas.size();
        monitor.beginTask("Updating bug markers", totalWork);

        if (!checkCancellation(monitor) && !deltas.isEmpty()) {

            final Set<BugGroup> changedParents = contentProvider.updateContent(deltas);
            final boolean fullRefreshNeeded = changedParents.isEmpty() || containsRoot(changedParents);

            Display.getDefault().syncExec(new Runnable() {
                public void run() {
                    if (checkCancellation(monitor)) {
                        return;
                    }
                    viewer.getControl().setRedraw(false);
                    try {
                        if (fullRefreshNeeded) {
                            // Attempt to fix https://sourceforge.net/p/findbugs/bugs/1213/
                            // discard selection (if any) before refreshing content
                            viewer.setSelection(StructuredSelection.EMPTY);
                            viewer.refresh();
                            if (BugContentProvider.DEBUG) {
                                System.out.println("Refreshing ROOT!!!");
                            }
                        } else {
                            // update the viewer based on the marker changes.
                            for (BugGroup parent : changedParents) {
                                if (BugContentProvider.DEBUG) {
                                    System.out.println("Refreshing: " + parent);
                                }
                                if (checkCancellation(monitor)) {
                                    break;
                                }
                                viewer.refresh(parent, true);
                            }
                        }
                    } finally {
                        if(viewer != null && !viewer.getControl().isDisposed()) {
                            viewer.getControl().setRedraw(true);
                        }
                    }
                }
            });
        }
        if(!monitor.isCanceled()) {
            monitor.worked(totalWork);
            monitor.done();
        }
        return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
    }

    private static boolean containsRoot(Set<BugGroup> changedParents) {
        for (BugGroup parent : changedParents) {
            boolean isRoot = parent.getParent() == null;
            if(isRoot) {
                return true;
            }
        }
        return false;
    }

    private List<DeltaInfo> fetchDeltas() {
        final List<DeltaInfo> deltas = new ArrayList<DeltaInfo>();
        synchronized (deltaToRefresh) {
            if (deltaToRefresh.isEmpty()) {
                return deltas;
            }
            deltas.addAll(deltaToRefresh);
            deltaToRefresh.clear();
        }
        Collections.sort(deltas, deltaComparator);
        if (BugContentProvider.DEBUG) {
            System.out.println("Job: going to update markers:\n" + deltas);
        }
        return deltas;
    }

    public boolean addToQueue(DeltaInfo res) {
        switch (res.changeKind) {
        case IResourceDelta.CHANGED:
            return false;
        }
        synchronized (deltaToRefresh) {
            if (!deltaToRefresh.contains(res)) {
                deltaToRefresh.add(res);
                return true;
            }
        }
        return false;
    }

    public void setViewer(CommonViewer newViewer) {
        if (newViewer != null) {
            this.viewer = newViewer;
            startListening();
        } else {
            stopListening();
            this.viewer = null;
        }
    }

    CommonViewer getViewer() {
        return viewer;
    }

    protected boolean checkCancellation(final IProgressMonitor monitor) {
        return viewer == null || monitor.isCanceled() || viewer.getControl().isDisposed();
    }

    /**
     * Sorts the removed delta's first. This allows more optimized refresh
     */
    private final static class RemovedFirstComparator implements Comparator<DeltaInfo> {
        public int compare(DeltaInfo o1, DeltaInfo o2) {
            if (o1.changeKind == o2.changeKind) {
                return 0;
            }
            if (o1.changeKind == IResourceDelta.REMOVED) {
                return -1;
            }
            return 1;
        }
    }
}
