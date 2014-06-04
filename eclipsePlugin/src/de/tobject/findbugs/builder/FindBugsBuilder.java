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

package de.tobject.findbugs.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;

import de.tobject.findbugs.FindBugsJob;
import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.plugin.eclipse.util.MutexSchedulingRule;

/**
 * The <code>FindBugsBuilder</code> performs a FindBugs run on a subset of the
 * current project. It will either check all classes in a project or just the
 * ones just having been modified.
 *
 * @author Peter Friese
 * @version 1.0
 * @since 25.9.2003
 * @see IncrementalProjectBuilder
 */
public class FindBugsBuilder extends IncrementalProjectBuilder {

    /** Controls debugging. */
    public static boolean DEBUG;

    /**
     * Run the builder.
     *
     * @see IncrementalProjectBuilder#build
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
        monitor.subTask("Running FindBugs...");
        switch (kind) {
        case IncrementalProjectBuilder.FULL_BUILD: {
            FindBugs2Eclipse.cleanClassClache(getProject());
            if (FindbugsPlugin.getUserPreferences(getProject()).isRunAtFullBuild()) {
                if (DEBUG) {
                    System.out.println("FULL BUILD");
                }
                doBuild(args, monitor, kind);
            } else {
                // TODO probably worth to cleanup?
                // MarkerUtil.removeMarkers(getProject());
            }
            break;
        }
        case IncrementalProjectBuilder.INCREMENTAL_BUILD: {
            if (DEBUG) {
                System.out.println("INCREMENTAL BUILD");
            }
            doBuild(args, monitor, kind);
            break;
        }
        case IncrementalProjectBuilder.AUTO_BUILD: {
            if (DEBUG) {
                System.out.println("AUTO BUILD");
            }
            doBuild(args, monitor, kind);
            break;
        }
        default: {

            FindbugsPlugin.getDefault()
            .logWarning("UKNOWN BUILD kind" + kind);
            doBuild(args, monitor, kind);
            break;
        }
        }
        return null;
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        MarkerUtil.removeMarkers(getProject());
    }

    /**
     * Performs the build process. This method gets all files in the current
     * project and has a <code>FindBugsVisitor</code> run on them.
     *
     * @param args
     *            A <code>Map</code> containing additional build parameters.
     * @param monitor
     *            The <code>IProgressMonitor</code> displaying the build
     *            progress.
     * @param kind
     *            kind the kind of build being requested, see
     *            IncrementalProjectBuilder
     * @throws CoreException
     */
    private void doBuild(final Map<?, ?> args, final IProgressMonitor monitor, int kind) throws CoreException {
        boolean incremental = (kind == IncrementalProjectBuilder.INCREMENTAL_BUILD
                || kind == IncrementalProjectBuilder.AUTO_BUILD);
        IProject project = getProject();
        IResource resource = project;
        List<WorkItem> files;
        if (incremental) {
            IResourceDelta resourceDelta = getDelta(project);
            boolean configChanged = !isConfigUnchanged(resourceDelta);
            if (configChanged) {
                files = new ArrayList<WorkItem>();
                files.add(new WorkItem(project));
            } else {
                files = ResourceUtils.collectIncremental(resourceDelta);
                if(files.size() == 1){
                    IResource corespondingResource = files.get(0).getCorespondingResource();
                    if(corespondingResource != null) {
                        resource = corespondingResource;
                    }
                }
            }
        } else {
            files = new ArrayList<WorkItem>();
            files.add(new WorkItem(project));
        }

        work(resource, files, monitor);
    }

    /**
     * Run a FindBugs analysis on the given resource as build job BUT not
     * delaying the current Java build
     *
     * @param part
     *
     * @param resources
     *            The resource to run the analysis on.
     * @param monitor
     */
    protected void work(final IResource resource, final List<WorkItem> resources, IProgressMonitor monitor) {
        IPreferenceStore store = FindbugsPlugin.getPluginPreferences(getProject());
        boolean runAsJob = store.getBoolean(FindBugsConstants.KEY_RUN_ANALYSIS_AS_EXTRA_JOB);
        FindBugsJob fbJob = new StartedFromBuilderJob("Finding bugs in " + resource.getName() + "...", resource, resources);
        if(runAsJob) {
            // run asynchronously, so there might be more similar jobs waiting to run
            if (DEBUG) {
                FindbugsPlugin.log("cancelSimilarJobs");
            }
            FindBugsJob.cancelSimilarJobs(fbJob);
            if (DEBUG) {
                FindbugsPlugin.log("scheduleAsSystem");
            }
            fbJob.scheduleAsSystem();
            if (DEBUG) {
                FindbugsPlugin.log("done scheduleAsSystem");
            }
        } else {
            // run synchronously (in same thread)
            if (DEBUG) {
                FindbugsPlugin.log("running fbJob");
            }
            fbJob.run(monitor);
            if (DEBUG) {
                FindbugsPlugin.log("done fbJob");
            }
        }
    }

    private boolean isConfigUnchanged(IResourceDelta resourceDelta) {
        return resourceDelta != null && resourceDelta.findMember(new Path(".project")) == null
                && resourceDelta.findMember(new Path(".classpath")) == null
                && resourceDelta.findMember(FindbugsPlugin.DEPRECATED_PREFS_PATH) == null
                && resourceDelta.findMember(FindbugsPlugin.DEFAULT_PREFS_PATH) == null;
    }

    private final static class StartedFromBuilderJob extends FindBugsJob {
        private final List<WorkItem> resources;

        private StartedFromBuilderJob(String name, IResource resource, List<WorkItem> resources) {
            super(name, resource);
            this.resources = resources;
        }

        @Override
        protected boolean supportsMulticore(){
            return MutexSchedulingRule.MULTICORE;
        }

        @Override
        protected void runWithProgress(IProgressMonitor monitor) throws CoreException {
            FindBugsWorker worker = new FindBugsWorker(getResource(), monitor);
            worker.work(resources);
        }
    }
}
