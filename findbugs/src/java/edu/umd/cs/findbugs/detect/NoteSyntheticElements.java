/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Synthetic;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.SyntheticElements;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * Note synthetic methods/fields/classes in application and referenced classes.
 * Detectors that use AnnotationDatabases should run in a later pass
 * than this detector. 
 * 
 * @author David Hovemeyer
 * @author Bill Pugh
 */
public class NoteSyntheticElements extends PreorderVisitor implements Detector, NonReportingDetector {

	private BugReporter bugReporter;
	private SyntheticElements database;
	
	public NoteSyntheticElements(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
		// nothing to do
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba.ClassContext)
	 */
	public void visitClassContext(ClassContext classContext) {
		if (database == null) {
			database = new SyntheticElements();
			Global.getAnalysisCache().eagerlyPutDatabase(SyntheticElements.class, database);
		}
		
		classContext.getJavaClass().accept(this);
	}

	@Override
	public void visit(Synthetic a) {
		if (visitingMethod()) {
			database.addVisitedMethod(this);
		} else if (visitingField()) {
			database.addVisitedField(this);
		} else {
			database.addVisitedClass(this);
		}
	}
	
	@Override
	public void visit(JavaClass obj) {
		if (obj.isSynthetic()) {
			database.addVisitedClass(this);
		}
	}
	
	@Override
	public void visit(Field f) {
		if (f.isSynthetic()) {
			database.addVisitedField(this);
		}
	}

	@Override
	public void visit(Method m) {
		if (m.isSynthetic()) {
			database.addVisitedMethod(this);
		}
	}

}
