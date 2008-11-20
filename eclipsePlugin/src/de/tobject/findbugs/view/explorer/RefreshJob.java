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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * @author Andrei
 */
class RefreshJob extends Job implements IViewerRefreshJob {

	final LinkedList<DeltaInfo> deltaToRefresh;
	private volatile CommonViewer viewer;
	private final BugContentProvider contentProvider;

	public RefreshJob(String name, BugContentProvider provider) {
		super(name);
		this.contentProvider = provider;
		deltaToRefresh = new LinkedList<DeltaInfo>();
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		int totalWork = deltaToRefresh.size();
		monitor.beginTask("Updating bug markers", totalWork);
		List<DeltaInfo> deltas;

		while (viewer != null && !monitor.isCanceled()
				&& !(deltas = fetchDeltas()).isEmpty()) {

			final Set<BugGroup> changedParents = contentProvider.updateContent(deltas);
			final boolean fullRefreshNeeded = changedParents.isEmpty();

			// XXX should we run a- or synchronious here????
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					if (viewer != null && !viewer.getControl().isDisposed()
							&& !monitor.isCanceled()) {

						viewer.getControl().setRedraw(false);
						try {
							if (fullRefreshNeeded) {
								viewer.refresh();
								if(BugContentProvider.DEBUG){
									System.out.println("Refreshing ROOT!!!");
								}
							} else {
								// update the viewer based on the marker changes.
								for (BugGroup parent : changedParents) {
									viewer.refresh(parent, true);
									if(BugContentProvider.DEBUG){
										System.out.println("Refreshing: " + parent);
									}
									if(monitor.isCanceled()){
										break;
									}
								}
							}
						} finally {
							viewer.getControl().setRedraw(true);
						}
					}
				}
			});
		}
		monitor.worked(totalWork);

		monitor.done();
		return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
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
		Collections.sort(deltas, new Comparator<DeltaInfo>(){

			public int compare(DeltaInfo o1, DeltaInfo o2) {
				if(o1.changeKind == o2.changeKind){
					return 0;
				}
				if(o1.changeKind == IResourceDelta.REMOVED){
					return -1;
				}
				return 1;
			}

		});
		return deltas;
	}

	public boolean addToQueue(DeltaInfo res) {
		switch (res.changeKind) {
		case IResourceDelta.CHANGED:
			if(res.data instanceof IMarker) {
				return false;
			}
		}
		synchronized (deltaToRefresh) {
			if (!deltaToRefresh.contains(res)) {
				deltaToRefresh.add(res);
				return true;
			}
		}
		return false;
	}

	public void setViewer(CommonViewer viewer) {
		this.viewer = viewer;
	}

	CommonViewer getViewer() {
		return viewer;
	}

}
