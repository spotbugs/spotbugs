/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.InvokeInstruction;

/**
 * Mark methodref constant pool entries of methods
 * that are likely to implement assertions.
 * This is useful for pruning likely false paths.
 *
 * @author David Hovemeyer
 */
public class AssertionMethods implements Constants {
	
	private static final boolean DEBUG = Boolean.getBoolean("assertionmethods.debug");
	
	/**
	 * Bitset of methodref constant pool indexes referring to likely assertion methods.
	 */
	private BitSet assertionMethodRefSet;

	private static class UserAssertionMethod {
		private String className;
		private String methodName;

		public UserAssertionMethod(String className, String methodName) {
			this.className = className;
			this.methodName = methodName;
		}

		public String getClassName() {
			return className;
		}

		public String getMethodName() {
			return methodName;
		}
	}

	private static final List<UserAssertionMethod> userAssertionMethodList = new ArrayList<UserAssertionMethod>();

	static {
		String userProperty = System.getProperty("findbugs.assertionmethods");
		if (userProperty != null) {
			StringTokenizer tok = new StringTokenizer(userProperty, ",");
			while (tok.hasMoreTokens()) {
				String fullyQualifiedName = tok.nextToken();
				int lastDot = fullyQualifiedName.lastIndexOf('.');
				if (lastDot < 0)
					continue;
				String className = fullyQualifiedName.substring(0, lastDot);
				String methodName = fullyQualifiedName.substring(lastDot + 1);
				userAssertionMethodList.add(new UserAssertionMethod(className, methodName));
			}
		}
	}

	/**
	 * Constructor.
	 *
	 * @param jclass the JavaClass containing the methodrefs
	 */
	public AssertionMethods(JavaClass jclass) {
		this.assertionMethodRefSet = new BitSet();
		init(jclass);
	}

	private void init(JavaClass jclass) {
		ConstantPool cp = jclass.getConstantPool();
		int numConstants = cp.getLength();
		for (int i = 0; i < numConstants; ++i) {
			try {
				Constant c = cp.getConstant(i);
				if (c instanceof ConstantMethodref) {
					ConstantMethodref cmr = (ConstantMethodref) c;
					ConstantNameAndType cnat = (ConstantNameAndType) cp.getConstant(cmr.getNameAndTypeIndex(), CONSTANT_NameAndType);
					String methodName = ((ConstantUtf8) cp.getConstant(cnat.getNameIndex(), CONSTANT_Utf8)).getBytes();
					String className = cp.getConstantString(cmr.getClassIndex(), CONSTANT_Class).replace('/', '.');
					String methodSig = ((ConstantUtf8) cp.getConstant(cnat.getSignatureIndex(), CONSTANT_Utf8)).getBytes();
					
					String classNameLC = className.toLowerCase();
					String methodNameLC = methodName.toLowerCase();
					boolean voidReturnType = methodSig.endsWith(")V");
					
					if (DEBUG) {
						System.out.print("Is " + className + "." + methodName + " assertion method: ");
					}

					if (isUserAssertionMethod(className, methodName) ||
					        // voidReturnType &&  // comment this out for now
					        (classNameLC.indexOf("assert") >= 0 ||
					        methodNameLC.startsWith("throw") ||
					        methodNameLC.equals("insist")  ||
					        methodNameLC.equals("usage")  ||methodNameLC.indexOf("assert") >= 0 || methodNameLC.indexOf("error") >= 0 ||
					        methodNameLC.indexOf("abort") >= 0 || methodNameLC.indexOf("check") >= 0 ||
					        methodNameLC.indexOf("failed") >= 0)) {
						assertionMethodRefSet.set(i);
						if (DEBUG) {
							System.out.println("==> YES");
						}
					} else {
						if (DEBUG) {
							System.out.println("==> NO");
						}
					}
				}
			} catch (ClassFormatException e) {
				// FIXME: should report
			}
		}
	}

	private static boolean isUserAssertionMethod(String className, String methodName) {
		for (UserAssertionMethod uam : userAssertionMethodList) {
			if (className.equals(uam.getClassName()) && methodName.equals(uam.getMethodName()))
				return true;
		}
		return false;
	}

	/**
	 * Does the given InvokeInstruction refer to a likely assertion method?
	 *
	 * @param inv the InvokeInstruction
	 * @return true if the instruction likely refers to an assertion, false if not
	 */
	public boolean isAssertionCall(InvokeInstruction inv) {
//		if (DEBUG) {
//			System.out.print("Checking if " + inv + " is an assertion method: ");
//		}
		boolean isAssertionMethod = assertionMethodRefSet.get(inv.getIndex());
//		if (DEBUG) {
//			System.out.println("==> " + isAssertionMethod);
//		}
		return isAssertionMethod;
	}
}

// vim:ts=4
