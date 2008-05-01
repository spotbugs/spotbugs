/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba.vna;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XField;

/**
 * Helper methods to find out information about the
 * source of the value represented by a given ValueNumber.
 * 
 * @author Bill Pugh
 * @author David Hovemeyer
 */
public abstract class ValueNumberSourceInfo {

	/**
	 * @param method
	 *            TODO
	 * @param location
	 * @param valueNumber
	 * @param vnaFrame
	 * @return the annotation
	 */
	public static BugAnnotation findAnnotationFromValueNumber(Method method,
			Location location, ValueNumber valueNumber,
			ValueNumberFrame vnaFrame) {
		LocalVariableAnnotation ann = ValueNumberSourceInfo.findLocalAnnotationFromValueNumber(
				method, location, valueNumber, vnaFrame);
		if (ann != null && ann.isSignificant())
			return ann;
		FieldAnnotation field = ValueNumberSourceInfo.findFieldAnnotationFromValueNumber(method,
				location, valueNumber, vnaFrame);
		if (field != null)
			return field;
		return ann;
	}

	public static LocalVariableAnnotation findLocalAnnotationFromValueNumber(
			Method method, Location location, ValueNumber valueNumber,
			ValueNumberFrame vnaFrame) {

		if (vnaFrame == null || vnaFrame.isBottom() || vnaFrame.isTop())
			return null;

		LocalVariableAnnotation localAnnotation = null;
		for (int i = 0; i < vnaFrame.getNumLocals(); i++) {
			if (valueNumber.equals(vnaFrame.getValue(i))) {
				InstructionHandle handle = location.getHandle();
				int position1 = handle.getPrev().getPosition();
				int position2 = handle.getPosition();
				localAnnotation = LocalVariableAnnotation
				.getLocalVariableAnnotation(method, i, position1,
						position2);
				if (localAnnotation != null)
					return localAnnotation;
			}
		}
		return null;
	}

	public static FieldAnnotation findFieldAnnotationFromValueNumber(
			Method method, Location location, ValueNumber valueNumber,
			ValueNumberFrame vnaFrame) {
		XField field = ValueNumberSourceInfo.findXFieldFromValueNumber(method, location, valueNumber,
				vnaFrame);
		if (field == null)
			return null;
		return FieldAnnotation.fromXField(field);
	}

	public static XField findXFieldFromValueNumber(Method method,
			Location location, ValueNumber valueNumber,
			ValueNumberFrame vnaFrame) {
		if (vnaFrame == null || vnaFrame.isBottom() || vnaFrame.isTop())
			return null;

		AvailableLoad load = vnaFrame.getLoad(valueNumber);
		if (load != null) {
			return load.getField();
		}
		return null;
	}

}
