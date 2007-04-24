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

package de.tobject.findbugs.nature;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;

/**
 * This is the nature for FindBugs-enabled projects.
 * 
 * @author Peter Friese
 * @version 1.0
 * @since 25.9.2003
 * @see IProjectNature
 */
public class FindBugsNature implements IProjectNature {

	/** Controls debugging of the nature */
	public static boolean DEBUG;

	/** project reference */
	private IProject project;

	/**
	 * Adds the FindBugs builder to the project.
	 * @see IProjectNature#configure
	 */
	public void configure() throws CoreException {
		if (DEBUG) {
			System.out.println("Adding findbugs to the project build spec.");
		}
		// register FindBugs builder
		addToBuildSpec(FindbugsPlugin.BUILDER_ID);
	}

	/**
	 * Removes the FindBugs builder from the project.
	 * @see IProjectNature#deconfigure
	 */
	public void deconfigure() throws CoreException {
		if (DEBUG) {
			System.out.println("Removing findbugs from the project build spec.");
		}
		//	de-register FindBugs builder
		removeFromBuildSpec(FindbugsPlugin.BUILDER_ID);
	}

	/**
	 * Removes the given builder from the build spec for the given project.
	 */
	protected void removeFromBuildSpec(String builderID) throws CoreException {
		MarkerUtil.removeMarkers(getProject());		
		IProjectDescription description = getProject().getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(builderID)) {
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
				description.setBuildSpec(newCommands);
				getProject().setDescription(description, null);
				return;
			}
		}
	}

	/**
	 * Adds a builder to the build spec for the given project.
	 */
	protected void addToBuildSpec(String builderID) throws CoreException {
		IProjectDescription description = getProject().getDescription();
		ICommand findBugsCommand = getFindBugsCommand(description);
		if (findBugsCommand == null) {
			// Add a Java command to the build spec
			ICommand newCommand = description.newCommand();
			newCommand.setBuilderName(builderID);
			setFindBugsCommand(description, newCommand);
		}
	}

	/**
	 * Find the specific FindBugs command amongst the build spec of a given description
	 */
	private ICommand getFindBugsCommand(IProjectDescription description)
		throws CoreException {
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(FindbugsPlugin.BUILDER_ID)) {
				return commands[i];
			}
		}
		return null;
	}

	/**
	 * Update the FindBugs  command in the build spec (replace existing one if present,
	 * add one first if none).
	 */
	private void setFindBugsCommand(
		IProjectDescription description,
		ICommand newCommand)
		throws CoreException {
		ICommand[] oldCommands = description.getBuildSpec();
		ICommand oldFindBugsCommand = getFindBugsCommand(description);
		ICommand[] newCommands;
		if (oldFindBugsCommand == null) {
			// Add the FindBugs build spec AFTER all other builders
			newCommands = new ICommand[oldCommands.length + 1];
			System.arraycopy(oldCommands, 0, newCommands, 0, oldCommands.length);
			newCommands[oldCommands.length] = newCommand;
		}
		else {
			for (int i = 0, max = oldCommands.length; i < max; i++) {
				if (oldCommands[i] == oldFindBugsCommand) {
					oldCommands[i] = newCommand;
					break;
				}
			}
			newCommands = oldCommands;
		}
		// Commit the spec change into the project
		description.setBuildSpec(newCommands);
		getProject().setDescription(description, null);
	}

	/**
	 * Returns the project reference.
	 * 
	 * @see IProjectNature#getProject
	 */
	public IProject getProject() {
		return this.project;
	}

	/**
	 * Sets the project reference.
	 * 
	 * @see IProjectNature#setProject
	 */
	public void setProject(IProject project) {
		this.project = project;
	}

}
