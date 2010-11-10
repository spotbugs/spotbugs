package edu.umd.cs.findbugs.flybush.appengine;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import edu.umd.cs.findbugs.flybush.DbClientVersionStats;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class AppEngineDbClientVersionStats implements DbClientVersionStats {
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    @Persistent
    private String application;
    @Persistent
    private String version;
    @Persistent
    private long dayStart;
    @Persistent
    private int hits;

    public AppEngineDbClientVersionStats(String application, String version, long dayStart) {
        this.application = application;
        this.version = version;
        this.dayStart = dayStart;
        this.hits = 0;
    }

    @Override
    public String getApplication() {
        return application;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public long getDayStart() {
        return dayStart;
    }

    @Override
    public int getHits() {
        return hits;
    }

    @Override
    public void incrHits() {
        hits++;
    }

    @Override
    public int compareTo(DbClientVersionStats o) {
        return 0;
    }
}
