/* 
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004 , Peter Friese
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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.Path;

/**
 * Factory for file collectors.
 * This factory produces the right kind of file collector depending
 * on the input parameters.
 * 
 * @author Peter Friese
 * @version 1.0
 * @since 25.09.2003
 */
public class FilesCollectorFactory {

	/** Controls debugging. */
	public  static boolean DEBUG = false;

	/**
	 * Produces a file collector for the given container.
	 * 
	 * @param container The container to produce a file collector for.
	 * @return A suitable file collector.
	 */
	public static AbstractFilesCollector getFilesCollector(IContainer container) {
		if (container != null) {
			return new ContainerFilesCollector(container);
		}
		else {
			return new NullFilesCollector();
		}
	}

	/**
	 * Produces a file collector for the given builder.
	 * 
	 * @param builder The builder to produce a file collector for.
	 * @return A suitable file collector.
	 */
	public static AbstractFilesCollector getFilesCollector(IncrementalProjectBuilder builder) {
		if (builder != null) {
			IProject project = builder.getProject();
			if (project != null) {
				IResourceDelta resourceDelta = builder.getDelta(project);
				if (resourceDelta != null) {
					if (resourceDelta.findMember(new Path(".project")) != null) {
						if (DEBUG) {
							System.out.println("Delta contains the PROJECT file.");
						}
						return new ContainerFilesCollector(project);
					}
					else {
						return new ResourceDeltaFilesCollector(resourceDelta);
					}
				}
				else {
					return new ContainerFilesCollector(project);
				}
			}
			else {
				if (DEBUG) {
					System.out.println("project is null");
				}
			}
		}
		return new NullFilesCollector();
	}

}
