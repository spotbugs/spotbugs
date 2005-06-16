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

import java.util.Comparator;

import edu.umd.cs.findbugs.ba.ClassHash;
import edu.umd.cs.findbugs.ba.MethodHash;

/**
 * @author David Hovemeyer
 */
public class FuzzyBugComparator implements Comparator<BugInstance> {
	private SortedBugCollection bugCollection;
	
	public FuzzyBugComparator(SortedBugCollection bugCollection) {
		this.bugCollection = bugCollection;
	}
	
	public int compare(BugInstance a, BugInstance b) {
		
		
		// TODO
		return 0;
	}
	
	// Compare classes: either exact fully qualified name must match, or class hash must match
	private boolean sameClass(ClassAnnotation aClass, ClassAnnotation bClass) {
		if (aClass == null || bClass == null) {
			return aClass == null && bClass == null;
		}
		
		if (aClass.getClassName().equals(bClass.getClassName()))
			return true;
		
		// Get class hashes
		ClassHash aHash = bugCollection.getClassHash(aClass.getClassName());
		ClassHash bHash = bugCollection.getClassHash(bClass.getClassName());
		if (aHash == null || bHash == null)
			return false;
		
		return aHash.isSameHash(bHash);
	}
	
	// Compare methods: either exact name and signature must match, or method hash must match
	private boolean sameMethod(MethodAnnotation aMethod, MethodAnnotation bMethod) {
		if (aMethod == null || bMethod == null) {
			return aMethod == null && bMethod == null;
		}
		
		if (aMethod.getMethodName().equals(bMethod.getMethodName()) &&
				aMethod.getMethodSignature().equals(bMethod.getMethodSignature()))
			return true;
		
		// Get class hashes for primary classes
		ClassHash aClassHash = bugCollection.getClassHash(aMethod.getClassName());
		ClassHash bClassHash = bugCollection.getClassHash(bMethod.getClassName());
		if (aClassHash == null || bClassHash == null)
			return false;
		
		// Look up method hashes
		MethodHash aHash = aClassHash.getMethodHash(aMethod.toXMethod());
		MethodHash bHash = bClassHash.getMethodHash(bMethod.toXMethod());
		if (aHash == null || bHash == null)
			return false;
		
		return aHash.isSameHash(bHash);
	}
}
