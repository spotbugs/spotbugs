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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.generic.InstructionHandle;

public class LoadedFieldSet {
	private Set<XField> loadedFieldSet;
	private Map<InstructionHandle, XField> handleToFieldMap;

	public LoadedFieldSet() {
		this.loadedFieldSet = new HashSet<XField>();
		this.handleToFieldMap = new HashMap<InstructionHandle, XField>();
	}

	public void addLoad(InstructionHandle handle, XField field) {
		loadedFieldSet.add(field);
		handleToFieldMap.put(handle, field);
	}

	public void addStore(InstructionHandle handle, XField field) {
		handleToFieldMap.put(handle, field);
	}

	public XField getField(InstructionHandle handle) {
		return handleToFieldMap.get(handle);
	}

	public boolean isLoaded(XField field) {
		return loadedFieldSet.contains(field);
	}
}

// vim:ts=4
