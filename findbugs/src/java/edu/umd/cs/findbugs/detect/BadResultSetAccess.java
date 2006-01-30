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

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.StatelessDetector;


public class BadResultSetAccess extends BytecodeScanningDetector implements  StatelessDetector {

	private static Set<String> dbFieldTypesSet = new HashSet<String>() 
	{
		static final long serialVersionUID = -3510636899394546735L;
		{ 
		add("Array");
		add("AsciiStream");
		add("BigDecimal");
		add("BinaryStream");
		add("Blob");
		add("Boolean");
		add("Byte");
		add("Bytes");
		add("CharacterStream");
		add("Clob");
		add("Date");
		add("Double");
		add("Float");
		add("Int");
		add("Long");
		add("Object");
		add("Ref");
		add("Short");
		add("String");
		add("Time");
		add("Timestamp");
		add("UnicodeStream");
		add("URL");
		}
	};
	
	private OpcodeStack stack = new OpcodeStack();
	private BugReporter bugReporter;
	
	public BadResultSetAccess(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	public void visit(Method obj) {
        stack.resetForMethodEntry(this);
		super.visit(obj);
	}

	public void sawOpcode(int seen) {
		stack.mergeJumps(this);
		try {
			if (seen == INVOKEINTERFACE) {
				String methodName = getNameConstantOperand();
				String clsConstant = getClassConstantOperand();
				if  ((clsConstant.equals("java/sql/ResultSet") && 
						((methodName.startsWith("get") && dbFieldTypesSet.contains(methodName.substring(3))) ||  
						 (methodName.startsWith("update") && dbFieldTypesSet.contains(methodName.substring(6)))))
			    ||   ((clsConstant.equals("java/sql/PreparedStatement") &&  
			    		((methodName.startsWith("set") && dbFieldTypesSet.contains(methodName.substring(3))))))) {
					String signature = getSigConstantOperand();
					Type[] argTypes = Type.getArgumentTypes(signature);
					int numParms = argTypes.length;
					if (stack.getStackDepth() >= numParms) {
						OpcodeStack.Item item = stack.getStackItem(numParms-1);
						Object cons = item.getConstant();
						if ((cons != null) && ("I".equals(item.getSignature())) && (((Integer) cons).intValue() == 0)) {
							bugReporter.reportBug(new BugInstance(this, "BRSA_BAD_RESULTSET_ACCESS", NORMAL_PRIORITY)
							        .addClassAndMethod(this)
							        .addSourceLine(this));
						}
					}
				}
			}
		} finally {
			stack.sawOpcode(this, seen);
		}
	}
}

// vim:ts=4
