/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class DetectorFactory {
	private final Class detectorClass;
	private boolean enabled;

	public DetectorFactory(Class detectorClass, boolean enabled) {
		this.detectorClass = detectorClass;
		this.enabled = enabled;
	}

	private static final Class[] constructorArgTypes = new Class[]{BugReporter.class};

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Detector create(BugReporter bugReporter) {
		try {
			Constructor constructor = detectorClass.getConstructor(constructorArgTypes);
			return (Detector) constructor.newInstance(new Object[] {bugReporter});
		} catch (Exception e) {
			throw new RuntimeException("Could not instantiate Detector", e);
		}
	}

	public String getShortName() {
		String className = detectorClass.getName();
		int endOfPkg = className.lastIndexOf('.');
		if (endOfPkg >= 0)
			className = className.substring(endOfPkg + 1);
		return className;
	}
}

// vim:ts=4
