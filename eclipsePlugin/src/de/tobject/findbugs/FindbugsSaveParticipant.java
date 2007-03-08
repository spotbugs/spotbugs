/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2005, University of Maryland
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
package de.tobject.findbugs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

/**
 * Callback object responsible for saving the uncomitted state
 * of any FindBugs-enabled projects.
 * 
 * @author David Hovemeyer
 */
public class FindbugsSaveParticipant implements ISaveParticipant {

	public void doneSaving(ISaveContext context) {
//		System.out.println("done saving!");
	}

	public void prepareToSave(ISaveContext context) throws CoreException {
//		System.out.println("preparing!");
	}

	public void rollback(ISaveContext context) {
//		System.out.println("rollback!");
	}

	public void saving(ISaveContext context) throws CoreException {
//		System.out.println("saving, kind == " + context.getKind());
		
		switch (context.getKind()) {
		case ISaveContext.FULL_SAVE:
			fullSave();
			break;
		case ISaveContext.PROJECT_SAVE:
			saveBugCollection(context.getProject());
			break;
		default:
			break;
		}
	}

	private void fullSave() {
		IProject[] projectList = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for(IProject project : projectList) 
            if(project.isAccessible() && FindbugsPlugin.isJavaProject(project)) {
			saveBugCollection(project);
		}
	}

	private void saveBugCollection(IProject project) {
		if (project.isAccessible())
		try {
//			System.out.println("Saving project " + project.getName());
			FindbugsPlugin.saveCurrentBugCollection(project, null);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			FindbugsPlugin.getDefault().logException(
					e, "Could not save bug collection for project " + project.getName());
		}
	}

}
