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

import java.util.*;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;
import org.apache.bcel.classfile.*;

/**
 * Mark methodref constant pool entries of methods
 * that are likely to implement assertions.
 * This is useful for pruning likely false paths.
 *
 * @author David Hovemeyer
 */
public class AssertionMethods implements Constants {
	/** Bitset of methodref constant pool indexes referring to likely assertion methods. */
	private BitSet assertionMethodRefSet;

	/**
	 * Constructor.
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
					ConstantNameAndType cnat = (ConstantNameAndType)cp.getConstant(cmr.getNameAndTypeIndex(), CONSTANT_NameAndType);
					String methodName = ((ConstantUtf8) cp.getConstant(cnat.getNameIndex(), CONSTANT_Utf8)).getBytes();
					String className = cp.getConstantString(cmr.getClassIndex(), CONSTANT_Class).replace('/', '.');

					if (className.indexOf("assert") >= 0 ||
						methodName.indexOf("assert") >= 0 || methodName.indexOf("error") >= 0 ||
						methodName.indexOf("abort") >= 0 || methodName.indexOf("check") >= 0 ||
						methodName.indexOf("failed") >= 0)
						assertionMethodRefSet.set(i);
				}
			} catch (ClassFormatException e) {
				// FIXME: should report
			}
		}
	}

	/**
	 * Does the given InvokeInstruction refer to a likely assertion method?
	 * @param inv the InvokeInstruction
	 * @return true if the instruction likely refers to an assertion, false if not
	 */
	public boolean isAssertionCall(InvokeInstruction inv) {
		return assertionMethodRefSet.get(inv.getIndex());
	}
}

// vim:ts=4
