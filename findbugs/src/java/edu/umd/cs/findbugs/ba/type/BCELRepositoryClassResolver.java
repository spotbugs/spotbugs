/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.ba.type;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

/**
 * ClassResolver that uses the BCEL global Repository class.
 *
 * @author David Hovemeyer
 * @see ClassResolver
 * @see TypeRepository
 */
public class BCELRepositoryClassResolver implements ClassResolver {
	public void resolveClass(ClassType type, TypeRepository repos) throws ClassNotFoundException {
		// Find the representation of the class
		JavaClass javaClass = Repository.lookupClass(type.getClassName());

		// Determine whether the type is a class or an interface
		type.setIsInterface(javaClass.isInterface());

		// Set superclass link (if any)
		int superclassIndex = javaClass.getSuperclassNameIndex();
		if (superclassIndex > 0) {
			// Type has a superclass
			String superclassName = getClassString(javaClass, superclassIndex);
			repos.addSuperclassLink(type, repos.classTypeFromSlashedClassName(superclassName));
		}

		// Set interface links (if any)
		int[] interfaceIndexList = javaClass.getInterfaceIndices();
		for (int i = 0; i < interfaceIndexList.length; ++i) {
			int index = interfaceIndexList[i];
			String interfaceName = getClassString(javaClass, index);
			repos.addInterfaceLink(type, repos.classTypeFromSlashedClassName(interfaceName));
		}
	}

	private static String getClassString(JavaClass javaClass, int index) {
		return javaClass.getConstantPool()
		        .getConstantString(index, Constants.CONSTANT_Class);
	}
}

// vim:ts=4
