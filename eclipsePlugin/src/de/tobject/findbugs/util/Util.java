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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;

import de.tobject.findbugs.FindbugsPlugin;

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
		if (resource == null || (resource.getType() != IResource.FILE)) {
			return false;
		}
		String ex = resource.getFileExtension();
		return "java".equalsIgnoreCase(ex); //$NON-NLS-1$
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
		if (resource == null || (resource.getType() != IResource.FILE)) {
			return false;
		}
		String ex = resource.getFileExtension();
		return "class".equalsIgnoreCase(ex); //$NON-NLS-1$

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
		if (resource == null || (resource.getType() != IResource.FILE)) {
			return false;
		}
		String ex = resource.getFileExtension();
		return ("java".equalsIgnoreCase(ex)
				|| "class".equalsIgnoreCase(ex));
	}

	public static boolean isJavaProject(IProject project) {
		try {
			return project != null && project.isOpen()
					&& project.hasNature(JavaCore.NATURE_ID);
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "couldn't determine project nature");
			return false;
		}
	}
}
