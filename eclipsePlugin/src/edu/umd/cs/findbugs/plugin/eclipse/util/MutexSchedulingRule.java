/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2006 University of Maryland
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */
package edu.umd.cs.findbugs.plugin.eclipse.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

/** a simple scheduling rule for mutually exclusivity, more-or-less copied from:
 *  http://help.eclipse.org/help30/topic/org.eclipse.platform.doc.isv/guide/runtime_jobs_rules.htm
 */
public class MutexSchedulingRule implements ISchedulingRule {

	// enable multicore
	private static final boolean MULTICORE = Runtime.getRuntime().availableProcessors() > 1;

	private static final int MAX_JOBS = Runtime.getRuntime().availableProcessors();

	private final IProject project;

	public MutexSchedulingRule(IProject project) {
		super();
		this.project = project;
	}

	public boolean isConflicting(ISchedulingRule rule) {
		if(rule instanceof MutexSchedulingRule) {
			MutexSchedulingRule mRule = (MutexSchedulingRule) rule;
			if(MULTICORE) {
				return mRule.project.equals(project) || tooManyJobsThere();
			}
			return true;
		}
		return false;
	}

	private static boolean tooManyJobsThere() {
		Job[] fbJobs = Job.getJobManager().find(MutexSchedulingRule.class);
		int runningCount = 0;
		for (Job job : fbJobs) {
			if(job.getState() == Job.RUNNING){
				runningCount ++;
			}
		}
		// TODO made this condition configurable
		return runningCount > MAX_JOBS;
	}

	public boolean contains(ISchedulingRule rule) {
		return isConflicting(rule);
		/* from the URL above: "If you do not need to create hierarchies of locks,
		   you can implement the contains method to simply call isConflicting." */
	}

}
