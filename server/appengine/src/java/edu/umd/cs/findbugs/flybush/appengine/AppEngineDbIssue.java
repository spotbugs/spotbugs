package edu.umd.cs.findbugs.flybush.appengine;

import edu.umd.cs.findbugs.flybush.DbEvaluation;
import edu.umd.cs.findbugs.flybush.DbIssue;

import javax.jdo.annotations.Element;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import java.util.HashSet;
import java.util.Set;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class AppEngineDbIssue implements DbIssue {
    @Persistent
    @PrimaryKey
    private String hash;

    @Persistent
    private String bugPattern;

    @Persistent
    private int priority;

    @Persistent
    private String primaryClass;

    @Persistent
    private long firstSeen;

    @Persistent
    private long lastSeen;

    @Persistent
    private boolean hasEvaluations = false;

    @Persistent
    private String bugLink;

    @Persistent
    private String bugLinkType;

    @Persistent(mappedBy = "issue")
    @Element(dependent = "true")
    private Set<AppEngineDbEvaluation> evaluations;

    @Override
    public String getHash() {
        return hash;
    }

    @Override
    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String getBugPattern() {
        return bugPattern;
    }

    @Override
    public void setBugPattern(String bugPattern) {
        this.bugPattern = bugPattern;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String getPrimaryClass() {
        return primaryClass;
    }

    @Override
    public void setPrimaryClass(String primaryClass) {
        this.primaryClass = primaryClass;
    }

    @Override
    public long getFirstSeen() {
        return firstSeen;
    }

    @Override
    public void setFirstSeen(long firstSeen) {
        this.firstSeen = firstSeen;
    }

    @Override
    public long getLastSeen() {
        return lastSeen;
    }

    @Override
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    @Override
    public Set<AppEngineDbEvaluation> getEvaluations() {
        return evaluations;
    }

    @Override
    public void addEvaluation(DbEvaluation eval) {
        if (evaluations == null) {
            evaluations = new HashSet<AppEngineDbEvaluation>();
        }
        evaluations.add((AppEngineDbEvaluation) eval);
        updateHasEvaluations();
    }

    @Override
    public void addEvaluations(DbEvaluation... evals) {
        for (DbEvaluation eval : evals) {
            addEvaluation(eval);
        }
    }

    @Override
    public boolean hasEvaluations() {
        return hasEvaluations;
    }

    private void updateHasEvaluations() {
        hasEvaluations = this.evaluations != null && !this.evaluations.isEmpty();
    }

    @Override
    public void setHasEvaluations(boolean hasEvaluations) {
        this.hasEvaluations = hasEvaluations;
    }

    /**
     * Does not access the given list, only stores it. Good for lazy loaded
     * evaluations.
     */
    @Override
    @SuppressWarnings({ "unchecked" })
    public void setEvaluationsDontLook(Set<? extends DbEvaluation> evaluations) {
        this.evaluations = (Set<AppEngineDbEvaluation>) evaluations;
    }

    @Override
    public String getBugLink() {
        return bugLink;
    }

    @Override
    public void setBugLink(String bugLink) {
        this.bugLink = bugLink;
    }

    @Override
    public String getBugLinkType() {
        return bugLinkType;
    }

    @Override
    public void setBugLinkType(String bugLinkType) {
        this.bugLinkType = bugLinkType;
    }

    @Override
    public String toString() {
        return "DbIssue{" + "hash='" + hash + '\'' + ", bugPattern='" + bugPattern + '\'' + ", evaluations=" + evaluations + '}';
    }

}
