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

package de.tobject.findbugs.util;

import org.eclipse.core.resources.IResource;

/**
 * Eclipse-specific utilities.
 * 
 * @author Phil Crosby
 * @author Peter Friese
 */
public class Util{
	/**
	 * Checks whether the given resource is a Java source file.
	 * 
     * @param resource The resource to check.
	 * @return
	 *      <code>true</code> if the given resource is a Java source file,
	 *      <code>false</code> otherwise.
     */
	public static boolean isJavaFile(IResource resource) {
		if (resource == null)
			return false;
        String ex = resource.getFileExtension();
		return ex.equalsIgnoreCase("java"); //$NON-NLS-1$
	}

	/**
	 * Checks whether the given resource is a Java class file.
	 * 
	 * @param resource The resource to check.
	 * @return
	 * 	<code>true</code> if the given resource is a class file,
	 * 	<code>false</code> otherwise.
	 */
	public static boolean isClassFile(IResource resource) {
		if (resource == null)
			return false;
		String ex = resource.getFileExtension();
		return ex.equalsIgnoreCase("class"); //$NON-NLS-1$

	}
	/**
	 * Checks whether the given resource is a Java artifact (i.e. either a
	 * Java source file or a Java class file).
	 * 
	 * @param resource The resource to check.
	 * @return
	 * 	<code>true</code> if the given resource is a Java artifact.
	 * 	<code>false</code> otherwise.
	 */
	public static boolean isJavaArtifact(IResource resource) {
		if (resource == null)
			return false;		
		String ex = resource.getFileExtension();
		if (ex == null)
			return false;
		return (ex.equalsIgnoreCase("java")
				|| ex.equalsIgnoreCase("class"));
	}

	/**
	 * Changes the file extension in a string to the desired extension.
	 * @param name the name of a file
	 * @param extension the new extension of the file
	 * @return a string with the old extension changed to the new extension
	 */
	public static String changeExtension(String name, String extension)
	{
		int i = name.lastIndexOf(".");
		return i<0 ? name : name.substring(0,i+1) + extension;
	}
}
