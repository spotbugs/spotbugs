/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.util.Set;
import java.util.TreeSet;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * @author pwilliam
 */
public class PutfieldScanner {



	public static Set<Integer> getPutfieldsFor(JavaClass theClass, Method method, XField field) {
		Scanner scanner = new Scanner(theClass, method, field);
		
		scanner.execute();
		return scanner.putfields;
		
	}

	static class Scanner extends OpcodeStackDetector {

		Set<Integer> putfields = new TreeSet<Integer>();
		public Scanner(JavaClass theClass, Method targetMethod, XField target) {
			this.theClass = theClass;
			this.targetMethod = targetMethod;
			this.targetField = target;
		}

		final JavaClass theClass;

		final Method targetMethod;

		final XField targetField;

		@Override
		public void sawOpcode(int seen) {
			if (seen == PUTFIELD && getXFieldOperand().equals(targetField) 
					&& stack.getStackItem(1).getRegisterNumber() == 0)
				putfields.add(getPC());
				
			
		}

		@Override
		public void visitJavaClass(JavaClass obj) {
			setupVisitorForClass(obj);
			getConstantPool().accept(this);
			doVisitMethod(targetMethod);
		}

		public void execute() {
			theClass.accept(this);
		}
	}

}
