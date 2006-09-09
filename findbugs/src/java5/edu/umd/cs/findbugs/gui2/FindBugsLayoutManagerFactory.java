/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.gui2;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author pugh
 */
public class FindBugsLayoutManagerFactory {
	Constructor<? extends FindBugsLayoutManager> constructor;
	public FindBugsLayoutManagerFactory(String classname) throws SecurityException, NoSuchMethodException, ClassNotFoundException {
		constructor = Class.forName(classname).asSubclass(FindBugsLayoutManager.class).getConstructor(MainFrame.class);
	}
	public FindBugsLayoutManager getInstance(MainFrame frame) {
		try {
			return constructor.newInstance(frame);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}

}
