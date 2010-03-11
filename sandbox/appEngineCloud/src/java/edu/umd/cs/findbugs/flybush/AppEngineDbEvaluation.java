package edu.umd.cs.findbugs.flybush;

import com.google.appengine.api.datastore.Key;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class AppEngineDbEvaluation implements DbEvaluation {
	@SuppressWarnings({"UnusedDeclaration"})
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

	@Persistent private Key who;
    @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
    @Persistent private Key invocation;
    @Persistent private String designation;
    @Persistent private String comment;
    @Persistent private AppEngineDbIssue issue;
    @Persistent private long when;

    public Comparable<?> getWho() {
		return who;
	}

    public String getWhoId() {
        return who.getName();
    }

    public void setWho(Object user) {
        setWho((Key) user);
    }

    public void setInvocation(DbInvocation invocation) {
        this.invocation = ((AppEngineDbInvocation)invocation).getKey();
    }

    public Object getInvocationKey() {
        return invocation;
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
        this.issue = (AppEngineDbIssue) issue;
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
