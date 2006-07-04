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
import java.io.InputStream;
import java.util.LinkedList;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.io.IO;
import edu.umd.cs.findbugs.util.Archive;

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
			
			// TODO: the execution plan, analysis, etc.
		} finally {
			// Make sure the codebases on the classpath are closed
			classPath.close();
		}
	}
	
	interface WorkListItem {
		ICodeBase getCodeBase() throws IOException;
	}
	
	static class TopLevelAppWorkListItem implements WorkListItem {
		String fileName;
		boolean isAppCodeBase;
		
		public TopLevelAppWorkListItem(String fileName, boolean isAppCodeBase) {
			this.fileName = fileName;
			this.isAppCodeBase = isAppCodeBase;
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.FindBugs2.AppCodeBaseWorkListItem#getCodeBase()
		 */
		public ICodeBase getCodeBase() throws IOException {
			IScannableCodeBase codeBase = ClassFactory.instance().createLocalCodeBase(fileName);
			codeBase.setApplicationCodeBase(isAppCodeBase);
			return codeBase;
		}
	}
	
	static class NestedArchiveWorkListItem implements WorkListItem {
		IScannableCodeBase parentCodeBase;
		String resourceName;
		
		public NestedArchiveWorkListItem(IScannableCodeBase parentCodeBase, String resourceName) {
			this.parentCodeBase = parentCodeBase;
			this.resourceName = resourceName;
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.FindBugs2.AppCodeBaseWorkListItem#getCodeBase()
		 */
		public ICodeBase getCodeBase() throws IOException {
			try {
				ICodeBase codeBase =
					ClassFactory.instance().createNestedArchiveCodeBase(parentCodeBase, resourceName);

				// The nested codebase is an application codebase IFF
				// the parent codebase is.
				if (parentCodeBase.isApplicationCodeBase()) {
					codeBase.setApplicationCodeBase(true);
				}
				
				return codeBase;
			} catch (ResourceNotFoundException e) {
				// This should not happen
				throw new IOException();
			}
		}
	}

	/**
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void buildClassPath() throws InterruptedException, IOException {
		// Seed worklist with app codebases and aux codebases.
		LinkedList<WorkListItem> workList = new LinkedList<WorkListItem>();
		for (String path : project.getFileArray()) {
			workList.add(new TopLevelAppWorkListItem(path, true));
		}
		for (String path : project.getAuxClasspathEntryList()) {
			workList.add(new TopLevelAppWorkListItem(path, false));
		}
		
		while (!workList.isEmpty()) {
			WorkListItem item = workList.removeFirst();
			
			ICodeBase codeBase = item.getCodeBase();
			classPath.addCodeBase(codeBase);
			
			// If it is a scannable codebase, check it for nested archives.
			if (codeBase instanceof IScannableCodeBase) {
				checkForNestedArchives(workList, (IScannableCodeBase) codeBase);
			}
			
			// Check for a Jar manifest for additional aux classpath entries.
			scanForClasspathReferences(workList, codeBase);
		}
	}

	/**
	 * Check a codebase for nested archives.
	 * Add any found to the worklist.
	 * 
	 * @param workList the worklist
	 * @param codeBase the codebase to scan
	 * @throws InterruptedException
	 */
	private void checkForNestedArchives(LinkedList<WorkListItem> workList, IScannableCodeBase codeBase)
			throws InterruptedException {
		ICodeBaseIterator i = codeBase.iterator();
		while (i.hasNext()) {
			ICodeBaseEntry entry = i.next();
			if (Archive.isArchiveFileName(entry.getResourceName())) {
				workList.add(new NestedArchiveWorkListItem(codeBase, entry.getResourceName()));
			}
		}
	}

	/**
	 * Check a codebase for a Jar manifest to examine for Class-Path entries.
	 * 
	 * @param workList the worklist
	 * @param codeBase the codebase for examine for a Jar manifest
	 * @throws IOException 
	 */
	private void scanForClasspathReferences(LinkedList<WorkListItem> workList, ICodeBase codeBase)
			throws IOException {
		try {
			ICodeBaseEntry manifestEntry = codeBase.lookupResource("META-INF/MANIFEST.MF");

			InputStream in = null;
			try {
				in = manifestEntry.openResource();
				Manifest manifest = new Manifest(in);

				Attributes mainAttrs = manifest.getMainAttributes();
				String classPath = mainAttrs.getValue("Class-Path");
				if (classPath != null) {
					String[] pathList = classPath.split("\\s+");

					for (String path : pathList) {
						// Referenced path is relative to this codebase.
						// FIXME: how to find it?
					}
				}
			} finally {
				if (in != null) {
					IO.close(in);
				}
			}
		} catch (ResourceNotFoundException e) {
			// Do nothing - no Jar manifest found
		}
	}
}
