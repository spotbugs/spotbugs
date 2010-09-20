package edu.umd.cs.findbugs.flybush.local;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import edu.umd.cs.findbugs.flybush.DbInvocation;
import edu.umd.cs.findbugs.flybush.DbUser;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class LocalDbInvocation implements DbInvocation {
    @SuppressWarnings({"UnusedDeclaration"})
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long key;

    @Persistent private LocalDbUser who;
    @Persistent protected long startTime;
    @Persistent protected long endTime;

    public DbUser getWho() {
        return who;
    }

    public void setWho(Object user) {
        who = (LocalDbUser) user;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        return "DbInvocation [endTime=" + endTime + ", key=" + key
                + ", startTime=" + startTime + ", who=" + who + "]";
    }
}