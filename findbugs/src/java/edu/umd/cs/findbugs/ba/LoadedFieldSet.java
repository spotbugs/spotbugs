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

package edu.umd.cs.findbugs.ba;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.generic.InstructionHandle;

/**
 * Object which stores which fields are loaded and stored
 * by the instructions in a method (including through inner-class
 * access methods), and also which fields are loaded/stored
 * by the overall method.  The main purpose is for doing
 * redundant load elimination and forward substitution
 * more efficiently, but it might be useful in other situations.
 *
 * @author David Hovemeyer
 */
public class LoadedFieldSet {
	private Set<XField> loadedFieldSet;
	private Map<InstructionHandle, XField> handleToFieldMap;
	private BitSet loadHandleSet;

	/**
	 * Constructor.
	 * Constructs an empty object.
	 */
	public LoadedFieldSet() {
		this.loadedFieldSet = new HashSet<XField>();
		this.handleToFieldMap = new HashMap<InstructionHandle, XField>();
		this.loadHandleSet = new BitSet();
	}

	/**
	 * Add a load of given field at given instruction.
	 *
	 * @param handle the instruction
	 * @param field  the field
	 */
	public void addLoad(InstructionHandle handle, XField field) {
		loadedFieldSet.add(field);
		handleToFieldMap.put(handle, field);
		loadHandleSet.set(handle.getPosition());
	}

	/**
	 * Add a store of given field at given instruction.
	 *
	 * @param handle the instruction
	 * @param field  the field
	 */
	public void addStore(InstructionHandle handle, XField field) {
		handleToFieldMap.put(handle, field);
	}

	/**
	 * Get the field loaded or stored at given instruction, if any.
	 *
	 * @param handle the instruction
	 * @return the field loaded or stored at the instruction, or null
	 *         if the instruction is not a load or store
	 */
	public XField getField(InstructionHandle handle) {
		return handleToFieldMap.get(handle);
	}

	/**
	 * Return whether or not the given field is loaded by any
	 * instruction in the method.
	 *
	 * @param field the field
	 * @return true if the field is loaded somewhere in the method,
	 *         false if it is never loaded
	 */
	public boolean isLoaded(XField field) {
		return loadedFieldSet.contains(field);
	}

	/**
	 * Return whether or not the given instruction is a load.
	 * 
	 * @param handle the instruction
	 * @return true if the instruction is a load, false if not
	 */
	public boolean instructionIsLoad(InstructionHandle handle) {
		return loadHandleSet.get(handle.getPosition());
	}
}

// vim:ts=4
