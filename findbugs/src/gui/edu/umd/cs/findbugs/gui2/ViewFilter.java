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
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.ClassName;

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

    enum PriorityFilter implements ViewFilterEnum {
        HIGH_PRIORITY(1, "High priority only"), NORMAL_PRIORITY(2, "High and normal priority"), ALL_BUGS(10, "All bug priorities");

        final int maxPriority;
        final String displayName;

        private PriorityFilter(int maxPriority, String displayName) {
            this.maxPriority = maxPriority;
            this.displayName = displayName;
        }

        @Override
        public boolean show(MainFrame mf, BugInstance b) {
            return b.getPriority() <= maxPriority;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    enum RankFilter implements ViewFilterEnum {
        SCARIEST(4, "Scariest"), SCARY(9, "Scary"), TROUBLING(14, "Troubling"), ALL(Integer.MAX_VALUE, "All bug ranks");
        final int maxRank;

        final String displayName;

        private RankFilter(int maxRank, String displayName) {
            this.maxRank = maxRank;
            this.displayName = displayName;
        }

        @Override
        public boolean show(MainFrame mf, BugInstance b) {
            int rank = BugRanker.findRank(b);
            return rank <= maxRank;
        }

        @Override
        public String toString() {
            if (maxRank < Integer.MAX_VALUE) {
                return displayName + " (Ranks 1 - " + maxRank + ")";
            }
            return displayName;
        }

    }

    enum OverallClassificationFilter implements ViewFilterEnum {
        NOT_HARMLESS("Not classified as harmless") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                double score = cloud.getClassificationScore(b);
                if (score <= UserDesignation.MOSTLY_HARMLESS.score()) {
                    return false;
                }

                score = cloud.getPortionObsoleteClassifications(b);
                if (score >= 0.5) {
                    return false;
                }

                return true;
            }
        },
        SHOULD_FIX("Overall classification is should fix") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                double score = cloud.getClassificationScore(b);
                return score >= UserDesignation.SHOULD_FIX.score();
            }
        },
        DONT_FIX("Overall classification is don't fix") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                double score = cloud.getClassificationScore(b);
                return score <= UserDesignation.MOSTLY_HARMLESS.score();
            }
        },
        OBSOLETE("Overall classification is obsolete code") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                double score = cloud.getPortionObsoleteClassifications(b);
                return score >= 0.5;
            }
        },
        UNCERTAIN("Overall classification is uncertain") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                if (SHOULD_FIX.show(cloud, b) || DONT_FIX.show(cloud, b) || OBSOLETE.show(cloud, b)) {
                    return false;
                }
                if (cloud.getNumberReviewers(b) >= 2) {
                    return true;
                }
                return false;
            }
        },
        HIGH_VARIANCE("Controversial") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                double variance = cloud.getClassificationDisagreement(b);
                return variance > 0.26;
            }

        },
        ALL("All issues") {

            @Override
            public boolean show(MainFrame mf, BugInstance b) {
                return true;
            }

            @Override
            boolean show(Cloud cloud, BugInstance b) {
                return true;
            }

        };
        OverallClassificationFilter(String displayName) {
            this.displayName = displayName;
        }

        final String displayName;

        abstract boolean show(Cloud cloud, BugInstance b);

        public boolean supported(Cloud cloud) {
            return true;
        }

        @Override
        public boolean show(MainFrame mf, BugInstance b) {
            Cloud c = mf.getBugCollection().getCloud();
            return c.isInCloud(b) && show(c, b);

        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    enum CloudFilter implements ViewFilterEnum {
        MY_REVIEWS("Classified by me") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                return cloud.getReviewers(b).contains(cloud.getUser());
            }

        },
        NOT_REVIEWED_BY_ME("Not classified by me") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                return !cloud.getReviewers(b).contains(cloud.getUser());
            }

        },
        NO_REVIEWS("No one has classified") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                return cloud.getReviewers(b).isEmpty();
            }

            @Override
            public boolean supported(Cloud cloud) {
                return cloud.getMode() != Cloud.Mode.SECRET;
            }
        },
        HAS_REVIEWS("Someone has classified") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                return !cloud.getReviewers(b).isEmpty();
            }

            @Override
            public boolean supported(Cloud cloud) {
                return cloud.getMode() != Cloud.Mode.SECRET;
            }
        },
        NO_ONE_COMMITTED_TO_FIXING("Has no fixers") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                return supported(cloud) && cloud.claimedBy(b) != null;
            }

            @Override
            public boolean supported(Cloud cloud) {
                return cloud.supportsClaims() && cloud.getMode() != Cloud.Mode.SECRET;
            }
        },
        I_WILL_FIX("I will fix") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                return cloud.getIWillFix(b);

            }

        },
        HAS_FILED_BUGS("Has entry in bug database") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                return cloud.getBugLinkStatus(b).bugIsFiled();

            }

            @Override
            public boolean supported(Cloud cloud) {
                return cloud.supportsBugLinks();
            }
        },
        NO_FILED_BUGS("Don't have entry in bug database") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                return !cloud.getBugLinkStatus(b).bugIsFiled();
            }

            @Override
            public boolean supported(Cloud cloud) {
                return cloud.supportsBugLinks();
            }
        },
        WILL_NOT_FIX("bug database entry marked Will Not Fix") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                return cloud.getWillNotBeFixed(b);
            }

            @Override
            public boolean supported(Cloud cloud) {
                return cloud.supportsBugLinks();
            }
        },
        BUG_STATUS_IS_UNASSIGNED("bug database entry is unassigned") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                return cloud.getBugIsUnassigned(b);
            }

            @Override
            public boolean supported(Cloud cloud) {
                return cloud.supportsBugLinks();
            }
        },
        ALL("All issues") {
            @Override
            boolean show(Cloud cloud, BugInstance b) {
                return true;
            }

            @Override
            public boolean show(MainFrame mf, BugInstance b) {
                return true;
            }
        };

        CloudFilter(String displayName) {
            this.displayName = displayName;
        }

        final String displayName;

        abstract boolean show(Cloud cloud, BugInstance b);

        public boolean supported(Cloud cloud) {
            return true;
        }

        @Override
        public boolean show(MainFrame mf, BugInstance b) {
            Cloud c = mf.getBugCollection().getCloud();
            return c.isInCloud(b) && show(c, b);
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    static final int NO_MATTER_WHEN_FIRST_SEEN = 400000;

    enum FirstSeenFilter implements ViewFilterEnum {
        LAST_DAY(1, "Last day"), LAST_3_DAYS(3, "Last 3 days"), LAST_WEEK(7, "Last week"), LAST_MONTH(30, "Last month"), LAST_THREE_MONTHS(
                91, "Last 90 days"), LAST_YEAR(
                        365, "Last year"), ALL(Integer.MAX_VALUE, "All time") {
            @Override
            public boolean show(MainFrame mf, BugInstance b) {
                return true;
            }

        };

        final int maxDays;

        final String displayName;

        private FirstSeenFilter(int days, String displayName) {
            this.maxDays = days;
            this.displayName = displayName;
        }

        @Override
        public boolean show(MainFrame mf, BugInstance b) {
            Cloud cloud = mf.getBugCollection().getCloud();
            if (!cloud.isInCloud(b)) {
                return false;
            }
            long firstSeen = cloud.getFirstSeen(b);
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

    PriorityFilter priority = PriorityFilter.ALL_BUGS;

    CloudFilter eval = CloudFilter.ALL;

    OverallClassificationFilter classificationFilter = OverallClassificationFilter.ALL;

    FirstSeenFilter firstSeen = FirstSeenFilter.ALL;

    String[] classSearchStrings;


    static final Pattern legalClassSearchString = Pattern.compile("[\\p{javaLowerCase}\\p{javaUpperCase}0-9.$/_]*");

    void setPackagesToDisplay(String value) {
        value = value.replace('/', '.').trim();
        if (value.length() == 0) {
            classSearchStrings = new String[0];
        } else {
            String[] parts = value.split("[ ,:]+");
            for (String p : parts) {
                if (!legalClassSearchString.matcher(p).matches()) {
                    throw new IllegalArgumentException("Classname filter must be legal Java identifier: " + p);
                }
            }

            classSearchStrings = parts;
        }
        FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);
    }

    public RankFilter getRank() {
        return rank;
    }

    public void setRank(RankFilter rank) {
        this.rank = rank;
        FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);

    }

    public PriorityFilter getPriority() {
        return priority;
    }

    public void setPriority(PriorityFilter priority) {
        this.priority = priority;
        FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);

    }

    public CloudFilter getEvaluation() {
        return eval;
    }

    public void setEvaluation(CloudFilter eval) {
        if (this.eval == eval) {
            return;
        }
        this.eval = eval;
        FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);

    }

    public void setClassification(OverallClassificationFilter classificationFilter) {
        if (this.classificationFilter == classificationFilter) {
            return;
        }
        this.classificationFilter = classificationFilter;
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
        return classSearchStrings;
    }

    public boolean showIgnoringPackagePrefixes(BugInstance b) {
        if (!firstSeen.show(mf, b)) {
            return false;
        }
        if (!rank.show(mf, b)) {
            return false;
        }
        if (!priority.show(mf, b)) {
            return false;
        }
        if (!eval.show(mf, b)) {
            return false;
        }
        if (!classificationFilter.show(mf, b)) {
            return false;
        }
        return true;
    }

    /**
     * @deprecated Use {@link ClassName#matchedPrefixes(String[],String)}
     *             instead
     */
    @Deprecated
    public static boolean matchedPrefixes(String[] classSearchStrings, @DottedClassName String className) {
        return ClassName.matchedPrefixes(classSearchStrings, className);
    }

    public boolean show(BugInstance b) {

        String className = b.getPrimaryClass().getClassName();

        return ClassName.matchedPrefixes(classSearchStrings, className) && showIgnoringPackagePrefixes(b);

    }

}
