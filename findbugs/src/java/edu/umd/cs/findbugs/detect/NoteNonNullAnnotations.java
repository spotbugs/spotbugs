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

import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.NullnessAnnotationDatabase;
import edu.umd.cs.findbugs.bcel.BCELUtil;

/**
 * Scan classes for @NonNull, @PossiblyNull and @CheckForNull annotations,
 * and convey them to FindNullDeref.
 * 
 * @deprecated AnnotationDatabases are being phased out, since
 *             annotations are now stored directly in the XClass/XMethod/XField objects.
 *             Resolving nullness annotations will be handled through the
 *             JSR-305 type qualifier code. 
 */
public class NoteNonNullAnnotations
	extends BuildNonNullAnnotationDatabase
	implements Detector, NonReportingDetector {

	public NoteNonNullAnnotations(BugReporter bugReporter) {
		super(AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase() instanceof NullnessAnnotationDatabase
				? (NullnessAnnotationDatabase) AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase() : null);
	}

	public void visitClassContext(ClassContext classContext) {

		JavaClass javaClass = classContext.getJavaClass();
		if  (!BCELUtil.preTiger(javaClass)) javaClass.accept(this);
	}

	public void report() {
	}
}
