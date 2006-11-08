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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import de.tobject.findbugs.util.Util;

/**
 * This file collector collects all files in a 
 * {@link org.eclipse.core.resources.IContainer}, e.g. a project.
 * 
 * @author Peter Friese
 * @version 1.0
 * @since 25.09.2003
 */
public class ContainerFilesCollector extends AbstractFilesCollector {
	
	/** The container we will be working on */
	private IContainer container;
	
	/**
	 * Creates a new {@link ContainerFilesCollector}.
	 * 
	 * @param container The container to process.
	 */
	public ContainerFilesCollector(IContainer container) {
		super();
		this.container = container;
	}
	
	/* (non-Javadoc)
	 * @see de.tobject.findbugs.builder.AbstractFilesCollector#getFiles()
	 */
	public Collection getFiles() throws CoreException {
		return collectFiles(this.container);
	}
	
	/**
	 * Returns a list of all files in a container. This is of help when performing 
	 * a full or automatic build.
	 * 
	 * @see #getFiles()
	 * @see #getFiles(IResourceDelta)
	 * @return Collection A list of all files to be built.
	 */
	private Collection<IResource> collectFiles(IContainer container) throws CoreException {
		ArrayList<IResource> files = new ArrayList<IResource>(0);
		ArrayList<IResource> folders = new ArrayList<IResource>(0);
		IResource children[] = container.members();
		for (int i = 0; i < children.length; i++) {
			IResource child = children[i];
			int childType = child.getType();
			if (childType == IResource.FILE) {
				if (DEBUG) {
					System.out.println(
						"Project file: " + child.getFullPath().toOSString());
				}
				if (Util.isJavaArtifact(child)) {
					files.add(child);
				}
			}
			else if (childType == IResource.FOLDER) {
				folders.add(child);
			}
		}
		
		for (Iterator iter = folders.iterator();
			iter.hasNext();
			)
			files.addAll(collectFiles((IContainer) iter.next()));
			
		return files;
	}
	
}
