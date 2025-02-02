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
import edu.umd.cs.findbugs.filter.FirstVersionMatcher;
import edu.umd.cs.findbugs.filter.LastVersionMatcher;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.filter.NotMatcher;
import edu.umd.cs.findbugs.filter.OrMatcher;
import edu.umd.cs.findbugs.filter.PriorityMatcher;
import edu.umd.cs.findbugs.filter.RankMatcher;
import edu.umd.cs.findbugs.filter.RelationalOp;
import edu.umd.cs.findbugs.gui2.BugAspects.SortableValue;
import edu.umd.cs.findbugs.util.NotImplementedYetException;

/**
 * @author pugh
 */
public class FilterFactory {

    public static Matcher makeOrMatcher(Collection<SortableValue> sortables) {
        return makeMatcher(sortables, false);
    }

    public static Matcher makeAndMatcher(Collection<SortableValue> sortables) {
        return makeMatcher(sortables, true);
    }

    public static Matcher invertMatcher(Matcher originalMatcher) {
        if (originalMatcher instanceof NotMatcher) {
            return ((NotMatcher) originalMatcher).originalMatcher();
        }

        NotMatcher notMatcher = new NotMatcher();
        notMatcher.addChild(originalMatcher);
        return notMatcher;
    }

    private static Matcher makeMatcher(Collection<SortableValue> sortables, boolean andOr) {
        if (sortables.size() == 1) {
            return makeMatcher(sortables.iterator().next());
        }
        edu.umd.cs.findbugs.filter.CompoundMatcher matcher;
        if (andOr == true) {
            matcher = new AndMatcher();
        } else {
            matcher = new OrMatcher();
        }
        for (SortableValue s : sortables) {
            matcher.addChild(makeMatcher(s));
        }
        return matcher;
    }

    public static Matcher makeMatcher(Collection<Sortables> sortables, BugInstance bug) {
        if (sortables.size() == 1) {
            return makeMatcher(sortables.iterator().next(), bug);
        }
        AndMatcher matcher = new AndMatcher();
        for (Sortables s : sortables) {
            matcher.addChild(makeMatcher(s, bug));
        }
        return matcher;
    }


    public static boolean canFilter(Sortables s) {
        switch (s) {
        case BUGCODE:
        case CATEGORY:
        case CLASS:
        case FIRSTVERSION:
        case LASTVERSION:
        case PACKAGE:
        case PRIORITY:
        case TYPE:
        case BUG_RANK:
            return true;
        default:
            return false;
        }
    }


    private static Matcher makeMatcher(Sortables s, BugInstance bug) {
        switch (s) {
        case BUGCODE:
            return new BugMatcher(s.getFrom(bug), null, null);
        case CATEGORY:
            return new BugMatcher(null, null, s.getFrom(bug));
        case CLASS:
            return new ClassMatcher(s.getFrom(bug));
        case FIRSTVERSION:
            return new FirstVersionMatcher(s.getFrom(bug), RelationalOp.EQ);
        case LASTVERSION:
            return new LastVersionMatcher(s.getFrom(bug), RelationalOp.EQ);
        case PACKAGE:
            String p = Sortables.CLASS.getFrom(bug);
            int lastDot = p.lastIndexOf('.');
            if (lastDot > 0) {
                p = p.substring(0, lastDot);
            }
            return new ClassMatcher("~" + p + "\\.[^.]+");
        case PRIORITY:
            return new PriorityMatcher(Integer.toString(bug.getPriority()));

        case TYPE:
            return new BugMatcher(null, s.getFrom(bug), null);

        case BUG_RANK:
            return new RankMatcher(s.getFrom(bug));

        case DIVIDER:
        default:
            throw new IllegalArgumentException("Don't know how to make maker for " + s);
        }
    }

    private static Matcher makeMatcher(SortableValue sv) {
        Sortables s = sv.key;
        String value = sv.value;
        switch (s) {
        case BUG_RANK:
            return new RankMatcher(value);
        case BUGCODE:
            return new BugMatcher(value, null, null);
        case CATEGORY:
            return new BugMatcher(null, null, value);
        case CLASS:
            return new ClassMatcher(value);
        case FIRSTVERSION:
            return new FirstVersionMatcher(value, RelationalOp.EQ);
        case LASTVERSION:
            return new LastVersionMatcher(value, RelationalOp.EQ);
        case PACKAGE:
            return new ClassMatcher("~" + value + "\\.[^.]+");
        case PRIORITY:
            return new PriorityMatcher(value);
        case PROJECT:
            throw new NotImplementedYetException("Matchers for Projects not supported yet");

        case TYPE:
            return new BugMatcher(null, value, null);

        case DIVIDER:
        default:
            throw new NotImplementedYetException("No matcher for " + s);

        }

    }


}
