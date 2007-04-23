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

package edu.umd.cs.findbugs.visitclass;

import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Eases access to a BCEL LocalVariable object
 */
public class LVTHelper
{
	/**
	 * returns the local variable at an index int the scope of PC
	 *
	 * @param lvt the local variable table
	 * @param index the variable index
	 * @param pc the PC where the variable is used
	 */
	public static LocalVariable getLocalVariableAtPC(@NonNull LocalVariableTable lvt, int index, int pc) {
		int length = lvt.getTableLength();
		LocalVariable[] lvs = lvt.getLocalVariableTable();

		for(int i = 0; i < length; i++) {
			if (lvs[i].getIndex() == index) {
				int startPC = lvs[i].getStartPC();
				if ((pc >= startPC) && (pc < (startPC + lvs[i].getLength())))
					return lvs[i];
			}
		}

		return null;
	}
}
