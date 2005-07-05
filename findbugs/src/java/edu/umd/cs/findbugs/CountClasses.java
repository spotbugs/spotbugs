/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import edu.umd.cs.findbugs.model.ClassFeatureSet;
import edu.umd.cs.findbugs.model.SimilarClassFinder;

/**
 * @author David Hovemeyer
 */
public class CountClasses {
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: " + CountClasses.class.getName() + " <bug collection>");
			System.exit(1);
		}
		
		SortedBugCollection bugCollection = new SortedBugCollection();
		bugCollection.readXML(args[0], new Project());
		
		Set<String> classesSeen = new TreeSet<String>();
		SimilarClassFinder similarClasses = new SimilarClassFinder();
		
		for (Iterator<ClassFeatureSet> i = bugCollection.classFeatureSetIterator(); i.hasNext();) {
			ClassFeatureSet classFeatureSet = i.next();
			classesSeen.add(classFeatureSet.getClassName());
			similarClasses.add(classFeatureSet);
		}
		
		System.out.println(classesSeen.size() + " classes");
		System.out.println(similarClasses.size() + " equivalence classes");
	}
}
