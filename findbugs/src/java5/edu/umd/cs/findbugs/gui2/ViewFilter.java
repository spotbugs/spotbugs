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

package edu.umd.cs.findbugs.gui2;

import java.util.concurrent.TimeUnit;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugRanker;

/**
 * @author pugh
 */
public class ViewFilter {
	
	
	public ViewFilter(MainFrame mf) {
		this.mf = mf;
	}
	interface ViewFilterEnum {
		boolean show(MainFrame mf, BugInstance b);
	}
	enum RankFilter  implements  ViewFilterEnum { SCARIEST(4, "Scariest"), SCARY(9, "Scary"), TROUBLING(14, "Troubling"), 
		   ALL(Integer.MAX_VALUE, "All bug ranks");
		final int maxRank;
		final String displayName;
		

	private RankFilter(int maxRank, String displayName) {
	        this.maxRank = maxRank;
	        this.displayName = displayName;
        }

    public boolean show(MainFrame mf, BugInstance b) {
    	int rank = BugRanker.findRank(b);
	    return rank <= maxRank;
    }
    @Override
    public String toString() {
    	if (maxRank < Integer.MAX_VALUE)
    		return displayName + " (Ranks 1 - " + maxRank + ")";
    	return displayName;
    }

	
 }
	
	enum FirstSeenFilter  implements ViewFilterEnum { LAST_DAY(1, "Last day"), LAST_WEEK(7, "Last week"), LAST_MONTH(30, "Last month"), LAST_THREE_MONTHS(91, "Last 90 days"), ALL(400000, "No matter when first seen");

	final int maxDays;
	final String displayName;
	
    private FirstSeenFilter(int days, String displayName) {
	    this.maxDays = days;
	    this.displayName = displayName;
    }

	public boolean show(MainFrame mf, BugInstance b) {
	    long firstSeen = mf.bugCollection.getCloud().getFirstSeen(b);
	    long time = System.currentTimeMillis() - firstSeen;
	    long days = TimeUnit.SECONDS.convert(time, TimeUnit.MILLISECONDS) / 3600 / 24;
	    return days < this.maxDays;   
    } 
	@Override
    public String toString() {
		return displayName;
	}
	}
	
	final MainFrame mf;
	RankFilter rank = RankFilter.ALL;
	FirstSeenFilter firstSeen = FirstSeenFilter.ALL;
	
	String [] packagePrefixes;
	void setPackagesToDisplay(String value) {
		packagePrefixes = value.replace('/','.').split("[ ,:]+");
		FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);
	}
	
	public RankFilter getRank() {
    	return rank;
    }

	public void setRank(RankFilter rank) {
    	this.rank = rank;
    	FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);
    	
    }

	public FirstSeenFilter getFirstSeen() {
    	return firstSeen;
    }

	public void setFirstSeen(FirstSeenFilter firstSeen) {
    	this.firstSeen = firstSeen;
    	FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);
    	
    }

	public String[] getPackagePrefixes() {
    	return packagePrefixes;
    }

	public boolean show(BugInstance b) {
		if (packagePrefixes != null && packagePrefixes.length > 0) {
			String packageName = b.getPrimaryClass().getPackageName();
			boolean match = false;
			for(String p : packagePrefixes) 
				if (packageName.startsWith(p)) {
					match = true;
					break;
				}
			if (!match) 
				return false;
		}
		
		if (!firstSeen.show(mf,b)) 
			return false;
		if (!rank.show(mf, b))
			return false;
		
		return true;

	}
	

}
