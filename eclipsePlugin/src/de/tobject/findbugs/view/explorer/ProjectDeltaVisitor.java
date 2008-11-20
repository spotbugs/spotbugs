/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
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
package de.tobject.findbugs.view.explorer;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;


/**
 * Resource change listener which reports structural changes on the top level of workspace
 * adding/removing/opening/closing of projects
 *
 * @author Andrei
 */
final class ProjectDeltaVisitor implements IResourceDeltaVisitor {

	private final Set<DeltaInfo> projects;

	public ProjectDeltaVisitor() {
		super();
		projects = new HashSet<DeltaInfo>();
	}

	public boolean visit(IResourceDelta delta) {
		if (delta == null) {
			return false;
		}
		IResource resource = delta.getResource();
		int kind = delta.getKind();
		int flags = delta.getFlags();
		if (kind == IResourceDelta.ADDED_PHANTOM
				|| kind == IResourceDelta.REMOVED_PHANTOM) {
			return false;
		}
		switch (resource.getType()) {
		case IResource.ROOT:
			return true;
		case IResource.FOLDER:
			// intended
		case IResource.FILE:
			// addProject(resource.getProject(), kind);
			return false;
		case IResource.PROJECT:
			boolean open = (flags & IResourceDelta.OPEN) != 0;
			boolean refreshNeeded = (kind == IResourceDelta.ADDED
					|| kind == IResourceDelta.REMOVED
					|| kind == IResourceDelta.COPIED_FROM || open);

			if (refreshNeeded) {
				addProject(resource.getProject(), kind);
			}
			return !refreshNeeded;
		}
		return false;
	}

	private void addProject(IResource project, int kind) {
		projects.add(new DeltaInfo(project, kind));
	}

	public Set<DeltaInfo> getProjectsDelta() {
		return projects;
	}

}
