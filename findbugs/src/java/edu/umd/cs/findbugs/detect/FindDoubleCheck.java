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
import java.io.PrintStream;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class FindDoubleCheck extends BytecodeScanningDetector implements   Constants2 {
    int stage = 0;
    int startPC, endPC;
    int count;
    boolean sawMonitorEnter;
    HashSet<FieldAnnotation> fields = new HashSet<FieldAnnotation>();
    HashSet<FieldAnnotation> twice = new HashSet<FieldAnnotation>();
    private BugReporter bugReporter;

    public FindDoubleCheck(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

    public void visit(Method obj) {
        super.visit(obj);
	fields.clear();
	twice.clear();
	stage = 0;
	count = 0;
	sawMonitorEnter = false;
	}

    public void sawOpcode(int seen) {

	if (seen == MONITORENTER) sawMonitorEnter = true;
	if (seen == GETFIELD || seen == GETSTATIC)  {
		FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
		if (!sawMonitorEnter) {
			fields.add(f);
			startPC = getPC();
			}
		else if(fields.contains(f))
			twice.add(f);
		}
	switch (stage) {
	 case 0:
		if (seen == IFNULL || seen == IFNONNULL) stage++;
		count = 0;
		break;
	 case 1:
		if (seen == MONITORENTER)  stage++;
		else { 
			count++;
			if (count > 10) stage = 0;
			}
		break;
	 case 2:
		if (seen == IFNULL || seen == IFNONNULL) {
			endPC = getPC();
			stage++;
			}
		else { 
			count++;
			if (count > 10) stage = 0;
			}
		break;
	 case 3:
		if (seen == PUTFIELD || seen == PUTSTATIC) {
			FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
			if (fields.contains(f) && !getNameConstantOperand().startsWith("class$")
					&& !getSigConstantOperand().equals("Ljava/lang/String;")) {
				Field declaration = findField(getClassConstantOperand(), getNameConstantOperand());
				/*
				System.out.println(f);
				System.out.println(declaration);
				System.out.println(getSigConstantOperand());
				*/
				if (declaration == null || !declaration.isVolatile())
				bugReporter.reportBug(new BugInstance("DC_DOUBLECHECK", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addField(f).describe("FIELD_ON")
					.addSourceLineRange(this, startPC, endPC));
				stage++;
				}
			}
		break;
	default:
	}
	}
	Field findField(String className, String fieldName) {
		try  {
		// System.out.println("Looking for " + className);
		JavaClass fieldDefinedIn = getThisClass();
		if (!className.equals(getClassName())) {
			// System.out.println("Using repository to look for " + className);
			
			fieldDefinedIn = Repository.lookupClass(className);
			}
		Field [] f = fieldDefinedIn.getFields();
		for(int i = 0; i < f.length; i++) 
			if (f[i].getName().equals(fieldName)) {
				// System.out.println("Found " + f[i]);
				return f[i];	
				}
		return null;
		}catch (ClassNotFoundException e) {
			return null;
			}
		}
		
}
