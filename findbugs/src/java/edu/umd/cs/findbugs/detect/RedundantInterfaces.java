/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
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
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import java.util.*;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

public class RedundantInterfaces extends PreorderVisitor implements Detector, StatelessDetector
{
	private BugReporter bugReporter;
	
	public RedundantInterfaces(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	@Override
         public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	public void visitClassContext(ClassContext classContext) {
		JavaClass obj = classContext.getJavaClass();

		String superClassName = obj.getSuperclassName();
		if (superClassName.equals("java.lang.Object"))
			return;
		
		String[] interfaceNames = obj.getInterfaceNames();
		if ((interfaceNames == null) || (interfaceNames.length == 0))
			return;

		try {
			JavaClass superObj = obj.getSuperClass();
			SortedSet<String> redundantInfNames = new TreeSet<String>();

			for (String interfaceName : interfaceNames) {
				JavaClass inf = Repository.lookupClass(interfaceName.replace('/', '.'));
				if (superObj.instanceOf(inf))
					redundantInfNames.add(inf.getClassName());
			}
				
			if (redundantInfNames.size() > 0) {
				BugInstance bug = new BugInstance( this, "RI_REDUNDANT_INTERFACES", LOW_PRIORITY )
							.addClass(obj);
				for (String redundantInfName : redundantInfNames)
					bug.addClass(redundantInfName).describe("INTERFACE_TYPE");
					
				bugReporter.reportBug(bug);
			}

		} catch (ClassNotFoundException cnfe) {
			bugReporter.reportMissingClass(cnfe);
		}
	}
	
	public void report() {
	}
}
