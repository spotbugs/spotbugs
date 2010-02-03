package edu.umd.cs.findbugs.flybush;

import java.util.SortedSet;
import java.util.TreeSet;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class DbIssue {
	@Persistent @PrimaryKey private String hash;

    @Persistent private String bugPattern;
    @Persistent private int priority;
    @Persistent private String primaryClass;
    @Persistent private long firstSeen;
    @Persistent private long lastSeen;
    @Persistent(mappedBy = "issue") private SortedSet<DbEvaluation> evaluations;

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
	public SortedSet<DbEvaluation> getEvaluations() {
		return evaluations;
	}
	public void setEvaluations(SortedSet<DbEvaluation> evaluations) {
		this.evaluations = evaluations;
	}
	public void addEvaluation(DbEvaluation eval) {
		if (evaluations == null) {
			evaluations = new TreeSet<DbEvaluation>();
		}
		evaluations.add(eval);
	}
	public void addEvaluations(DbEvaluation... evals) {
		for (DbEvaluation eval : evals) {
			addEvaluation(eval);
		}
	}

}
