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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.DBCloud;
import edu.umd.cs.findbugs.cloud.UserDesignation;
import edu.umd.cs.findbugs.cloud.Cloud.BugFilingStatus;

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

	enum RankFilter implements ViewFilterEnum {
		SCARIEST(4, "Scariest"), SCARY(9, "Scary"), TROUBLING(14, "Troubling"), ALL(Integer.MAX_VALUE, "All bug ranks");
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

	enum EvaluationFilter implements ViewFilterEnum {
		NO_REVIEWS("No one has reviewed") {
	        @Override
	        boolean show(DBCloud cloud, BugInstance b) {
		        return cloud.getReviewers(b).isEmpty();
	        }
        }, MY_REVIEWS("My reviews") {
	        @Override
	        boolean show(DBCloud cloud, BugInstance b) {
	        	return cloud.getReviewers(b).contains(cloud.getUser());
	        }},
	       NOT_REVIEWED_BY_ME("Not reviewed by me") {
		        @Override
		        boolean show(DBCloud cloud, BugInstance b) {
		        	return !cloud.getReviewers(b).contains(cloud.getUser());
		        }
        }, HAS_REVIEWS("Someone has reviewed") {
	        @Override
	        boolean show(DBCloud cloud, BugInstance b) {
	        	return !cloud.getReviewers(b).isEmpty();
	        }
        },  NO_ONE_COMMITTED_TO_FIXING("Has no fixers") {
	        @Override
	        boolean show(DBCloud cloud, BugInstance b) {
	        	return !cloud.isClaimed(b);
	        }
        }, HAS_FILED_BUGS("Has bugs filed against them") {
	        @Override
	        boolean show(DBCloud cloud, BugInstance b) {
	        	return cloud.getBugLinkStatus(b).bugIsFiled();
	        	
	        }
        }, NO_FILED_BUGS("Don't have bugs filed against them") {
	        @Override
	        boolean show(DBCloud cloud, BugInstance b) {
	        	return !cloud.getBugLinkStatus(b).bugIsFiled();
	        }
        },  I_WILL_FIX("I will fix") {
	        @Override
	        boolean show(DBCloud cloud, BugInstance b) {
	        	return cloud.getUserDesignation(b) == UserDesignation.I_WILL_FIX;
	        }
        }, ALL("All issues") {
	        @Override
	        boolean show(DBCloud cloud, BugInstance b) {
		       return true;
	        }
        }; 
        
        EvaluationFilter(String displayName) {
        	this.displayName = displayName;
        }
		final String displayName;

		abstract boolean show(DBCloud cloud, BugInstance b);
        public boolean show(MainFrame mf, BugInstance b) {
	        Cloud c = mf.bugCollection.getCloud();
	        if (c instanceof DBCloud) 
	        	return show((DBCloud) c, b);
	        return true;
        }
        @Override
        public String toString() {
        	return displayName;
        }
	}
	enum FirstSeenFilter implements ViewFilterEnum {
		LAST_DAY(1, "Last day"), LAST_WEEK(7, "Last week"), LAST_MONTH(30, "Last month"), LAST_THREE_MONTHS(91, "Last 90 days"), ALL(
		        400000, "No matter when first seen");

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
	EvaluationFilter eval = EvaluationFilter.ALL;

	FirstSeenFilter firstSeen = FirstSeenFilter.ALL;

	String[] packagePrefixes;

	void setPackagesToDisplay(String value) {
		value = value.replace('/', '.').trim();
		if (value.length() == 0)
			packagePrefixes = new String[0];
		else
			packagePrefixes = value.split("[ ,:]+");
		FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);
	}

	public RankFilter getRank() {
		return rank;
	}

	public void setRank(RankFilter rank) {
		this.rank = rank;
		FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);

	}
	
	public EvaluationFilter getEvaluation() {
		return eval;
	}
	public void setEvaluation(EvaluationFilter eval) {
		this.eval = eval;
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
		String[] pp = packagePrefixes;
		if (pp != null && pp.length > 0) {
			String packageName = b.getPrimaryClass().getClassName();
			boolean match = false;
			for (String p : pp)
				if (p.length() > 0 && packageName.startsWith(p)) {
					match = true;
					break;
				}
			if (!match) {
				return false;
			}
		}

		if (!firstSeen.show(mf, b)) {
			return false;
		}
		if (!rank.show(mf, b)) {
			return false;
		}
		if (!eval.show(mf, b)) {
			return false;
		}
		return true;

	}

}
