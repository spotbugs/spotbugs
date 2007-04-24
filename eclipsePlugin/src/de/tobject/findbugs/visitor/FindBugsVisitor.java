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

package de.tobject.findbugs.visitor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * TODO Enter a comment for .
 * 
 * @author Peter Friese
 * @version 1.0
 * @since 28.07.2003
 */
public class FindBugsVisitor implements IResourceVisitor {

	/**
	 * Create a new visitor.
	 */
	public FindBugsVisitor() {
	}

	private boolean isJavaFile(IResource resource) {
		if ((resource.getType() == IResource.FILE)
			&& resource.getName().endsWith(".class")) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * @see IResourceVisitor#visit(IResource)
	 */
	public boolean visit(IResource resource) throws CoreException {
		if (isJavaFile(resource)) {
			System.out.println("Visiting " + resource.getName());
			//checkFile((IFile)resource);
		}
		// true to continue visiting children,
		// false to stop.
		return true;
	}

}
