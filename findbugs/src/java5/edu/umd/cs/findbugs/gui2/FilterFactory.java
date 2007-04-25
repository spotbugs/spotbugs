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

package edu.umd.cs.findbugs.gui2;

import java.util.Collection;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.filter.AndMatcher;
import edu.umd.cs.findbugs.filter.BugMatcher;
import edu.umd.cs.findbugs.filter.ClassMatcher;
import edu.umd.cs.findbugs.filter.Matcher;

/**
 * @author pugh
 */
public class FilterFactory {
	
	public static AndMatcher makeMatcher(Collection<Sortables> sortables, BugInstance bug) {
		AndMatcher matcher = new AndMatcher();
		for(Sortables s : sortables) 
			matcher.addChild(makeMatcher(s, bug));
		return matcher;
	}

	/**
     * @param s
     * @param bug
     * @return
     */
    private static Matcher makeMatcher(Sortables s, BugInstance bug) {
    	switch(s) {
    	case BUGCODE:
    		return new BugMatcher(s.getFrom(bug), null, null);
    	case CATEGORY:
    		return new BugMatcher(null, null, s.getFrom(bug));
    	case CLASS:
    		return new ClassMatcher(s.getFrom(bug));
    	case DESIGNATION:
    	case FIRSTVERSION:
    	case LASTVERSION:
    	case PACKAGE:
    	case PRIORITY:
    		 throw new UnsupportedOperationException();
    	case TYPE:
    		return new BugMatcher(null, s.getFrom(bug), null);
    		
    	case DIVIDER:
		default:
    		throw new IllegalArgumentException();

    			 
    	}
	  
    }

}
