/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class FindUninitializedGet extends BytecodeScanningDetector implements   Constants2 {
    HashSet<FieldAnnotation> initializedFields = new HashSet<FieldAnnotation>();
    HashSet<FieldAnnotation> declaredFields = new HashSet<FieldAnnotation>();
    boolean inConstructor;
    boolean thisOnTOS = false;
    private BugReporter bugReporter;

    private static final int UNKNOWN_PRIORITY = -1;

  public FindUninitializedGet(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
  }


  public void visit(JavaClass obj)     {
	declaredFields.clear();
	super.visit(obj);
	}

    public void visit(Field obj) {
        super.visit(obj);
	//declaredFields.add(fieldName);
	FieldAnnotation f = FieldAnnotation.fromVisitedField(this);
	declaredFields.add(f);
	/*
	System.out.println("Visiting " + fieldName);
	*/
	}
    public void visit(Method obj) {
        super.visit(obj);
	initializedFields.clear();
	/*
	System.out.println("Visiting " + methodName);
	*/
        thisOnTOS = false;
	inConstructor = methodName.equals("<init>")
			&& methodSig.indexOf(className) == -1;
	/*
	System.out.println("methodName: " + methodName);
	System.out.println("methodSig: " + methodSig);
	System.out.println("inConstructor: " + inConstructor);
	*/
	}


    public void sawOpcode(int seen) {
	if (!inConstructor) return;

	/*
	System.out.println("thisOnTOS:" + thisOnTOS);
	System.out.println("seen:" + seen);
	*/
	if (seen == ALOAD_0)  {
		thisOnTOS = true;
		/*
		System.out.println("set thisOnTOS");
		*/
		return;
		}

/*
	if (thisOnTOS && seen == GETFIELD) {
		System.out.println("Saw getfield of " + classConstant 
				+ "." + nameConstant);
		if (initializedFields.contains(nameConstant))
		    System.out.println("   initialized");
		if (declaredFields.contains(nameConstant))
		    System.out.println("   declared");
		}
*/
			
	if (seen == PUTFIELD && classConstant.equals(className))
		initializedFields.add(FieldAnnotation.fromReferencedField(this));

	else if (thisOnTOS && seen == GETFIELD && classConstant.equals(className)) {
		FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
		if (!initializedFields.contains(f) && declaredFields.contains(f))  {
	  		bugReporter.reportBug(new BugInstance("UR_UNINIT_READ", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addField(f)
				.addSourceLine(this));
			  initializedFields.add(FieldAnnotation.fromReferencedField(this));
			}
		}

	else if (
		(seen == INVOKESPECIAL  
			&& !(nameConstant.equals("<init>")
					&& !classConstant.equals(className))
				)
		 || (seen == INVOKESTATIC  
			&& nameConstant.equals("doPrivileged")
			&& classConstant.equals("java/security/AccessController")
				)
		 || (seen == INVOKEVIRTUAL
				&& classConstant.equals(className))) {
			/*
			System.out.println("Saw invocation of " 
				+ classConstant + "." + nameConstant
				+ " in " + className + "." + methodName);
			*/
			inConstructor = false;
			}

        thisOnTOS = false;
	}
	}	
