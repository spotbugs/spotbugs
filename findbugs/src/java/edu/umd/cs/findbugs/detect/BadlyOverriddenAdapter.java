/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2004 University of Maryland
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
import java.util.*;
import org.apache.bcel.classfile.*;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class BadlyOverriddenAdapter extends BytecodeScanningDetector implements   Constants2 {
    private BugReporter bugReporter;
    private boolean isAdapter;
    private boolean classReported;
    private Map<String,String> methodMap;
    
    public BadlyOverriddenAdapter(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        methodMap = new HashMap<String,String>();
    }

    public void visit(JavaClass obj) {
    	try {
	    	methodMap.clear();
			JavaClass superClass = obj.getSuperClass();
			String packageName = superClass.getPackageName();
			String className = superClass.getClassName();
			isAdapter = ((className.endsWith("Adapter")) && (packageName.equals( "java.awt.event") || packageName.equals( "javax.swing.event" )));
			if (isAdapter) {
				Method[] methods = superClass.getMethods();
				for (int i = 0; i < methods.length; i++) {
					methodMap.put(methods[i].getName(), methods[i].getSignature());
				}
				classReported = false;
			}
		}
		catch (ClassNotFoundException cnfe) {
            bugReporter.reportMissingClass(cnfe);
		}
    }
    
    public void visit(Method obj) {
    	if (isAdapter && !classReported) {
	    	String methodName = obj.getName();
	    	String signature = methodMap.get(methodName);
	    	if (!methodName.equals("<init>") && signature != null) {
	    		if (!signature.equals(obj.getSignature())) {
					bugReporter.reportBug(new BugInstance("BOA_BADLY_OVERRIDDEN_ADAPTER", NORMAL_PRIORITY)
										.addClassAndMethod(this)
										.addSourceLine(this));
					classReported = true;
	    		}
	    	}
	    }
    }
}

// vim:ts=4
