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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ProjectPackagePrefixes;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.BugFilingStatus;
import edu.umd.cs.findbugs.cloud.Cloud.Mode;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * A useful enum for dealing with all the types of filterable and sortable data
 * in BugInstances This is the preferred way for getting the information out of
 * a BugInstance and formatting it for display It also has the comparators for
 * the different types of data
 *
 * @author Reuven
 */

public enum Sortables implements Comparator<String> {

    FIRST_SEEN(edu.umd.cs.findbugs.L10N.getLocalString("sort.first_seen", "First Seen")) {
        @Override
        public String getFrom(BugInstance bug) {
            long firstSeen = getFirstSeen(bug);
            return Long.toString(firstSeen);
        }

        /**
         * @param bug
         * @return
         */
        private long getFirstSeen(BugInstance bug) {
            BugCollection bugCollection = MainFrame.getInstance().getBugCollection();
            long firstSeen = bugCollection.getCloud().getFirstSeen(bug);
            return firstSeen;
        }

        @Override
        public String formatValue(String value) {
            long when = Long.parseLong(value);
            return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(when);
        }

        @Override
        public int compare(String one, String two) {
            // Numerical (zero is first)
            return Long.valueOf(one).compareTo(Long.valueOf(two));
        }

        @Override
        public boolean isAvailable(MainFrame mainframe) {
            BugCollection bugCollection = mainframe.getBugCollection();
            return bugCollection != null;
        }
    },

    FIRSTVERSION(edu.umd.cs.findbugs.L10N.getLocalString("sort.first_version", "First Version")) {
        @Override
        public String getFrom(BugInstance bug) {
            return Long.toString(bug.getFirstVersion());
        }

        @Override
        public String formatValue(String value) {
            int seqNum = Integer.parseInt(value);
            BugCollection bugCollection = MainFrame.getInstance().getBugCollection();
            if (bugCollection == null) {
                return "--";
            }
            AppVersion appVersion = bugCollection.getAppVersionFromSequenceNumber(seqNum);
            if (appVersion != null) {
                String timestamp = new Timestamp(appVersion.getTimestamp()).toString();
                return appVersion.getReleaseName() + " (" + timestamp.substring(0, timestamp.indexOf(' ')) + ")";
            } else {
                return "#" + seqNum;
            }
        }

        @Override
        public int compare(String one, String two) {
            // Numerical (zero is first)
            return Integer.valueOf(one).compareTo(Integer.valueOf(two));
        }

        @Override
        public boolean isAvailable(MainFrame mainframe) {
            BugCollection bugCollection = mainframe.getBugCollection();
            if (bugCollection == null) {
                return true;
            }
            long sequenceNumber = bugCollection.getCurrentAppVersion().getSequenceNumber();
            return sequenceNumber > 0;

        }
    },

    LASTVERSION(edu.umd.cs.findbugs.L10N.getLocalString("sort.last_version", "Last Version")) {
        @Override
        public String getFrom(BugInstance bug) {
            return Long.toString(bug.getLastVersion());
        }

        @Override
        public String formatValue(String value) {
            // System.out.println("Formatting last version value");
            if ("-1".equals(value)) {
                return "";
            }
            int seqNum = Integer.parseInt(value);
            BugCollection bugCollection = MainFrame.getInstance().getBugCollection();
            if (bugCollection == null) {
                return "--";
            }
            AppVersion appVersion = bugCollection.getAppVersionFromSequenceNumber(seqNum);
            if (appVersion != null) {
                String timestamp = new Timestamp(appVersion.getTimestamp()).toString();
                return appVersion.getReleaseName() + " (" + timestamp.substring(0, timestamp.indexOf(' ')) + ")";
            } else {
                return "#" + seqNum;
            }
        }

        @Override
        public int compare(String one, String two) {
            if (one.equals(two)) {
                return 0;
            }

            // Numerical (except that -1 is last)
            int first = Integer.parseInt(one);
            int second = Integer.parseInt(two);
            if (first == second) {
                return 0;
            }
            if (first < 0) {
                return 1;
            }
            if (second < 0) {
                return -1;
            }
            if (first < second) {
                return -1;
            }
            return 1;
        }

        @Override
        public boolean isAvailable(MainFrame mainframe) {
            BugCollection bugCollection = mainframe.getBugCollection();
            if (bugCollection == null) {
                return true;
            }
            return bugCollection.getCurrentAppVersion().getSequenceNumber() > 0;

        }

    },

