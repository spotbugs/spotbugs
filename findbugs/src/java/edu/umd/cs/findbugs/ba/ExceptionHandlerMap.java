/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.daveho.ba;

import java.util.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * This class provides a convenient way of determining the exception handlers
 * for instructions in a method.  Essentially, it's a
 * a map of InstructionHandles to lists of CodeExceptionGen objects.
 * 
 * @author David Hovemeyer
 */
public class ExceptionHandlerMap {
	private IdentityHashMap<InstructionHandle, List<CodeExceptionGen>> handlerMap;

	/**
	 * Constructor.
	 * @param methodGen the method to build the map for
	 */
	public ExceptionHandlerMap(MethodGen methodGen) {
		handlerMap = new IdentityHashMap<InstructionHandle, List<CodeExceptionGen>>();
		build(methodGen);
	}

	/**
	 * Get the list of exception handlers (CodeExceptionGen objects)
	 * which are specified to handle exceptions for the instruction whose
	 * handle is given.
	 * @param handle the handle of the instruction we want the exception handlers for
	 * @return the list of exception handlers, or null if there are no handlers
	 *   registered for the instruction
	 */
	public List<CodeExceptionGen> getHandlerList(InstructionHandle handle) {
		return handlerMap.get(handle);
	}

	private void build(MethodGen methodGen) {
		CodeExceptionGen[] handlerList = methodGen.getExceptionHandlers();
		for (int i = 0; i < handlerList.length; ++i) {
			CodeExceptionGen exceptionHandler = handlerList[i];

			InstructionHandle handle;
			InstructionHandle next = exceptionHandler.getStartPC();
			InstructionHandle end = exceptionHandler.getEndPC();

			do {
				handle = next;
				addHandler(handle, exceptionHandler);
				next = handle.getNext();
			} while (handle != end);
		}
	}

	private void addHandler(InstructionHandle handle, CodeExceptionGen exceptionHandler) {
		List<CodeExceptionGen> handlerList = handlerMap.get(handle);
		if (handlerList == null) {
			handlerList = new LinkedList<CodeExceptionGen>();
			handlerMap.put(handle, handlerList);
		}
		handlerList.add(exceptionHandler);
	}
}

// vim:ts=4
