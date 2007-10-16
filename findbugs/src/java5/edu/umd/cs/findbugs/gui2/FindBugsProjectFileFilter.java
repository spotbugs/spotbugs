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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.io.File;

public final class FindBugsProjectFileFilter extends FindBugsFileFilter {

	public static final  FindBugsProjectFileFilter INSTANCE = new FindBugsProjectFileFilter();


	@Override
	public boolean accept(File arg0) {
		//return arg0.getName().endsWith(".fb") || arg0.isDirectory();
		if (arg0.isDirectory()) return true;
//		return (OriginalGUI2ProjectFile.fileContainingXMLData(arg0).exists());
		return false;
	}

	@Override
	public String getDescription() {
		return "FindBugs project directory";
	}

	@Override
	SaveType getSaveType() {
		return SaveType.PROJECT;
	}
}
