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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import edu.umd.cs.findbugs.plugin.eclipse.util.MutexSchedulingRule;

/**
 * @author Andrei
 */
public abstract class FindBugsJob extends Job {

	public FindBugsJob(String name, IProject project) {
		super(name);
		setRule(new MutexSchedulingRule(project));
	}

	@Override
	public boolean belongsTo(Object family) {
		return FindbugsPlugin.class == family;
	}

	public void scheduleInteractive(){
		setUser(true);
		setPriority(Job.INTERACTIVE);
		schedule();
	}

	protected String createErrorMessage(){
		return getName() + " failed";
	}

	abstract protected void runWithProgress(IProgressMonitor monitor) throws CoreException;

	@Override
	public IStatus run(IProgressMonitor monitor) {
		try {
			runWithProgress(monitor);
		} catch (OperationCanceledException e) {
			// Do nothing when operation cancelled.
			return Status.CANCEL_STATUS;
		} catch (CoreException ex) {
			FindbugsPlugin.getDefault().logException(ex, createErrorMessage());
			return ex.getStatus();
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}
}
