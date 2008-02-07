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

package edu.umd.cs.findbugs.classfile;


/**
 * Interface for a basic code base in which we can look up resources
 * but not necessarily scan for the list of all resources.
 *
 * @author David Hovemeyer
 */
public interface ICodeBase {
	/** Codebase was explicitly specified. */
	public static final int SPECIFIED = 0;

	/** Codebase was discovered as a nested archive in another codebase. */
	public static final int NESTED = 1;

	/** Codebase was referenced in the Class-Path attribute of a Jar manifest of another codebase. */
	public static final int IN_JAR_MANIFEST = 2;

	/** Codebase was discovered in the system classpath. */
	public static final int IN_SYSTEM_CLASSPATH = 3;

	/**
	 * Get the codebase locator describing the location of this codebase.
	 *
	 * @return the ICodeBaseLocator
	 */
	public ICodeBaseLocator getCodeBaseLocator();

	/**
	 * Look up a resource in this code base.
	 *
	 * @param resourceName name of the resource to look up
	 * @return ICodeBaseEntry representing the resource or null if the resource cannot be
	 * found in this code base
	 */
	public ICodeBaseEntry lookupResource(String resourceName);

	/**
	 * Designate this code base as an application codebase.
	 *
	 * @param isAppCodeBase true if this is an application codebase, false if not
	 */
	public void setApplicationCodeBase(boolean isAppCodeBase);

	/**
	 * Return whether or not this codebase is an application codebase.
	 *
	 * @return true if this is an application codebase, false if not
	 */
	public boolean isApplicationCodeBase();

	/**
	 * Set how this codebase was discovered.
	 *
	 * @param howDiscovered one of the constants SPECIFIED, NESTED,
	 *                       IN_JAR_MANIFEST, or IN_SYSTEM_CLASSPATH
	 */
	public void setHowDiscovered(int howDiscovered);

	/**
	 * Return how this codebase was discovered.
	 *
	 * @return one of the constants SPECIFIED, NESTED, IN_JAR_MANIFEST, or IN_SYSTEM_CLASSPATH
	 */
	public int getHowDiscovered();

	/**
	 * Return whether or not this code base contains any source files.
	 *
	 * @return true if the code base contains source file(s),
	 *          false if it does not contain source files
	 */
	public boolean containsSourceFiles() throws InterruptedException;

	/**
	 * Get the filesystem pathname of this codebase.
	 *
	 * @return the filesystem pathname of this codebase,
	 *          or null if this codebase is not accessible via the filesystem
	 */
	public String getPathName();

	/**
	 * Set timestamp indicating the most recent time when any of the files
	 * in the codebase were modified.
	 *
	 * @param lastModifiedTime timestamp when any codebase files were most-recently modified
	 */
	public void setLastModifiedTime(long lastModifiedTime);

	/**
	 * Get timestamp indicating the most recent time when any of the files
	 * in the codebase were modified.
	 * This information is only likely to be accurate if an
	 * ICodeBaseIterator has been used to scan the resources
	 * in the codebase (scannable codebases only, obviously).
	 *
	 * @return timestamp when any codebase files were most-recently modified,
	 *          -1 if unknown
	 */
	public long getLastModifiedTime();

	/**
	 * This method should be called when done using the code base.
	 */
	public void close();
}
