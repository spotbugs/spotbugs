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

import java.util.Map;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodFactory;
import edu.umd.cs.findbugs.ba.npe.NonNullParamProperty;
import edu.umd.cs.findbugs.ba.npe.NonNullParamPropertyDatabase;
import edu.umd.cs.findbugs.visitclass.AnnotationVisitor;

/**
 * Scan application classes for @NonNull annotations.
 * 
 * @author David Hovemeyer
 */
public class NoteNonNullAnnotations extends AnnotationVisitor implements NonReportingDetector {
	private static final boolean DEBUG = Boolean.getBoolean("fnd.debug.nullarg");
	
	private static final String NONNULL_ANNOTATION_CLASS = NonNull.class.getName().replace('.', '/');
	
	private BugReporter bugReporter;
	private NonNullParamPropertyDatabase database;
	
	public NoteNonNullAnnotations(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		if (database == null) {
			database = new NonNullParamPropertyDatabase();
			AnalysisContext.currentAnalysisContext().setNonNullParamDatabase(database);
		}
		
		classContext.getJavaClass().accept(this);
	}
	
	// TODO: non-null fields
	
	//@Override
	public void visitAnnotation(String annotationClass, Map<String, Object> map, boolean runtimeVisible) {
		// TODO: non-null return values
	}
	
	//@Override
	public void visitParameterAnnotation(int p, String annotationClass, Map<String, Object> map, boolean runtimeVisible) {
		if (!annotationClass.equals(NONNULL_ANNOTATION_CLASS))
			return;

		XMethod xmethod = XMethodFactory.createXMethod(this);
		if (DEBUG) {
			System.out.println("Parameter " + p + " @NonNull in " + xmethod.toString());
		}
		
		NonNullParamProperty property = database.getProperty(xmethod);
		if (property == null) {
			property = new NonNullParamProperty();
			database.setProperty(xmethod, property);
		}
		property.setNonNull(p, true);
	}

	public void report() {
	}

}
