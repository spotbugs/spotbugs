/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.TrainingDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;

/**
 * Training detector to
 * store NonNull and PossiblyNull annotations to database files.
 * 
 * @author David Hovemeyer
 */
public class TrainNonNullAnnotations extends BuildNonNullAnnotationDatabase
		implements TrainingDetector {
	
	BugReporter bugReporter;
	
	public TrainNonNullAnnotations(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba.ClassContext)
	 */
	public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
		AnalysisContext.currentAnalysisContext().storePropertyDatabase(
				getNonNullDatabase(),
				AnalysisContext.DEFAULT_NONNULL_PARAM_DATABASE_FILENAME,
				"non-null param database");
		AnalysisContext.currentAnalysisContext().storePropertyDatabase(
				getPossiblyNullDatabase(),
				AnalysisContext.DEFAULT_POSSIBLYNULL_PARAM_DATABASE_FILENAME,
				"possibly-null param database");
	}

}
