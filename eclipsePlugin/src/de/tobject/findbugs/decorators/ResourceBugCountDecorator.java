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
package de.tobject.findbugs.decorators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.progress.WorkbenchJob;

import de.tobject.findbugs.builder.ResourceUtils;
import de.tobject.findbugs.builder.WorkItem;
import de.tobject.findbugs.util.Util;

/**
 * A simple decorator which adds (in currently hardcoded way) bug counts to the
 * resources. There are 3 different decorators configured via plugin.xml
 * (project/folder/file), current implementation is the same for all.
 *
 * @author Andrey
 */
public class ResourceBugCountDecorator implements ILabelDecorator {

    final class BugCountUpdateJob extends WorkbenchJob {

        private final Set<WorkItem> queue;

        public BugCountUpdateJob() {
            super("Bug count decoration update..."); //$NON-NLS-1$
            this.queue = ConcurrentHashMap.newKeySet();
            setSystem(true);
            setPriority(DECORATE);
        }

        @Override
        public boolean belongsTo(Object family) {
            return ResourceBugCountDecorator.class == family;
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            List<WorkItem> changed = new ArrayList<>(queue);
            queue.removeAll(changed);
            Set<IResource> set = changed.stream().map(WorkItem::getProject).filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!set.isEmpty()) {
                fireProblemsChanged(set.toArray(new IResource[set.size()]));
            }
            if (monitor.isCanceled()) {
                queue.clear();
                return Status.CANCEL_STATUS;
            } else if (!queue.isEmpty()) {
                schedule(100);
            }
            return Status.OK_STATUS;
        }

