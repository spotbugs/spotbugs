/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.workflow;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class RecursiveSearchForJavaFiles {

	public static void main(String args[]) {
		for(File f : search(new File(args[0])))
			System.out.println(f.getPath());
	}
	public static Set<File> search(File root) {
		Set<File> result = new HashSet<File>();
		Set<File> directories = new HashSet<File>();
		LinkedList<File> worklist = new LinkedList<File>();
		directories.add(root);
		worklist.add(root);
		while (!worklist.isEmpty()) {
			File next = worklist.removeFirst();
			File[] files = next.listFiles();
			if (files != null) for (File f : files) {
				if (f.getName().endsWith(".java"))
					result.add(f);
				else if (f.isDirectory() && directories.add(f)) {
					worklist.add(f);
				}

			}

		}
		return result;

	}

}
