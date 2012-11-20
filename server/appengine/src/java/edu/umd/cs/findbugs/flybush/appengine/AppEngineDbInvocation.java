package edu.umd.cs.findbugs.flybush.appengine;

import com.google.appengine.api.datastore.Key;
import edu.umd.cs.findbugs.flybush.DbInvocation;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class AppEngineDbInvocation implements DbInvocation {
    @SuppressWarnings({ "UnusedDeclaration" })
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    @Persistent
    private Key who;

    @Persistent
    protected long startTime;

    @Persistent
    protected long endTime;

    public Key getKey() {
        return key;
    }

    @Override
    public Key getWho() {
        return who;
    }

    @Override
    public void setWho(Object user) {
        setWho((Key) user);
    }

    public void setWho(Key who) {
        this.who = who;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        return "DbInvocation [endTime=" + endTime + ", key=" + key + ", startTime=" + startTime + ", who=" + who + "]";
    }
}
