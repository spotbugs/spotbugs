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

package edu.umd.cs.findbugs.classfile.impl;

import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;

/**
 * @author David Hovemeyer
 */
public abstract class AbstractScannableCodeBaseEntry implements ICodeBaseEntry {
	public abstract AbstractScannableCodeBase getCodeBase();
	
	public abstract String getRealResourceName();
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#overrideResourceName(java.lang.String)
	 */
	public void overrideResourceName(String resourceName) {
		getCodeBase().addResourceNameTranslation(getRealResourceName(), resourceName);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#getResourceName()
	 */
	public String getResourceName() {
		return getCodeBase().translateResourceName(getRealResourceName());
	}
}
