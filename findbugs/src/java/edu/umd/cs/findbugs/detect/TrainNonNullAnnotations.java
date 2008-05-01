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
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.TrainingDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.NullnessAnnotationDatabase;

/**
 * Training detector to
 * store NonNull, PossiblyNull and CheckForNull annotations to database files.
 * 
 * @author David Hovemeyer
 * 
 * @deprecated AnnotationDatabases are being phased out, since
 *             annotations are now stored directly in the XClass/XMethod/XField objects.
 *             Resolving nullness annotations will be handled through the
 *             JSR-305 type qualifier code. 
 */
@Deprecated
public class TrainNonNullAnnotations extends BuildNonNullAnnotationDatabase
		implements Detector, TrainingDetector {


	public TrainNonNullAnnotations(BugReporter bugReporter) {
		super(AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase() instanceof NullnessAnnotationDatabase
				? (NullnessAnnotationDatabase) AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase() : null);
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
		// TODO: FIX for new version of annnotations
//		AnalysisContext.currentAnalysisContext().storePropertyDatabase(
//				getNonNullDatabase(),
//				AnalysisContext.DEFAULT_NONNULL_PARAM_DATABASE_FILENAME,
//				"non-null param database");
//		AnalysisContext.currentAnalysisContext().storePropertyDatabase(
//				getCheckForNullDatabase(),
//				AnalysisContext.DEFAULT_CHECK_FOR_NULL_PARAM_DATABASE_FILENAME,
//				"possibly-null param database");
//		AnalysisContext.currentAnalysisContext().storePropertyDatabase(
//				getNullReturnValueDatabase(),
//				AnalysisContext.DEFAULT_NULL_RETURN_VALUE_ANNOTATION_DATABASE,
//				"non-null and possibly-null return value database");
	}

}
