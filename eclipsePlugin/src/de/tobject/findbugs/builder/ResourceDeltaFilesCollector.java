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
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;

import de.tobject.findbugs.util.Util;

/**
 * This file collector collects all files in a
 * {@link org.eclipse.core.resources.IResourceDelta}.
 *
 * @author Peter Friese
 * @version 1.0
 * @since 25.09.2003
 */
public class ResourceDeltaFilesCollector extends AbstractFilesCollector {

	private IResourceDelta resourceDelta;

	/**
	 * Creates a new resource delta file collector.
	 *
	 * @param resourceDelta The resource delta to scan for files.
	 */
	public ResourceDeltaFilesCollector(IResourceDelta resourceDelta) {
		this.resourceDelta = resourceDelta;
	}

	/* (non-Javadoc)
	 * @see de.tobject.findbugs.builder.AbstractFilesCollector#getFiles()
	 */
	@Override
	public Collection<IFile> getFiles() {
		return collectFiles(resourceDelta);
	}

	/**
	 * Returns a list of all files in a resource delta. This is of help when
	 * performing an incremental build.
	 *
	 * @see #getFiles()
	 * @see #getFiles(IContainer)
	 * @return Collection A list of all files to be built.
	 */
	private Collection<IFile> collectFiles(IResourceDelta delta) {
		List<IFile> files = new ArrayList<IFile>();
		List<IResourceDelta> folders = new ArrayList<IResourceDelta>();
		IResourceDelta affectedChildren[] = delta.getAffectedChildren();
		for (int i = 0; i < affectedChildren.length; i++) {
			IResourceDelta childDelta = affectedChildren[i];
			IResource child = childDelta.getResource();
			int childType = child.getType();
			if (childType == IResource.FILE) {
				if (DEBUG) {
					System.out.println(
						"Delta file: " + child.getFullPath().toOSString());
				}
				int deltaKind = childDelta.getKind();
				if ((deltaKind == IResourceDelta.ADDED
					|| deltaKind == IResourceDelta.CHANGED)) {
					if (Util.isJavaArtifact(child)) {
						files.add((IFile) child);
					}
				}
			}
			else {
				if (childType == IResource.FOLDER) {
					folders.add(childDelta);
				}
			}
		}

		for (IResourceDelta resourceDelta2 : folders) {
			files.addAll(collectFiles(resourceDelta2));
		}

		return files;
	}

}
