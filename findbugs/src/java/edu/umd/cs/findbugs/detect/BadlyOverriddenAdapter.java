/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2004,2005 University of Maryland
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

import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;

public class BadlyOverriddenAdapter extends BytecodeScanningDetector implements  StatelessDetector {
	private BugReporter bugReporter;
	private boolean isAdapter;
	private Map<String, String> methodMap;
	private Map<String, BugInstance> badOverrideMap;

	public BadlyOverriddenAdapter(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		methodMap = new HashMap<String, String>();
		badOverrideMap = new HashMap<String,BugInstance>();
	}
	
	@Override
         public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
         public void visit(JavaClass obj) {
		try {
			methodMap.clear();
			badOverrideMap.clear();
			JavaClass superClass = obj.getSuperClass();
			if (superClass == null) return;
			String packageName = superClass.getPackageName();
			String className = superClass.getClassName();
			
			//A more generic way to add Adapters would be nice here
			isAdapter = ((className.endsWith("Adapter")) && (packageName.equals("java.awt.event") || packageName.equals("javax.swing.event")))
			          ||((className.equals("DefaultHandler") && (packageName.equals("org.xml.sax.helpers"))));
			if (isAdapter) {
				Method[] methods = superClass.getMethods();
				for (Method method1 : methods) {
					methodMap.put(method1.getName(), method1.getSignature());
				}
			}
		} catch (ClassNotFoundException cnfe) {
			bugReporter.reportMissingClass(cnfe);
		}
	}
	
	@Override
         public void visitAfter(JavaClass obj) {
		for (BugInstance bi : badOverrideMap.values()) {
			if (bi != null)
				bugReporter.reportBug(bi);
		}
	}

	@Override
         public void visit(Method obj) {
		if (isAdapter) {
			String methodName = obj.getName();
			String signature = methodMap.get(methodName);
			if (!methodName.equals("<init>") && signature != null) {
				if (!signature.equals(obj.getSignature())) {
					if (!badOverrideMap.keySet().contains(methodName)) {
						badOverrideMap.put(methodName, new BugInstance(this, "BOA_BADLY_OVERRIDDEN_ADAPTER", NORMAL_PRIORITY)
								.addClassAndMethod(this)
								.addSourceLine(this));
					}
				}
				else {
					badOverrideMap.put(methodName, null);
				}
			}
		}
	}
}

// vim:ts=4
