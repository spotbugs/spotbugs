/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author pwilliam
 */
public class BugRanker {
	
	/**
	 * @param u may be null. In this case, a default value will be used for all bugs
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public BugRanker(URL u) throws UnsupportedEncodingException, IOException {
		if(u == null){
			return;
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream(), "UTF-8"));
		while (true) {
			String s = in.readLine();
			if (s == null) break;
			String parts [] = s.split(" ");
			int rank = Integer.parseInt(parts[0]);
			String kind = parts[1];
			String what = parts[2];
			if (kind.equals("BugPattern"))
				bugPatterns.put(what, rank);
			else if (kind.equals("BugKind"))
				bugKinds.put(what, rank);
			else if (kind.equals("Category"))
				bugCategories.put(what, rank);
			else
				AnalysisContext.logError("Can't parse bug rank " + s);
		}
		Util.closeSilently(in);
	}

	private final HashMap<String, Integer> bugPatterns = new HashMap<String, Integer>();

	private final HashMap<String, Integer> bugKinds = new HashMap<String, Integer>();

	private final HashMap<String, Integer> bugCategories = new HashMap<String, Integer>();

	public int rankBug(BugInstance bug) {
		BugPattern bugPattern = bug.getBugPattern();
		int priority = bug.getPriority();
		return rankBugPattern(bugPattern, priority);

	}

	/**
     * @param bugPattern
     * @param priority
     * @return
     */
    public int rankBugPattern(BugPattern bugPattern, int priority) {
	    Integer value = rankBugPattern(bugPattern);
		if (value == null)
			return 25;
		int v = value.intValue();
		switch (priority) {
		case Priorities.HIGH_PRIORITY:
			break;
		case Priorities.NORMAL_PRIORITY:
			v += 2;
			break;
		case Priorities.LOW_PRIORITY:
			v += 5;
			break;
		default:
			return 26;
		}
		return Math.min(20, Math.max(1, v));
    }

	private Integer rankBugPattern(BugPattern bugPattern) {
	    String type = bugPattern.getType();
		Integer value = bugPatterns.get(type);
		if (value == null) {
			value = bugKinds.get(bugPattern.getAbbrev());
			if (value == null)
				value = bugCategories.get(bugPattern.getCategory());
			bugPatterns.put(type, value);
		}
	    return value;
    }
	
	public static int findRank(BugInstance bug) {
		int finalRank = 30;
		for(Plugin p : DetectorFactoryCollection.instance().plugins()) {
			BugRanker r = p.getBugRanker();
			finalRank = Math.min(finalRank,r.rankBug(bug));

		}
		return finalRank;
	}

	public static int findRank(BugPattern pattern, int priority) {
		int finalRank = 30;
		for(Plugin p : DetectorFactoryCollection.instance().plugins()) {
			BugRanker r = p.getBugRanker();
			finalRank = Math.min(finalRank, r.rankBugPattern(pattern, priority));
		}
		return finalRank;
	}


    public static void trimToMaxRank(BugCollection origCollection, int maxRank) {
	    if (maxRank < 20) 
	    	for(Iterator<BugInstance> i = origCollection.getCollection().iterator(); i.hasNext(); ) {
	    		BugInstance b = i.next();
	    		if (BugRanker.findRank(b) > maxRank)
	    			i.remove();

	    	}
    }

}
