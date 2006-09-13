/*
 * FindBugs - Find Bugs in Java programs
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

package edu.umd.cs.findbugs.ba;

import java.util.HashMap;

public class AnnotationRetentionDatabase {
	private HashMap<String, Boolean> annotationRetention = new HashMap<String, Boolean>();

	public boolean hasClassfileRetention(String dottedClassName) {
		Boolean result = annotationRetention.get(dottedClassName);
		if (result == null) return false;
		return result;
	}

	/** return false if it has class retention *or* if the retention is unknown */
	public boolean lacksClassfileRetention(String dottedClassName) {
		Boolean result = annotationRetention.get(dottedClassName);
		if (result == null) return false;
		return !result;
	}

	public void setClassfileRetention(String dottedClassName, boolean value) {
		annotationRetention.put(dottedClassName, Boolean.valueOf(value));
	}

}
