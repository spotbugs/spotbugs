package edu.umd.cs.findbugs.flybush.appengine;

import java.util.Date;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import edu.umd.cs.findbugs.flybush.DbInvocation;
import edu.umd.cs.findbugs.flybush.SqlCloudSession;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class AppEngineSqlCloudSession implements SqlCloudSession {

    @Persistent
    @PrimaryKey
    private String randomID;

    @Persistent
    private Key user;

    @Persistent
    private String email;

    @Persistent
    private Date date;

    @Persistent
    private Key invocation;

    public AppEngineSqlCloudSession(Key author, String randomID, String email, Date date) {
        this.user = author;
        this.randomID = randomID;
        this.email = email;
        this.date = date;
    }

    public AppEngineSqlCloudSession(Key author, long randomID, String email, Date date) {
        this(author, Long.toString(randomID), email, date);
    }

    @Override
    public Key getUser() {
        return user;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public Key getInvocation() {
        return invocation;
    }

    @Override
    public void setInvocation(DbInvocation invocation) {
        this.invocation = ((AppEngineDbInvocation) invocation).getKey();
    }

    @Override
    public String getRandomID() {
        return randomID;
    }

    @Override
    public String toString() {
        return "SqlCloudSession [date=" + date + ", invocation=" + invocation + ", randomID=" + randomID + ", user=" + user + "]";
    }
}
