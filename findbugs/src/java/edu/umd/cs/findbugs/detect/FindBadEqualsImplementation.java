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
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessProperty;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase;

/**
 * Find equals(Object) methods that unconditionally dereference the parameter,
 * rather than returning false if it's null.
 * 
 * @author David Hovemeyer
 */
public class FindBadEqualsImplementation implements Detector {
	
	private BugReporter bugReporter;
	private ParameterNullnessPropertyDatabase database;
	private boolean checkedDatabase;
	
	public FindBadEqualsImplementation(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba.ClassContext)
	 */
	public void visitClassContext(ClassContext classContext) {
		if (!checkedDatabase) {
			database = AnalysisContext.currentAnalysisContext().getUnconditionalDerefParamDatabase();
			checkedDatabase = true;
		}
		
		if (database == null)
			return;
		
		JavaClass javaClass = classContext.getJavaClass();
		Method[] methodList = javaClass.getMethods();
		for (Method method : methodList) {
			if (!isEqualsMethod(method))
				continue;

			XMethod xmethod = XFactory.createXMethod(javaClass, method);
			ParameterNullnessProperty property = database.getProperty(xmethod);
			if (property == null)
				continue;

			if (property.isNonNull(0)) {
				BugInstance warning = new BugInstance(this, "NP_DOES_NOT_HANDLE_NULL", NORMAL_PRIORITY)
						.addClassAndMethod(javaClass, method);
				bugReporter.reportBug(warning);
			}
		}
	}

	private boolean isEqualsMethod(Method method) {
		return method.getName().equals("equals")
			&& method.getSignature().equals("(Ljava/lang/Object;)Z")
			&& !method.isStatic();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
		// TODO Auto-generated method stub

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
