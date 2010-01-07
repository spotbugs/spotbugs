package edu.umd.cs.findbugs.flybush;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class DbIssue {
	@PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

	@Persistent @Unique private String hash;
    @Persistent private String bugPattern;
    @Persistent private int priority;
    @Persistent private String primaryClass;
    @Persistent private long firstSeen;
    @Persistent private long lastSeen;
    @Persistent(mappedBy = "issue") private List<DbEvaluation> evaluations;

	public Key getKey() {
		return key;
	}
	public void setKey(Key key) {
		this.key = key;
	}
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
	public List<DbEvaluation> getEvaluations() {
		return evaluations;
	}
	public void setEvaluations(List<DbEvaluation> evaluations) {
		this.evaluations = evaluations;
	}
	public void addEvaluation(DbEvaluation eval) {
		if (evaluations == null) {
			evaluations = new ArrayList<DbEvaluation>();
		}
		evaluations.add(eval);
	}
	public void addEvaluations(DbEvaluation... evals) {
		for (DbEvaluation eval : evals) {
			addEvaluation(eval);
		}
	}

}