    PRIORITY(edu.umd.cs.findbugs.L10N.getLocalString("sort.priority", "Confidence")) {
        @Override
        public String getFrom(BugInstance bug) {
            return String.valueOf(bug.getPriority());
        }

        @Override
        public String formatValue(String value) {
            if (value.equals(String.valueOf(Priorities.HIGH_PRIORITY))) {
                return edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_high", "High");
            }
            if (value.equals(String.valueOf(Priorities.NORMAL_PRIORITY))) {
                return edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_normal", "Normal");
            }
            if (value.equals(String.valueOf(Priorities.LOW_PRIORITY))) {
                return edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_low", "Low");
            }
            if (value.equals(String.valueOf(Priorities.EXP_PRIORITY))) {
                return edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_experimental", "Experimental");
            }
            return edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_ignore", "Ignore"); // This
            // probably
            // shouldn't
            // ever
            // happen,
            // but
            // what
            // the
            // hell,
            // let's
            // be
            // complete

        }

        @Override
        public int compare(String one, String two) {
            // Numerical
            return Integer.valueOf(one).compareTo(Integer.valueOf(two));
        }
    },
    CLASS(edu.umd.cs.findbugs.L10N.getLocalString("sort.class", "Class")) {
        @Override
        public String getFrom(BugInstance bug) {
            return bug.getPrimarySourceLineAnnotation().getClassName();
        }

        @Override
        public int compare(String one, String two) {
            // If both have dollar signs and are of the same outer class,
            // compare the numbers after the dollar signs.
            try {
                if (one.contains("$") && two.contains("$")
                        && one.substring(0, one.lastIndexOf('$')).equals(two.substring(0, two.lastIndexOf('$')))) {
                    return Integer.valueOf(one.substring(one.lastIndexOf('$'))).compareTo(
                            Integer.valueOf(two.substring(two.lastIndexOf('$'))));
                }
            } catch (NumberFormatException e) {
            } // Somebody's playing silly buggers with dollar signs, just do it
            // lexicographically

            // Otherwise, lexicographicalify it
            return one.compareTo(two);
        }
    },
    PACKAGE(edu.umd.cs.findbugs.L10N.getLocalString("sort.package", "Package")) {
        @Override
        public String getFrom(BugInstance bug) {
            return bug.getPrimarySourceLineAnnotation().getPackageName();
        }

        @Override
        public String formatValue(String value) {
            if ("".equals(value)) {
                return "(Default)";
            }
            return value;
        }
    },
    PACKAGE_PREFIX(edu.umd.cs.findbugs.L10N.getLocalString("sort.package_prefix", "Package prefix")) {
        @Override
        public String getFrom(BugInstance bug) {
            int count = GUISaveState.getInstance().getPackagePrefixSegments();

            if (count < 1) {
                count = 1;
            }
            String packageName = bug.getPrimarySourceLineAnnotation().getPackageName();
            return ClassName.extractPackagePrefix(packageName, count);
        }

        @Override
        public String formatValue(String value) {
            return value + "...";
        }
    },
    CATEGORY(edu.umd.cs.findbugs.L10N.getLocalString("sort.category", "Category")) {
        @Override
        public String getFrom(BugInstance bug) {

            BugPattern bugPattern = bug.getBugPattern();
            return bugPattern.getCategory();
        }

        @Override
        public String formatValue(String value) {
            return I18N.instance().getBugCategoryDescription(value);
        }

        @Override
        public int compare(String one, String two) {
            String catOne = one;
            String catTwo = two;
            int compare = catOne.compareTo(catTwo);
            if (compare == 0) {
                return 0;
            }
            if ("CORRECTNESS".equals(catOne)) {
                return -1;
            }
            if ("CORRECTNESS".equals(catTwo)) {
                return 1;
            }
            return compare;

        }

    },
    DESIGNATION(edu.umd.cs.findbugs.L10N.getLocalString("sort.designation", "Designation")) {
        @Override
        public String getFrom(BugInstance bug) {
            return bug.getUserDesignationKey();
        }

        /**
         * value is the key of the designations.
         *
         * @param value
         * @return
         */
        @Override
        public String formatValue(String value) {
            return I18N.instance().getUserDesignation(value);
        }

        @Override
        public String[] getAllSorted() {// FIXME I think we always want user to
            // see all possible designations, not
            // just the ones he has set in his
            // project, Agreement? -Dan
            List<String> sortedDesignations = I18N.instance().getUserDesignationKeys(true);
            return sortedDesignations.toArray(new String[sortedDesignations.size()]);
        }
    },
    BUGCODE(edu.umd.cs.findbugs.L10N.getLocalString("sort.bug_kind", "Bug Kind")) {
        @Override
        public String getFrom(BugInstance bug) {
            BugPattern bugPattern = bug.getBugPattern();
            return bugPattern.getAbbrev();
        }

        @Override
        public String formatValue(String value) {
            return I18N.instance().getBugTypeDescription(value);
        }

        @Override
        public int compare(String one, String two) {
            return formatValue(one).compareTo(formatValue(two));
        }
    },
    TYPE(edu.umd.cs.findbugs.L10N.getLocalString("sort.bug_pattern", "Bug Pattern")) {
        @Override
        public String getFrom(BugInstance bug) {
            return bug.getBugPattern().getType();
        }

        @Override
        public String formatValue(String value) {
            return I18N.instance().getShortMessageWithoutCode(value);
        }
    },
    CONSENSUS(edu.umd.cs.findbugs.L10N.getLocalString("sort.consensus", "Consensus")) {
        @Override
        public String getFrom(BugInstance bug) {
            BugCollection bugCollection = MainFrame.getInstance().getBugCollection();

            return bugCollection.getCloud().getConsensusDesignation(bug).name();
        }

        @Override
        public String formatValue(String value) {
            return I18N.instance().getUserDesignation(value);
        }

        @Override
        public boolean isAvailable(MainFrame mf) {
            BugCollection bugCollection = mf.getBugCollection();
            if (bugCollection == null) {
                return false;
            }
            return bugCollection.getCloud().getMode() == Mode.COMMUNAL;

        }
    },

