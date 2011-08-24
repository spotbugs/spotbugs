package edu.umd.cs.findbugs.flybush.local;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import edu.umd.cs.findbugs.flybush.DbClientVersionStats;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class LocalDbClientVersionStats implements DbClientVersionStats {
    @SuppressWarnings({"UnusedDeclaration"})
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long key;

    @Persistent
    private String application;
    @Persistent
    private String version;
    @Persistent
    private long dayStart;
    @Persistent
    private int hits;

    public LocalDbClientVersionStats(String application, String version, long dayStart) {
        this.application = application;
        this.version = version;
        this.dayStart = dayStart;
        this.hits = 0;
    }

    public String getApplication() {
        return application;
    }

    public String getVersion() {
        return version;
    }

    public long getDayStart() {
        return dayStart;
    }

    public int getHits() {
        return hits;
    }

    public void incrHits() {
        hits++;
    }

    public int compareTo(DbClientVersionStats o) {
        return 0;
    }
}