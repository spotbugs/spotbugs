/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Andrei Loskutov
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
package de.tobject.findbugs;

import java.util.concurrent.Semaphore;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import edu.umd.cs.findbugs.plugin.eclipse.util.MutexSchedulingRule;

/**
 * @author Andrei
 */
public abstract class FindBugsJob extends Job {

    private final static Semaphore analysisSem;

    private static final boolean DEBUG = false;
    static {
        analysisSem = new Semaphore(MutexSchedulingRule.MAX_JOBS, true);

        // see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=298795
        // we must run this stupid code in the UI thread
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                PlatformUI.getWorkbench().getProgressService().registerIconForFamily(
                        FindbugsPlugin.getDefault().getImageDescriptor("runFindbugs.png"),
                        FindbugsPlugin.class);
            }
        });
    }

    private final IResource resource;

    public static void cancelSimilarJobs(FindBugsJob job) {
        if(job.getResource() == null) {
            return;
        }
        Job[] jobs = Job.getJobManager().find(FindbugsPlugin.class);
        for (Job job2 : jobs) {
            if (job2 instanceof FindBugsJob
                    && job.getResource().equals(((FindBugsJob)job2).getResource())) {
                if(job2.getState() != Job.RUNNING) {
                    job2.cancel();
                }
            }
        }
    }

    public FindBugsJob(String name, IResource resource) {
        super(name);
        this.resource = resource;
        setRule(new MutexSchedulingRule(resource));
    }

    public IResource getResource() {
        return resource;
    }

    @Override
    public boolean belongsTo(Object family) {
        return FindbugsPlugin.class == family;
    }

    public void scheduleInteractive() {
        setUser(true);
        setPriority(Job.INTERACTIVE);

        // paranoia
        if(supportsMulticore() && analysisSem.availablePermits() == 0
                && Job.getJobManager().find(FindbugsPlugin.class).length == 0){
            analysisSem.release(MutexSchedulingRule.MAX_JOBS);
        }

        schedule();
    }

    public void scheduleAsSystem() {
        setUser(false);
        setPriority(Job.BUILD);
        schedule();
    }

    protected String createErrorMessage() {
        return getName() + " failed";
    }

    abstract protected void runWithProgress(IProgressMonitor monitor) throws CoreException;

    protected boolean supportsMulticore(){
        return false;
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {
        boolean acquired = false;
        try {
            if(supportsMulticore()){
                if (DEBUG) {
                    FindbugsPlugin.log("Acquiring analysisSem");
                }
                analysisSem.acquire();
                acquired = true;
                if (DEBUG) {
                    FindbugsPlugin.log("Acquired analysisSem");
                }
                if(monitor.isCanceled()){
                    return Status.CANCEL_STATUS;
                }
            }
            runWithProgress(monitor);
        } catch (OperationCanceledException e) {
            // Do nothing when operation cancelled.
            return Status.CANCEL_STATUS;
        } catch (CoreException ex) {
            if (DEBUG) {
                FindbugsPlugin.getDefault().logException(ex, createErrorMessage());
            }
            return ex.getStatus();
        } catch (InterruptedException e) {
            return Status.CANCEL_STATUS;
        } finally {
            if(acquired){
                if (DEBUG) {
                    FindbugsPlugin.log("releasing analysisSem");
                }

                analysisSem.release();
            }
            monitor.done();
        }
        return Status.OK_STATUS;
    }
}
