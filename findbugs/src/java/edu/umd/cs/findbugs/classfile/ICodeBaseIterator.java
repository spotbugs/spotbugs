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
 * Iterator over the resources in an IScannableCodeBase.
 * Note that some of the methods can throw InterruptedException.
 * This occurs when the analysis is canceled by interrupting
 * the analysis thread.
 * 
 * <p>
 * Note that the close() method must be called when done with
 * an ICodeBaseIterator object.
 * </p>
 * 
 * @author David Hovemeyer
 */
public interface ICodeBaseIterator {
	/**
	 * Return true if there is another resource to be scanned,
	 * false otherwise.
	 * 
	 * @return true if there is another resource to be scanned,
	 *          false otherwise
	 */
	public boolean hasNext() throws InterruptedException;

	/**
	 * Get the ICodeBaseEntry representing the next resource in the code base.
	 * 
	 * @return the ICodeBaseEntry representing the next resource in the code base
	 * @throws InterruptedException
	 */
	public ICodeBaseEntry next() throws InterruptedException;
}
