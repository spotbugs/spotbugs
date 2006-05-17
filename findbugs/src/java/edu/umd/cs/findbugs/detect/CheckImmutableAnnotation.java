/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.JCIPAnnotationDatabase;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

import org.apache.bcel.classfile.*;

public class CheckImmutableAnnotation extends PreorderVisitor implements
		Detector {

	BugReporter bugReporter;

	public CheckImmutableAnnotation(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visitJavaClass(JavaClass obj) {
		JCIPAnnotationDatabase jcipAnotationDatabase = AnalysisContext
				.currentAnalysisContext().getJCIPAnnotationDatabase();
		if (jcipAnotationDatabase.hasClassAnnotation(obj.getClassName()
				.replace('/', '.'), "Immutable"))
			super.visitJavaClass(obj);
	}

	@Override
	public void visit(Field obj) {
		if (!obj.isFinal())
			bugReporter.reportBug(new BugInstance(this, "JCIP_FIELD_ISNT_FINAL_IN_IMMUTABLE_CLASS",
					NORMAL_PRIORITY).addClass(this).addVisitedField(this));
	}


	public void report() {
	
	}


	public void visitClassContext(ClassContext classContext) {
		 classContext.getJavaClass().accept(this);
		
	}

}
