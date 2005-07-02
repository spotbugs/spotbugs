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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Choose a random subset of warnings from a collection
 * and save them to a new collection.  Useful in sampling experiments.
 * 
 * @author David Hovemeyer
 */
public class RandomSubset {
	private static final long DEFAULT_SEED = -8609134467944889339L;

	private BugCollection bugCollection;
	private BugCollection resultCollection;
	private int numWarningsToPick;
	
	public RandomSubset(BugCollection bugCollection, int numWarningsToPick) {
		this.bugCollection = bugCollection;
		this.numWarningsToPick = numWarningsToPick;
		
		this.resultCollection = bugCollection.createEmptyCollectionWithMetadata();
	}
	
	public RandomSubset execute() {
		
		ArrayList<BugInstance> warningList = new ArrayList<BugInstance>(bugCollection.getCollection().size());
		warningList.addAll(bugCollection.getCollection());
		Collections.shuffle(warningList, new Random(DEFAULT_SEED));
		
		int count = 0;
		for (BugInstance warning : warningList) {
			if (count >= numWarningsToPick)
				break;
			resultCollection.add((BugInstance) warning.clone());
			count++;
		}
		
		return this;
	}
	
	/**
	 * @return Returns the resultCollection.
	 */
	public BugCollection getResultCollection() {
		return resultCollection;
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: " + RandomSubset.class.getName() + " <bug collection> <num warnings>");
			System.exit(1);
		}
		
		String fileName = args[0];
		int numWarningsToPick = Integer.parseInt(args[1]);
		
		BugCollection bugCollection = new SortedBugCollection();
		Project project = new Project();
		
		bugCollection.readXML(fileName, project);
		
		RandomSubset randomSubset = new RandomSubset(bugCollection, numWarningsToPick);
		randomSubset.execute();
		
		randomSubset.getResultCollection().writeXML(System.out, project);
	}
}
