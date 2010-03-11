package edu.umd.cs.findbugs.flybush;

import java.util.Set;

public interface DbIssue {
    String getHash();

    void setHash(String hash);

    String getBugPattern();

    void setBugPattern(String bugPattern);

    int getPriority();

    void setPriority(int priority);

    String getPrimaryClass();

    void setPrimaryClass(String primaryClass);

    long getFirstSeen();

    void setFirstSeen(long firstSeen);

    long getLastSeen();

    void setLastSeen(long lastSeen);

    Set<? extends DbEvaluation> getEvaluations();

    void addEvaluation(DbEvaluation eval);

    void addEvaluations(DbEvaluation... evals);

    boolean hasEvaluations();

    void setHasEvaluations(boolean hasEvaluations);

    void setEvaluationsDontLook(Set<? extends DbEvaluation> evaluations);

    String getBugLink();

    void setBugLink(String bugLink);

    DbBugLinkType getBugLinkType();

    void setBugLinkType(DbBugLinkType bugLinkType);

    public static enum DbBugLinkType { GOOGLE_CODE, JIRA }
}
