/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Deprecated;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class Methods extends PreorderVisitor implements Detector,
		NonReportingDetector {
	
	private XFactory xFactory = AnalysisContext.currentXFactory();

	XMethod m;
	XField f;
	public Methods(BugReporter bugReporter) {
	}

	public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);
	}

	@Override
	public void visit(Method obj) {
		m = XFactory.createXMethod(this);
	}
	@Override 
	public void visit(Attribute a) {
		if (a instanceof Deprecated) {
			if (visitingMethod()) xFactory.deprecate(m);
			else if (visitingField())xFactory.deprecate(f);
		}
	}

	@Override
	public void visit(Field obj) {
		f = XFactory.createXField(this);
	}

	public void report() {

	}

}
