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

import java.util.*;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class BadResultSetAccess extends BytecodeScanningDetector implements Constants2 {
	static final int SEEN_NOTHING = 0;
	static final int SEEN_ICONST_0 = 1;
	static final int SEEN_ANEWARRAY = 2;

	static private Set<String> dbFieldTypesSet = new HashSet<String>() 
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

	private BugReporter bugReporter;
	private int state = SEEN_NOTHING;
	private int iConst0_PC = 0;
	
	public BadResultSetAccess(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public void visit(Method obj) {
		state = SEEN_NOTHING;
		super.visit(obj);
	}

	public void sawOpcode(int seen) {
		switch (state) {
			case SEEN_NOTHING:
				if (seen == ICONST_0) {
					state = SEEN_ICONST_0;
					iConst0_PC = getPC();
				}
				break;
			
			case SEEN_ICONST_0:
				if ((seen == INVOKEINTERFACE)
				&&  (getClassConstantOperand().equals("java/sql/ResultSet"))) {
					String methodName = getNameConstantOperand();
					
					if (((getPC() - iConst0_PC) <= 1)
					&&  (methodName.startsWith("get"))
					&&  (dbFieldTypesSet.contains(methodName.substring(3)))) {
						state = SEEN_NOTHING;
						bugReporter.reportBug(new BugInstance("BRSA_BAD_RESULTSET_ACCESS", NORMAL_PRIORITY)
						        .addClassAndMethod(this)
						        .addSourceLine(this));
					}
					else if (((getPC() - iConst0_PC) <= 2)
					&&       (methodName.startsWith("update"))
					&&       (dbFieldTypesSet.contains(methodName.substring(6)))) {
						state = SEEN_NOTHING;
						bugReporter.reportBug(new BugInstance("BRSA_BAD_RESULTSET_ACCESS", NORMAL_PRIORITY)
						        .addClassAndMethod(this)
						        .addSourceLine(this));
					} 
				}
				if ((getPC() - iConst0_PC) > 2)     
					state = SEEN_NOTHING;
				break;
		}
	}
}

// vim:ts=4
