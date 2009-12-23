package edu.umd.cs.findbugs.flybush;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class DbEvaluation {
	@PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

	@Persistent private String who;
	@Persistent private String designation;
	@Persistent private String comment;
	@Persistent private DbIssue issue;
	@Persistent private long when;
	@Persistent private Invocation invocation;

	public Key getKey() {
		return key;
	}
	public void setKey(Key key) {
		this.key = key;
	}
	public String getWho() {
		return who;
	}
	public void setWho(String who) {
		this.who = who;
	}
	public String getDesignation() {
		return designation;
	}
	public void setDesignation(String designation) {
		this.designation = designation;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public DbIssue getIssue() {
		return issue;
	}
	public void setIssue(DbIssue issue) {
		this.issue = issue;
	}
	public long getWhen() {
		return when;
	}
	public void setWhen(long when) {
		this.when = when;
	}
	public Invocation getInvocation() {
		return invocation;
	}
	public void setInvocation(Invocation invocation) {
		this.invocation = invocation;
	}

}
