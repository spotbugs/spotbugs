package edu.umd.cs.findbugs.flybush.local;

import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import edu.umd.cs.findbugs.flybush.DbEvaluation;
import edu.umd.cs.findbugs.flybush.DbIssue;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class LocalDbIssue implements DbIssue {
    @Persistent @PrimaryKey
    private String hash;

    @Persistent private String bugPattern;
    @Persistent private int priority;
    @Persistent private String primaryClass;
    @Persistent private long firstSeen;
    @Persistent private long lastSeen;
    @Persistent private boolean hasEvaluations = false;
    @Persistent private String bugLink;
    @Persistent private String bugLinkType;
    @Persistent(mappedBy = "issue") @Element(dependent="true") private Set<LocalDbEvaluation> evaluations;

    public String getHash() {
        return hash;
    }
    public void setHash(String hash) {
        this.hash = hash;
    }
    public String getBugPattern() {
        return bugPattern;
    }
    public void setBugPattern(String bugPattern) {
        this.bugPattern = bugPattern;
    }
    public int getPriority() {
        return priority;
    }
    public void setPriority(int priority) {
        this.priority = priority;
    }
    public String getPrimaryClass() {
        return primaryClass;
    }
    public void setPrimaryClass(String primaryClass) {
        this.primaryClass = primaryClass;
    }
    public long getFirstSeen() {
        return firstSeen;
    }
    public void setFirstSeen(long firstSeen) {
        this.firstSeen = firstSeen;
    }
    public long getLastSeen() {
        return lastSeen;
    }
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    public Set<LocalDbEvaluation> getEvaluations() {
        return evaluations;
    }

    public void addEvaluation(DbEvaluation eval) {
        if (evaluations == null) {
            evaluations = new HashSet<LocalDbEvaluation>();
        }
        evaluations.add((LocalDbEvaluation) eval);
        updateHasEvaluations();
    }
    public void addEvaluations(DbEvaluation... evals) {
        for (DbEvaluation eval : evals) {
            addEvaluation(eval);
        }
    }

    public boolean hasEvaluations() {
        return hasEvaluations;
    }

    private void updateHasEvaluations() {
        hasEvaluations = this.evaluations != null && !this.evaluations.isEmpty();
    }

    public void setHasEvaluations(boolean hasEvaluations) {
        this.hasEvaluations = hasEvaluations;
    }

    /** Does not access the given list, only stores it. Good for lazy loaded evaluations. */
    @SuppressWarnings({"unchecked"})
    public void setEvaluationsDontLook(Set<? extends DbEvaluation> evaluations) {
        this.evaluations = (Set<LocalDbEvaluation>) evaluations;
    }

    public String getBugLink() {
        return bugLink;
    }

    public void setBugLink(String bugLink) {
        this.bugLink = bugLink;
    }

    public String getBugLinkType() {
        return bugLinkType;
    }

    public void setBugLinkType(String bugLinkType) {
        this.bugLinkType = bugLinkType;
    }

    @Override
    public String toString() {
        return "DbIssue{" +
               "hash='" + hash + '\'' +
               ", bugPattern='" + bugPattern + '\'' +
               ", evaluations=" + evaluations +
               '}';
    }

}