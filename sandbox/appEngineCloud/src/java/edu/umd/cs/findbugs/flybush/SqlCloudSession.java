package edu.umd.cs.findbugs.flybush;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;

import java.util.Date;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class SqlCloudSession {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    @Persistent
    private User user;

    @Persistent
    private long randomID;

    @Persistent
    private Date date;

    @Persistent
    private Key invocation;

    public SqlCloudSession(User author, long randomID, Date date) {
        this.user = author;
        this.randomID = randomID;
        this.date = date;
    }

    public Key getKey() {
        return key;
    }

    public long getRandomID() {
        return randomID;
    }

    public Date getDate() {
        return date;
    }

    public void setRandomID(long randomID) {
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

	public void setKey(Key key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return "SqlCloudSession [date=" + date + ", invocation=" + invocation
				+ ", key=" + key + ", randomID=" + randomID + ", user=" + user
				+ "]";
	}
}
