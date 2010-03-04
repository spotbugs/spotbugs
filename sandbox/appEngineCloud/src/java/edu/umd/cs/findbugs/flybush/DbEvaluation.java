package edu.umd.cs.findbugs.flybush;

import com.google.appengine.api.datastore.Key;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class DbEvaluation implements Comparable<DbEvaluation> {
	@SuppressWarnings({"UnusedDeclaration"})
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

	@Persistent private Key who;
	@Persistent private String designation;
	@Persistent private String comment;
	@Persistent private DbIssue issue;
	@Persistent private long when;
	@Persistent private Key invocation;

	public Key getWho() {
		return who;
	}
	public void setWho(Key who) {
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
	public Key getInvocation() {
		return invocation;
	}
	public void setInvocation(Key invocation) {
		this.invocation = invocation;
	}

	public int compareTo(DbEvaluation o) {
		if (getWhen() < o.getWhen()) return -1;
		if (getWhen() > o.getWhen()) return 1;

		int whoCompare = comparePossiblyNull(getWho(), o.getWho());
		if (whoCompare != 0)
			return whoCompare;

		int commentCompare = comparePossiblyNull(getComment(), o.getComment());
		if (commentCompare != 0)
			return commentCompare;

		return comparePossiblyNull(getDesignation(), o.getDesignation());
	}

	private <E extends Comparable<? super E>> int comparePossiblyNull(E a, E b) {
		return a == null ? -1 : (b == null ? 1 : a.compareTo(b));
	}

}