        void schedule(Set<WorkItem> resources) {
            if (queue.addAll(resources)) {
                schedule(100);
            }
        }
    }

    static final class BugCountCacheManager {

        static final BugCountCacheManager instance = new BugCountCacheManager();

        final Set<ResourceBugCountDecorator> listeners;

        /**
         * Cache for projects status, key is resource, value is known bug count
         */
        final Map<IProject, Integer> bugCountCache;

        /** Job to compute bug counts for container resources in background */
        final BugCountCalculationJob bugCountJob;

        public BugCountCacheManager() {
            bugCountCache = new ConcurrentHashMap<>();
            bugCountJob = new BugCountCalculationJob();
            listeners = Collections.synchronizedSet(new LinkedHashSet<>());
        }

        static void scheduleTask(Set<WorkItem> resources, BugCountUpdateJob uiUpdate) {
            for (WorkItem workItem : resources) {
                scheduleTask(workItem, uiUpdate);
            }
        }

        static void scheduleTask(WorkItem workItem, BugCountUpdateJob uiUpdate) {
            instance.bugCountJob.schedule(new BugCountTask(workItem), uiUpdate);
        }

        static Integer getBugCount(IProject project) {
            return instance.bugCountCache.get(project);
        }

        static Integer setBugCount(IProject project, int bugCount) {
            return instance.bugCountCache.put(project, Integer.valueOf(bugCount));
        }

        static void register(ResourceBugCountDecorator decorator) {
            instance.listeners.add(decorator);
        }

        static void deregister(ResourceBugCountDecorator decorator) {
            instance.listeners.remove(decorator);
            if (instance.listeners.isEmpty()) {
                instance.bugCountJob.cancel();
                instance.bugCountCache.clear();
            }
        }
    }

    static final class BugCountCalculationJob extends Job {

        private final LinkedHashMap<BugCountTask, Set<BugCountUpdateJob>> queue;

        public BugCountCalculationJob() {
            super("Bug count decoration calculation..."); //$NON-NLS-1$
            this.queue = new LinkedHashMap<>();
            setSystem(true);
            setPriority(DECORATE);
        }

        @Override
        public boolean belongsTo(Object family) {
            return ResourceBugCountDecorator.class == family;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            Map<BugCountUpdateJob, Set<WorkItem>> changed = new LinkedHashMap<>();
            Entry<BugCountTask, Set<BugCountUpdateJob>> next;
            while ((next = poll()) != null && !monitor.isCanceled()) {
                BugCountTask task = next.getKey();
                task.run();
                if (task.isBugCountChanged()) {
                    final WorkItem resource = task.workItem;
                    Set<BugCountUpdateJob> jobs = next.getValue();
                    for (BugCountUpdateJob job : jobs) {
                        changed.compute(job, (k, v) -> {
                            if (v == null) {
                                v = new LinkedHashSet<>();
                            }
                            v.add(resource);
                            return v;
                        });
                    }
                }
            }
            if (!changed.isEmpty() && !monitor.isCanceled()) {
                for (Entry<BugCountUpdateJob, Set<WorkItem>> entry : changed.entrySet()) {
                    BugCountUpdateJob job = entry.getKey();
                    Set<WorkItem> resources = entry.getValue();
                    job.schedule(resources);
                }
            }
            synchronized (queue) {
                if (monitor.isCanceled()) {
                    queue.clear();
                    return Status.CANCEL_STATUS;
                } else if (!queue.isEmpty()) {
                    schedule(100);
                }
            }
            return Status.OK_STATUS;
        }

        private Entry<BugCountTask, Set<BugCountUpdateJob>> poll() {
            Entry<BugCountTask, Set<BugCountUpdateJob>> next = null;
            synchronized (queue) {
                if (!queue.isEmpty()) {
                    Iterator<Entry<BugCountTask, Set<BugCountUpdateJob>>> iterator = queue.entrySet().iterator();
                    next = iterator.next();
                    iterator.remove();
                }
            }
            return next;
        }

        void schedule(BugCountTask task, BugCountUpdateJob job) {
            synchronized (queue) {
                queue.compute(task, (k, v) -> {
                    if (v == null) {
                        v = new LinkedHashSet<>();
                    }
                    if (v.add(job)) {
                        schedule(100);
                    }
                    return v;
                });
            }
        }
    }

    static final class BugCountTask {

        final WorkItem workItem;
        volatile int oldBugCount;
        volatile int newBugCount;

        public BugCountTask(WorkItem workItem) {
            this.workItem = workItem;
        }

        @Override
        public int hashCode() {
            return workItem.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BugCountTask)) {
                return false;
            }
            BugCountTask other = (BugCountTask) obj;
            return workItem.equals(other.workItem);
        }

        void run() {
            IProject project = workItem.getProject();
            if (project == null) {
                return;
            }
            try {
                newBugCount = workItem.getMarkerCount(true);
            } finally {
                Integer old = BugCountCacheManager.setBugCount(project, newBugCount);
                if (old != null) {
                    oldBugCount = old.intValue();
                }
            }
        }

        boolean isBugCountChanged() {
            return newBugCount != oldBugCount;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("BugCountTask ["); //$NON-NLS-1$
            if (workItem != null) {
                builder.append("resource="); //$NON-NLS-1$
                builder.append(workItem);
                builder.append(", "); //$NON-NLS-1$
            }
            builder.append("newBugCount="); //$NON-NLS-1$
            builder.append(newBugCount);
            builder.append(", oldBugCount="); //$NON-NLS-1$
            builder.append(oldBugCount);
            builder.append("]"); //$NON-NLS-1$
            return builder.toString();
        }
    }

    private final ListenerList<ILabelProviderListener> listeners;

    /** Job to update bug counts for container resources in UI thread */
    private final BugCountUpdateJob bugCountUpdateJob;

    /**
     * Default constructor
     */
    public ResourceBugCountDecorator() {
        listeners = new ListenerList<>();
        bugCountUpdateJob = new BugCountUpdateJob();
        BugCountCacheManager.register(this);
    }

    @Override
    public Image decorateImage(Image image, Object element) {
        return null;
    }

    @Override
    public String decorateText(String text, Object element) {
        WorkItem item = ResourceUtils.getWorkItem(element);
        if (item == null) {
            IWorkingSet workingSet = Util.getAdapter(IWorkingSet.class, element);
            if (workingSet != null) {
                return decorateText(text, workingSet);
            }
            return text;
        }
        return decorateText(text, getMarkerCount(item));
    }

    private static String decorateText(String text, int markerCount) {
        if (markerCount == 0) {
            return text;
        }
        return text + " (" + markerCount + ")";
    }

    private String decorateText(String text, IWorkingSet workingSet) {
        Set<WorkItem> resources = ResourceUtils.getResources(workingSet);
        int markerCount = 0;
        for (WorkItem workItem : resources) {
            markerCount += getMarkerCount(workItem);
        }
        return decorateText(text, markerCount);
    }

    private int getMarkerCount(WorkItem workItem) {
        if (workItem.isProject() && workItem.getProject() != null) {
            Integer cachedCount = BugCountCacheManager.getBugCount(workItem.getProject());
            BugCountCacheManager.scheduleTask(workItem, bugCountUpdateJob);
            return cachedCount != null ? cachedCount.intValue() : 0;
        }
        return workItem.getMarkerCount(false);
    }

    void fireProblemsChanged(IResource[] changedResources) {
        if (!listeners.isEmpty()) {
            LabelProviderChangedEvent event = new LabelProviderChangedEvent(this, changedResources);
            for (ILabelProviderListener listener : listeners) {
                listener.labelProviderChanged(event);
            }
        }
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
        listeners.add(listener);
    }

    @Override
    public void dispose() {
        BugCountCacheManager.deregister(this);
        listeners.clear();
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
        listeners.remove(listener);
    }

}
