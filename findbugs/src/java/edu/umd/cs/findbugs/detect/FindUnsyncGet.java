/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

public class FindUnsyncGet extends BytecodeScanningDetector {
	String prevClassName = " none ";
	private BugReporter bugReporter;
	static final int doNotConsider = ACC_PRIVATE | ACC_STATIC | ACC_NATIVE;

	// Maps of property names to get and set methods
	private HashMap<String, MethodAnnotation> getMethods = new HashMap<String, MethodAnnotation>();
	private HashMap<String, MethodAnnotation> setMethods = new HashMap<String, MethodAnnotation>();

	public FindUnsyncGet(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
         public void report() {
		// Find the set of properties for which we have both
		// unsynchronized get and synchronized set methods
		Set<String> commonProperties = new HashSet<String>(getMethods.keySet());
		commonProperties.retainAll(setMethods.keySet());

		// Report method pairs
		for (String propName : commonProperties) {
			MethodAnnotation getMethod = getMethods.get(propName);
			MethodAnnotation setMethod = setMethods.get(propName);

			bugReporter.reportBug(new BugInstance(this, "UG_SYNC_SET_UNSYNC_GET", NORMAL_PRIORITY)
					.addClass(prevClassName)
					.addMethod(getMethod)
					.addMethod(setMethod));
		}
		getMethods.clear();
		setMethods.clear();
	}

	@Override
         public void visit(JavaClass obj) {
		report();
		prevClassName = getDottedClassName();
	}

	@Override
         public void visit(Method obj) {
		int flags = obj.getAccessFlags();
		if ((flags & doNotConsider) != 0) return;
		String name = obj.getName();
		boolean isSynchronized = (flags & ACC_SYNCHRONIZED) != 0;
		/*
		String sig = obj.getSignature();
		char firstArg = sig.charAt(1);
		char returnValue = sig.charAt(1 + sig.indexOf(')'));
		boolean firstArgIsRef = (firstArg == 'L') || (firstArg == '[');
		boolean returnValueIsRef = (returnValue == 'L') || (returnValue == '[');

		System.out.println(className + "." + name
				+ " " +  firstArgIsRef
				+ " " +  returnValueIsRef
				+ " " + isSynchronized
				+ " " + isNative
				);
		*/
		if (name.startsWith("get")
		        && !isSynchronized
		// && returnValueIsRef
		) {
			getMethods.put(name.substring(3), MethodAnnotation.fromVisitedMethod(this));
		} else if (name.startsWith("set")
		        && isSynchronized
		// && firstArgIsRef
		) {
			setMethods.put(name.substring(3), MethodAnnotation.fromVisitedMethod(this));
		}
	}
}
