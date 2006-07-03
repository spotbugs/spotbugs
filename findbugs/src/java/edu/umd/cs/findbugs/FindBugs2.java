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

package edu.umd.cs.findbugs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;

/**
 * FindBugs driver class.
 * Experimental version to use the new bytecode-framework-neutral
 * codebase/classpath/classfile infrastructure.
 * Don't expect this class to do anything useful for a while.
 * 
 * @author David Hovemeyer
 */
public class FindBugs2 {
	private BugReporter bugReporter;
	private Project project;
	private IClassPath classPath;
	
	public FindBugs2(BugReporter bugReporter, Project project) {
		this.bugReporter = bugReporter;
		this.project = project;
	}
	
	public void execute() throws IOException, InterruptedException {
		
		// The class path object
		// FIXME: this should be in the analysis context eventually
		classPath = ClassFactory.instance().createClassPath();

		try {
			buildClassPath();
		} finally {
			classPath.close();
		}
	}

	/**
	 * @throws IOException
	 */
	private void buildClassPath() throws IOException {
		// Worklist of application code bases to be registered
		// and scanned for:
		//   - nested archives
		//   - Class-Path references in Jar manifests (which will be
		//     added to the aux classpath)
		LinkedList<String> appWorkList = new LinkedList<String>();

		// List of auxiliary classpath entries
		LinkedList<String> auxList = new LinkedList<String>();
		
		// Create application codebases.
		appWorkList.addAll(Arrays.asList(project.getFileArray()));
		while (!appWorkList.isEmpty()) {
			String url = appWorkList.removeFirst();
			IScannableCodeBase appCodeBase = ClassFactory.instance().createLocalCodeBase(url);
			classPath.addCodeBase(appCodeBase);
			
			// Any nested archives?
			
			// Any Class-Path references?
		}
		
		// Create auxiliary codebases.
		// TODO
	}
}
