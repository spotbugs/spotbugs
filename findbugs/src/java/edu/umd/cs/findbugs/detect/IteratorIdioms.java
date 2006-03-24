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


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.ClassContext;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

public class IteratorIdioms extends BytecodeScanningDetector implements  StatelessDetector {

    private JavaClass iteratorClass;
	private BugReporter bugReporter;
	
	public IteratorIdioms(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
         public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
         public void visitClassContext(ClassContext classContext) {
		findJavaUtilIterator();

	    if (iteratorClass == null)
	        return;
	    try {
		    JavaClass cls = classContext.getJavaClass();
		    if (cls.implementationOf(iteratorClass))
		        super.visitClassContext(classContext); 
	    }
	    catch (ClassNotFoundException cnfe) {
	        //Already logged
	    }
	}

	private void findJavaUtilIterator() {
		if (iteratorClass == null) {
			try {
				iteratorClass = Repository.lookupClass("java.util.Iterator");
			} catch (ClassNotFoundException cnfe) {
				iteratorClass = null;
				bugReporter.reportMissingClass(cnfe);
			}
		}
	}
	
	boolean sawNoSuchElement;
	boolean sawCall;

	@Override
         public void visit(Code obj) {
		if (getMethodName().equals("next")
		        && getMethodSig().equals("()Ljava/lang/Object;")) {
			sawNoSuchElement = false;
			sawCall = false;
			super.visit(obj);
			if (!sawNoSuchElement)

				bugReporter.reportBug(new BugInstance(this, "IT_NO_SUCH_ELEMENT", sawCall ? LOW_PRIORITY : NORMAL_PRIORITY).addClassAndMethod(this));
		}
	}


	@Override
         public void sawOpcode(int seen) {
		if (seen == NEW
		        && getClassConstantOperand().equals("java/util/NoSuchElementException"))
			sawNoSuchElement = true;
		else if (seen == INVOKESPECIAL
		        || seen == INVOKEVIRTUAL
		        || seen == INVOKEINTERFACE) {
			sawCall = true;
			// System.out.println("Saw call to " + nameConstant);
			if (getNameConstantOperand().toLowerCase().indexOf("next")  >= 0 || getNameConstantOperand().toLowerCase().indexOf("previous") >= 0 )
				sawNoSuchElement = true;
		}
	}
}
