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
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;
import edu.umd.cs.pugh.visitclass.PreorderVisitor;

public class DroppedException extends PreorderVisitor implements Detector, Constants2 {
    private static final boolean DEBUG = Boolean.getBoolean("de.debug");

    static Set reported = new HashSet();
    static Set causes = new HashSet();
    static Set checkedCauses = new HashSet();
    private BugReporter bugReporter;

    public DroppedException(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

    public void visitClassContext(ClassContext classContext) {
	classContext.getJavaClass().accept(this);
	}

    public void report() { }

    static boolean isChecked(String c) {
	if (!causes.add(c)) return checkedCauses.contains(c);
	try {
		Class cl = Class.forName(c);
		if (Exception.class.isAssignableFrom(cl)
			&& !RuntimeException.class.isAssignableFrom(cl))
		   checkedCauses.add(c);
		   return true;
	    }
	catch (ClassNotFoundException e) {
		// just fall through
		}
	return false;
	}
		
		
    public void visit(Code obj) { 

	CodeException [] exp = obj.getExceptionTable();
	if (exp == null) return;
	byte [] code = obj.getCode();

	for(int i = 0; i < exp.length; i++)  {
	  int handled = exp[i].getHandlerPC();
	  int start = exp[i].getStartPC();
	  int end = exp[i].getEndPC();
	  int cause = exp[i].getCatchType();
	  boolean exitInTryBlock = false;

	  for(int j = start; j <= end;)   {
	    int opcode = asUnsignedByte(code[j]);
	    if (opcode >= IRETURN && opcode <= RETURN
		|| opcode >=IFEQ && opcode <= GOTO
				&& (opcode != GOTO || j < end)
			) {
			exitInTryBlock =  true;
			break;
			/*
			System.out.println("	exit: " + opcode 
				+ " in " + betterMethodName);
			*/
			}
		if (NO_OF_OPERANDS[opcode] < 0)  {
			exitInTryBlock = true;
			break;
			}
		j += 1+NO_OF_OPERANDS[opcode];
		}

	  if (exitInTryBlock) continue;
	  String c ;
	  if (cause == 0)
		c = "Throwable";
	  else
		{
		c = Utility.compactClassName(
		  constant_pool.getConstantString(cause, 
					CONSTANT_Class), false);
		if (!isChecked(c)) continue;
		}
	if (handled < 5) continue;
	/*
	if ( (0xff&code[handled]) == POP) {
		System.out.println( "DE:	" 
			+ betterMethodName
			+ " might ignore " + c
			+ " (" + (0xff&code[handled] )
			+ "," + (0xff&code[handled+1] )
			+")"
			);
		}
	else 
	*/
	int opcode = asUnsignedByte(code[handled]);
	 // System.out.println( "DE:	opcode is "  + opcode + ", " + asUnsignedByte(code[handled+1]));
	boolean drops = false;
	if ( opcode >= ASTORE_0
		&& opcode <= ASTORE_3
		 && asUnsignedByte(code[handled+1]) == RETURN) {
		if (DEBUG) System.out.println("Drop 1");
		drops = true;
		}
	if (handled+2 < code.length
		&&opcode == ASTORE
		 && asUnsignedByte(code[handled+2]) == RETURN) {
		drops = true;
		if (DEBUG) System.out.println("Drop 2");
		}
	if (handled+3 < code.length
	   && !exitInTryBlock) {
	if ( opcode >= ASTORE_0
		&& opcode <= ASTORE_3
		 && asUnsignedByte(code[handled-3]) == GOTO) {
		int offsetBefore = 
			asUnsignedByte(code[handled-2]) << 8
			| asUnsignedByte(code[handled-1]);
		if (offsetBefore == 4) {
			drops = true;
			if (DEBUG) System.out.println("Drop 3");
			}
		}
	if ( opcode == ASTORE
		 && asUnsignedByte(code[handled-3]) == GOTO) {
		int offsetBefore = 
			asUnsignedByte(code[handled-2]) << 8
			| asUnsignedByte(code[handled-1]);
		if (offsetBefore == 5) {
			drops = true;
			if (DEBUG) System.out.println("Drop 4");
			}
		}
	if ( opcode >= ASTORE_0
		&& opcode <= ASTORE_3
		 && asUnsignedByte(code[handled+1]) == GOTO 
		 && asUnsignedByte(code[handled-3]) == GOTO) {
		int offsetBefore = 
			asUnsignedByte(code[handled-2]) << 8
			| asUnsignedByte(code[handled-1]);
		int offsetAfter = 
			asUnsignedByte(code[handled+2]) << 8
			| asUnsignedByte(code[handled+3]);
	
		if (offsetAfter > 0 && offsetAfter+4 == offsetBefore)	  {
			drops = true;
			if (DEBUG) System.out.println("Drop 5");
			}
		}

	if ( opcode == ASTORE
		 && asUnsignedByte(code[handled+2]) == GOTO 
		 && asUnsignedByte(code[handled-3]) == GOTO) {
		int offsetBefore = 
			asUnsignedByte(code[handled-2]) << 8
			| asUnsignedByte(code[handled-1]);
		int offsetAfter = 
			asUnsignedByte(code[handled+3]) << 8
			| asUnsignedByte(code[handled+4]);
	
		if (offsetAfter > 0 && offsetAfter+5 == offsetBefore)	  {
			drops = true;
			if (DEBUG) System.out.println("Drop 6");
			}
		}




	}
	if (end-start >= 5 && drops && !c.equals("java.lang.InterruptedException")
			&& !c.equals("java.lang.CloneNotSupportedException")) {
	String key = (exitInTryBlock ? "mightDrop," : "mightIgnore,") + betterMethodName + "," + c;
	if (reported.add(key)) {
		bugReporter.reportBug(
			new BugInstance(exitInTryBlock ? "DE_MIGHT_DROP" : "DE_MIGHT_IGNORE", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addSourceLine(this, handled)
				.addClass(c).describe("CLASS_EXCEPTION"));
	}

	}
		}
}
}
