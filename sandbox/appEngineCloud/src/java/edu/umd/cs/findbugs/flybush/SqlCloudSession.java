package edu.umd.cs.findbugs.flybush;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import java.util.Date;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class SqlCloudSession {
    @Persistent
    @PrimaryKey
    private String randomID;

    @Persistent
    private User user;

    @Persistent
    private Date date;

    @Persistent
    private Key invocation;

    public SqlCloudSession(User author, String randomID, Date date) {
        this.user = author;
        this.randomID = randomID;
        this.date = date;
    }

    public SqlCloudSession(User author, long randomID, Date date) {
        this(author, Long.toString(randomID), date);
    }

    public String getRandomID() {
        return randomID;
    }

    public Date getDate() {
        return date;
    }

    public void setRandomID(String randomID) {
		this.randomID = randomID;
	}

    public void setDate(Date date) {
        this.date = date;
    }

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Key getInvocation() {
		return invocation;
	}

	public void setInvocation(Key invocation) {
		this.invocation = invocation;
	}

	@Override
	public String toString() {
		return "SqlCloudSession [date=" + date + ", invocation=" + invocation
				+ ", randomID=" + randomID + ", user=" + user
				+ "]";
	}
}