    BUG_RANK(edu.umd.cs.findbugs.L10N.getLocalString("sort.bug_bugrank", "Bug Rank")) {
        String[] values;
        {
            values = new String[40];
            for (int i = 0; i < values.length; i++) {
                values[i] = String.format("%2d", i);
            }
        }

        @Override
        public String getFrom(BugInstance bug) {
            int rank = BugRanker.findRank(bug);
            return values[rank];
        }

        @Override
        public String formatValue(String value) {
            return value;
        }
    },

    BUG_STATUS(edu.umd.cs.findbugs.L10N.getLocalString("sort.bug_bugstatus", "Status")) {
        @Override
        public String getFrom(BugInstance bug) {

            BugCollection bugCollection = MainFrame.getInstance().getBugCollection();

            Cloud cloud = bugCollection.getCloud();
            assert cloud != null;
            BugFilingStatus status = cloud.getBugLinkStatus(bug);
            if (status == BugFilingStatus.VIEW_BUG) {
                String bugStatus = cloud.getBugStatus(bug);
                if (bugStatus != null) {
                    return bugStatus;
                }
            }

            return CONSENSUS.getFrom(bug);
        }

        @Override
        public String formatValue(String value) {
            return value;
        }

        @Override
        public boolean isAvailable(MainFrame mf) {
            BugCollection bugCollection = mf.getBugCollection();
            if (bugCollection == null) {
                return false;
            }
            boolean a = bugCollection.getCloud().supportsBugLinks() && bugCollection.getCloud().getMode() == Mode.COMMUNAL;
            return a;

        }
    },

    PROJECT(edu.umd.cs.findbugs.L10N.getLocalString("sort.bug_project", "Project")) {

        @Override
        public String getFrom(BugInstance bug) {
            ProjectPackagePrefixes p = MainFrame.getInstance().getProjectPackagePrefixes();
            Collection<String> projects = p.getProjects(bug.getPrimaryClass().getClassName());
            if (projects.size() == 0) {
                return "unclassified";
            }
            String result = projects.toString();

            return result.substring(1, result.length() - 1);
        }

        @Override
        public boolean isAvailable(MainFrame mf) {
            return mf.getProjectPackagePrefixes().size() > 0;
        }

    },

    DIVIDER(" ") {
        @Override
        public String getFrom(BugInstance bug) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String[] getAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String formatValue(String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compare(String one, String two) {
            throw new UnsupportedOperationException();
        }
    };

    String prettyName;

    Sortables(String prettyName) {
        this.prettyName = prettyName;
        this.bugLeafNodeComparator = new Comparator<BugLeafNode>() {
            @Override
            public int compare(BugLeafNode one, BugLeafNode two) {
                return Sortables.this.compare(Sortables.this.getFrom(one.getBug()), Sortables.this.getFrom(two.getBug()));
            }
        };
    }

    @Override
    public String toString() {
        return prettyName;
    }

    public abstract String getFrom(BugInstance bug);

    public String[] getAll() {
        return getAll(BugSet.getMainBugSet());
    }

    public String[] getAll(BugSet set) {
        return set.getAll(this);
    }

    public String formatValue(String value) {
        return value;
    }

    @Override
    public int compare(String one, String two) {
        // Lexicographical by default
        return one.compareTo(two);
    }

    public String[] getAllSorted() {
        return getAllSorted(BugSet.getMainBugSet());
    }

    public String[] getAllSorted(BugSet set) {
        String[] values = getAll(set);
        Arrays.sort(values, this);
        return values;
    }

    private final SortableStringComparator comparator = new SortableStringComparator(this);

    public SortableStringComparator getComparator() {
        return comparator;
    }

    final Comparator<BugLeafNode> bugLeafNodeComparator;

    public Comparator<BugLeafNode> getBugLeafNodeComparator() {
        return bugLeafNodeComparator;
    }

    public boolean isAvailable(MainFrame frame) {
        return true;
    }

    public static Sortables getSortableByPrettyName(String name) {
        for (Sortables s : values()) {
            if (s.prettyName.equals(name)) {
                return s;
            }
        }
        return null;
    }
}
