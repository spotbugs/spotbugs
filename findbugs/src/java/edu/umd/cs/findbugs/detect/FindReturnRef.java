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
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class FindReturnRef extends BytecodeScanningDetector implements   Constants2 {
    boolean check = false;
    boolean thisOnTOS = false;
    boolean fieldOnTOS = false;
    boolean staticMethod = false;
    String nameOnStack;
    String classNameOnStack;
    String sigOnStack;
    boolean fieldIsStatic;
    private BugReporter bugReporter;

    public FindReturnRef(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

  public void visit(JavaClass obj)     {
	super.visit(obj);
	}

    public void visit(Method obj) {
        check =  (obj.getAccessFlags() & (ACC_PUBLIC )) != 0;
        staticMethod =  (obj.getAccessFlags() & (ACC_STATIC)) != 0;
        if (check) 
		super.visit(obj);
        thisOnTOS = false;
        fieldOnTOS = false;
	}


    public void sawOpcode(int seen) {
	/*
	System.out.println("Saw " + OPCODE_NAMES[seen] + "	" 
			+ thisOnTOS
			+ "	"
			+ fieldOnTOS
			);
	*/
	if (seen == ALOAD_0)  {
		thisOnTOS = true;
		fieldOnTOS = false;
		return;
		}

	if (thisOnTOS && seen == GETFIELD && classConstant == className)  {
		fieldOnTOS = true;
		thisOnTOS = false;
		nameOnStack = nameConstant;
		classNameOnStack = betterClassConstant;
		sigOnStack = sigConstant;
		fieldIsStatic = false;
		// System.out.println("Saw getfield");
		return;
		}
	if (seen == GETSTATIC && classConstant == className)  {
		fieldOnTOS = true;
		thisOnTOS = false;
		nameOnStack = nameConstant;
		classNameOnStack = betterClassConstant;
		sigOnStack = sigConstant;
		fieldIsStatic = true;
		return;
		}
	thisOnTOS = false;
	if (check && fieldOnTOS && seen == ARETURN 
		&& !sigOnStack.equals("Ljava/lang/String;")
		&& sigOnStack.indexOf("Exception") == -1
		&& sigOnStack.indexOf("[") >= 0
		) {
			bugReporter.reportBug(new BugInstance(staticMethod ? "MS_EXPOSE_REP" : "EI_EXPOSE_REP", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addField(classNameOnStack, nameOnStack, sigOnStack, fieldIsStatic)
				.addSourceLine(this));
		}

	fieldOnTOS = false;
	thisOnTOS = false;
	}


}	
