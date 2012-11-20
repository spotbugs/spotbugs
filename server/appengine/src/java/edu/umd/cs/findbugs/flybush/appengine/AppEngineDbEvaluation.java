package edu.umd.cs.findbugs.flybush.appengine;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import edu.umd.cs.findbugs.flybush.DbEvaluation;
import edu.umd.cs.findbugs.flybush.DbInvocation;
import edu.umd.cs.findbugs.flybush.DbIssue;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import java.util.Set;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class AppEngineDbEvaluation implements DbEvaluation {
    @SuppressWarnings({ "UnusedDeclaration" })
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    @Persistent
    private Key who;

    @Persistent
    private String email;

    @SuppressWarnings({ "FieldCanBeLocal", "UnusedDeclaration" })
    @Persistent
    private Key invocation;

    @Persistent
    private String designation;

    @Persistent
    private String comment;

    @Persistent
    private Text commentLong;

    @Persistent
    private AppEngineDbIssue issue;

    @Persistent
    private long when;

    @Persistent
    private String primaryClass;

    @Persistent
    private Set<String> packages;

    @Override
    public Comparable<?> getWho() {
        return who;
    }

    @Override
    public String getWhoId() {
        return who.getName();
    }

    @Override
    public void setWho(Object user) {
        setWho((Key) user);
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
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
    public Set<String> getPackages() {
        return packages;
    }

    @Override
    public void setPackages(Set<String> packages) {
        this.packages = packages;
    }

    @Override
    public void setInvocation(DbInvocation invocation) {
        this.invocation = ((AppEngineDbInvocation) invocation).getKey();
    }

    @Override
    public Object getInvocationKey() {
        return invocation;
    }

    public void setWho(Key who) {
        this.who = who;
    }

    @Override
    public String getDesignation() {
        return designation;
    }

    @Override
    public void setDesignation(String designation) {
        this.designation = designation;
    }

    @Override
    public String getComment() {
        return commentLong == null ? comment : commentLong.getValue();
    }

    @Override
    public void setComment(String comment) {
        this.commentLong = new Text(comment);
    }

    @Override
    public DbIssue getIssue() {
        return issue;
    }

    @Override
    public void setIssue(DbIssue issue) {
        this.issue = (AppEngineDbIssue) issue;
    }

    @Override
    public long getWhen() {
        return when;
    }

    @Override
    public void setWhen(long when) {
        this.when = when;
    }

    @Override
    public int compareTo(DbEvaluation o) {
        if (getWhen() < o.getWhen())
            return -1;
        if (getWhen() > o.getWhen())
            return 1;

        @SuppressWarnings({ "RedundantCast" })
        int whoCompare = comparePossiblyNull((Comparable) getWho(), (Comparable) o.getWho());
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

    public Text getLongComment() {
        return commentLong;
    }

    public void setShortComment(String comment) {
        this.comment = comment;
    }

    public String getShortComment() {
        return comment;
    }

    public void setLongComment(Text comment) {
        this.commentLong = comment;
    }

    public boolean convertToNewCommentStyle() {
        if (commentLong != null && comment == null) {
            // already converted
            return false;
        }
        if (comment != null) {
            if (commentLong == null) {
                commentLong = new Text(comment);
            }
            this.comment = null;
            return true;
        }
        return false;
    }
}
