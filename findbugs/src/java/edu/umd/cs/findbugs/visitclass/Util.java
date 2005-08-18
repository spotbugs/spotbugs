/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.visitclass;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * @author pugh
 */
public class Util {
	/**
	 * Determine the outer class of obj.
	 * @param obj
	 * @return JavaClass for outer class, or null if obj is not an outer class
	 * @throws ClassNotFoundException
	 */
			
	@CheckForNull public static JavaClass getOuterClass(JavaClass obj) throws ClassNotFoundException {
		for(Attribute a : obj.getAttributes()) 
			if (a instanceof InnerClasses) {
				for(InnerClass ic :  ((InnerClasses) a).getInnerClasses()) {
					if (obj.getClassNameIndex() == ic.getInnerClassIndex()) {
//						System.out.println("Outer class is " + ic.getOuterClassIndex());
						ConstantClass oc = (ConstantClass) obj.getConstantPool().getConstant(ic.getOuterClassIndex());
						String ocName = oc.getBytes(obj.getConstantPool());
						return Repository.lookupClass(ocName);
					}
				}
			}
		return null;
	}

}
