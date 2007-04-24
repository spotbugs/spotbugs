/* 
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package de.tobject.findbugs.builder;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;

/**
 * Abstract base class for file collectors.
 *  
 * @author Peter Friese
 * @version 1.0
 * @since 25.09.2003
 */
public abstract class AbstractFilesCollector {

	/** Controls debugging. */
	public static boolean DEBUG;

	/**
	 * Retrieves a list of files to process.
	 * 
	 * @return A collection of files to process.
	 * @throws CoreException If some error occurred.
	 */
	public abstract Collection getFiles() throws CoreException;



}
