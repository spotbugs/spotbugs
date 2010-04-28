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
    boolean hasEvaluations();
    void setHasEvaluations(boolean hasEvaluations);

    Set<? extends DbEvaluation> getEvaluations();
    void addEvaluation(DbEvaluation eval);
    void addEvaluations(DbEvaluation... evals);
    void setEvaluationsDontLook(Set<? extends DbEvaluation> evaluations);

    String getBugLink();
    void setBugLink(String bugLink);

    String getBugLinkType();
    void setBugLinkType(String bugLinkType);
}
