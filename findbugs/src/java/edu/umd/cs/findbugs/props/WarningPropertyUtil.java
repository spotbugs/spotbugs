/*
 * FindBugs - Find bugs in Java programs
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
package edu.umd.cs.findbugs.props;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.TypeDataflow;
import edu.umd.cs.findbugs.ba.TypeFrame;

/**
 * Utility methods for creating general warning properties.
 * 
 * @author David Hovemeyer
 */
public abstract class WarningPropertyUtil {
	/**
	 * Add a RECEIVER_OBJECT_TYPE warning property for a particular
	 * location in a method to given warning property set.
	 * 
	 * @param propertySet  the property set
	 * @param classContext ClassContext of the class containing the method 
	 * @param method       the method
	 * @param location     Location within the method
	 * @throws DataflowAnalysisException
	 * @throws CFGBuilderException
	 */
	public static void addReceiverObjectType(
			WarningPropertySet propertySet,
			ClassContext classContext,
			Method method,
			Location location) throws DataflowAnalysisException, CFGBuilderException {
		TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
		TypeFrame frame = typeDataflow.getFactAtLocation(location);
		if (frame.isValid()) {
			Type type = frame.getInstance(
					location.getHandle().getInstruction(), classContext.getConstantPoolGen());
			if (type instanceof ReferenceType) {
				propertySet.setProperty(GeneralWarningProperty.RECEIVER_OBJECT_TYPE, type.toString());
			}
		}
	}
	
	/**
	 * Add a RECEIVER_OBJECT_TYPE warning property for a particular
	 * instruction in a method to given warning property set.
	 * 
	 * @param propertySet  the property set
	 * @param classContext ClassContext of the class containing the method 
	 * @param method       the method
	 * @param pc           PC value of the instruction
	 * @throws DataflowAnalysisException
	 * @throws CFGBuilderException
	 */
	public static void addReceiverObjectType(
			WarningPropertySet propertySet,
			ClassContext classContext,
			Method method,
			int pc) throws CFGBuilderException, DataflowAnalysisException {
		CFG cfg = classContext.getCFG(method);
		
		// Get all Locations for the given PC value.
		// There may be more than one because of JSR subroutines.
		List<Location> locationList = new LinkedList<Location>();
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();
			if (location.getHandle().getPosition() == pc) {
				locationList.add(location);
			}
		}
		
		TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
		
		// Get all types for those locations
		List<Type> typeList = new LinkedList<Type>();
		for (Iterator<Location> i = locationList.iterator(); i.hasNext();) {
			Location location = i.next();
			TypeFrame frame = typeDataflow.getFactAtLocation(location);
			if (frame.isValid()) {
				Type type = frame.getInstance(
						location.getHandle().getInstruction(), classContext.getConstantPoolGen());
				typeList.add(type);
			}
		}
		
		if (!typeList.isEmpty()) {
			// Find least upper bound of collected types
			ReferenceType lub = null;
			for (Iterator<Type> i = typeList.iterator(); i.hasNext();) {
				Type type = i.next();
				if (!(type instanceof ReferenceType))
					return;
				ReferenceType refType = (ReferenceType) type;
				if (lub == null) {
					lub = refType;
				} else {
					try {
						lub = lub.getFirstCommonSuperclass(refType);
					} catch (ClassNotFoundException e) {
						return;
					}
				}
			}
			
			propertySet.setProperty(GeneralWarningProperty.RECEIVER_OBJECT_TYPE, lub.toString());
		}
	}
}
