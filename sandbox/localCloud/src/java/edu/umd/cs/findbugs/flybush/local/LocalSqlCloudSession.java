package edu.umd.cs.findbugs.flybush.local;

import java.util.Date;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import edu.umd.cs.findbugs.flybush.DbInvocation;
import edu.umd.cs.findbugs.flybush.SqlCloudSession;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class LocalSqlCloudSession implements SqlCloudSession {

    @Persistent
    @PrimaryKey
    private String randomID;

    @Persistent private LocalDbUser user;
    @Persistent private String email;
    @Persistent private Date date;
    @Persistent private LocalDbInvocation invocation;

    public LocalSqlCloudSession(LocalDbUser author, String email, String randomID, Date date) {
        this.user = author;
        this.email = email;
        this.randomID = randomID;
        this.date = date;
    }

    public LocalSqlCloudSession(LocalDbUser author, String email, long randomID, Date date) {
        this(author, email, Long.toString(randomID), date);
    }

    public LocalDbUser getUser() {
        return user;
    }

    public String getEmail() {
        return email;
    }

    public LocalDbInvocation getInvocation() {
        return invocation;
    }

    public void setInvocation(DbInvocation invocation) {
        this.invocation = (LocalDbInvocation) invocation;
    }

    public String getRandomID() {
        return randomID;
    }

    @Override
    public String toString() {
        return "SqlCloudSession [date=" + date + ", invocation=" + invocation
                + ", randomID=" + randomID + ", user=" + user
                + "]";
    }
}