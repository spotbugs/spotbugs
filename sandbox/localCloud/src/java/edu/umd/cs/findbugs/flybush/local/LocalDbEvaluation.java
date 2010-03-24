package edu.umd.cs.findbugs.flybush.local;

import edu.umd.cs.findbugs.flybush.DbEvaluation;
import edu.umd.cs.findbugs.flybush.DbInvocation;
import edu.umd.cs.findbugs.flybush.DbIssue;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class LocalDbEvaluation implements DbEvaluation {
	@SuppressWarnings({"UnusedDeclaration"})
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long key;

	@Persistent private LocalDbUser who;
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    @Persistent private LocalDbInvocation invocation;
    @Persistent private String designation;
    @Persistent(columns = @Column(sqlType = "longvarchar")) private String comment;
    @Persistent private LocalDbIssue issue;
    @Persistent private long when;

    public Comparable<?> getWho() {
		return who;
	}

    public String getWhoId() {
        return who.getOpenid();
    }

    public void setWho(Object user) {
        who = (LocalDbUser) user;
    }

    public void setInvocation(DbInvocation invocation) {
        this.invocation = ((LocalDbInvocation)invocation);
    }

    public Object getInvocationKey() {
        return invocation;
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
        this.issue = (LocalDbIssue) issue;
    }

    public long getWhen() {
        return when;
    }

    public void setWhen(long when) {
        this.when = when;
    }

    public int compareTo(DbEvaluation o) {
        if (getWhen() < o.getWhen()) return -1;
        if (getWhen() > o.getWhen()) return 1;

        @SuppressWarnings({"RedundantCast"})
        int whoCompare = comparePossiblyNull((Comparable) getWho(),
                                             (Comparable) o.getWho());
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