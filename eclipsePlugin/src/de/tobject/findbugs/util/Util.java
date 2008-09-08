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

import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map.Entry;

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

	/**
	 * A countdown timer which starts to work with the first entry and prints the results
	 * ascending with the overall time.
	 */
	public static class StopTimer {
		TreeMap<Long, String> stopTimes = new TreeMap<Long, String>();

		public synchronized void newPoint(String name) {
			Long time = Long.valueOf(System.currentTimeMillis());
			if(stopTimes.size() == 0){
				stopTimes.put(time, name);
				return;
			}
			Long lastTime = stopTimes.lastKey();
			if(time.longValue() <= lastTime.longValue()){
				time = Long.valueOf(lastTime.longValue() + 1);
			}
			stopTimes.put(time, name);
		}

		public synchronized String getResults() {
			StringBuilder sb = new StringBuilder();
			Iterator<Entry<Long, String>> iterator = stopTimes.entrySet().iterator();
			Entry<Long, String> firstEntry = iterator.next();
			while(iterator.hasNext()){
				Entry<Long, String> entry = iterator.next();
				long diff = entry.getKey().longValue() - firstEntry.getKey().longValue();
				sb.append(firstEntry.getValue()).append(": ").append(diff).append(" ms\n");
				firstEntry = entry;
			}

			long overall = stopTimes.lastKey().longValue()
					- stopTimes.firstKey().longValue();
			sb.append("Overall: ").append(overall).append(" ms");
			return sb.toString();
		}
	}
}
