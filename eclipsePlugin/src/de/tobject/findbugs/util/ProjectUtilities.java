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

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * Project utility class.
 *
 * @author Peter Friese
 * @version 1.0
 * @since 25.07.2003
 */
public class ProjectUtilities {

	private static boolean DEBUG;

	/**
	 * Adds a FindBugs nature to a project.
	 *
	 * @param project The project the nature will be applied to.
	 * @param monitor A progress monitor. Must not be null.
	 * @throws CoreException
	 */
	public static void addFindBugsNature(IProject project, IProgressMonitor monitor) throws CoreException {
		if (!project.hasNature(FindbugsPlugin.NATURE_ID)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length] = FindbugsPlugin.NATURE_ID;
			if(DEBUG) {
				for (int i = 0; i < newNatures.length; i++) {
					System.out.println(newNatures[i]);
				}
			}
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
		}
		else {
			monitor.worked(1);
		}
	}

	/**
	 * Removes the FindBugs nature from a project.
	 *
	 * @param project The project the nature will be removed from.
	 * @param monitor A progress monitor. Must not be null.
	 * @throws CoreException
	 */
	public static void removeFindBugsNature(IProject project, IProgressMonitor monitor) throws CoreException {
		if (project.hasNature(FindbugsPlugin.NATURE_ID)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			ArrayList<String> newNaturesList = new ArrayList<String>();
			for (int i = 0; i < prevNatures.length; i++) {
				if (!prevNatures[i].equals(FindbugsPlugin.NATURE_ID)) {
					newNaturesList.add(prevNatures[i]);
				}
			}
			String[] newNatures = newNaturesList.toArray(new String[newNaturesList.size()]);
			description.setNatureIds(newNatures);
			project.setDescription(description, monitor);
		}
		else {
			monitor.worked(1);
		}
	}

}
