/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004 University of Maryland
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.bcel.generic.ObjectType;

public class ExceptionSetFactory implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private HashMap<ObjectType, Integer> typeIndexMap;
	private ArrayList<ObjectType> typeList;

	public ExceptionSetFactory() {
		this.typeIndexMap = new HashMap<ObjectType, Integer>();
		this.typeList = new ArrayList<ObjectType>();
	}

	public ExceptionSet createExceptionSet() {
		return new ExceptionSet(this);
	}

	int getIndexOfType(ObjectType type) {
		Integer index = typeIndexMap.get(type);
		if (index == null) {
			index = getNumTypes();
			typeList.add(type);
			typeIndexMap.put(type, index);
		}
		return index.intValue();
	}

	ObjectType getType(int index) {
		return typeList.get(index);
	}

	int getNumTypes() {
		return typeList.size();
	}
}

// vim:ts=3
