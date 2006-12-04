/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import edu.umd.cs.findbugs.classfile.IClassPathBuilderProgress;

/**
 * A callback that may be installed in a FindBugs instance
 * to asynchronously keep track of its progress.
 *
 * @author David Hovemeyer
 * @see FindBugs
 */
public interface FindBugsProgress extends IClassPathBuilderProgress {
	/**
	 * Report the total number of archives (Jar or zip files) that will be analyzed.
	 *
	 * @param numArchives the number of archives
	 */
	public void reportNumberOfArchives(int numArchives);

	/**
	 * Report that FindBugs has finished scanning an archive in order
	 * to add its classes to the repository.
	 */
	public void finishArchive();

	/**
	 * Provide an array that predicts the number of classes in each pass
	 * @param classesPerPass
	 */
	public void predictPassCount(int [] classesPerPass);
	/**
	 * Report that FindBugs has finished scanning the archives and will
	 * start analysing the classes contained therein.
	 *
	 * @param numClasses number of classes found in all of the archives
	 */
	public void startAnalysis(int numClasses);

	/**
	 * Report that FindBugs has finished analyzing a class.
	 */
	public void finishClass();

	/**
	 * Called to indicate that the per-class analysis is finished, and
	 * that the whole program analysis is taking place.
	 */
	public void finishPerClassAnalysis();
}

// vim:ts=4
