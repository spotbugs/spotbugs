/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author pugh
 */
public class YourKitController {

	Object controller;

	Method advanceGeneration, captureMemorySnapshot;

	public YourKitController() {
		try {
			Class<?> c = Class.forName("com.yourkit.api.Controller");
			controller = c.newInstance();
			advanceGeneration = c.getMethod("advanceGeneration", String.class);
			captureMemorySnapshot = c.getMethod("captureMemorySnapshot");
		} catch (Exception e) {
			controller = null;
		}

	}

	public void advanceGeneration(String name) {
		if (controller == null)
			return;
		try {
			advanceGeneration.invoke(controller, name);
		} catch (Exception e) {
			assert true;
		}
	}

	public void captureMemorySnapshot() {
		if (controller == null)
			return;
		try {
			captureMemorySnapshot.invoke(controller);
		} catch (Exception e) {
			assert true;
		}
	}

}
