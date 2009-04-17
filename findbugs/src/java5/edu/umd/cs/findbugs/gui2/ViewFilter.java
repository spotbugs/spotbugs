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
	enum RankFilter  implements  ViewFilterEnum { SCARIEST(4), SCARY(9), TROUBLING(14), ALL(Integer.MAX_VALUE);
		final int maxRank;
		

	private RankFilter(int maxRank) {
	        this.maxRank = maxRank;
        }

    public boolean show(MainFrame mf, BugInstance b) {
    	int rank = BugRanker.findRank(b);
	    return rank <= maxRank;
    }

	
 }
	
	enum FirstSeenFilter  implements ViewFilterEnum { LAST_DAY(1), LAST_WEEK(7), LAST_MONTH(30), LAST_THREE_MONTHS(91), ALL(400000);

	final int maxDays;
	
    private FirstSeenFilter(int days) {
	    this.maxDays = days;
    }

	public boolean show(MainFrame mf, BugInstance b) {
	    long firstSeen = mf.bugCollection.getCloud().getFirstSeen(b);
	    long time = System.currentTimeMillis() - firstSeen;
	    long days = TimeUnit.SECONDS.convert(time, TimeUnit.MILLISECONDS) / 3600 / 24;
	    return days < this.maxDays;   
    } }
	
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
