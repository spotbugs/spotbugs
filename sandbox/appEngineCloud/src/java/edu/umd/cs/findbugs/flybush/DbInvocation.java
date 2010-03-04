package edu.umd.cs.findbugs.flybush;

import com.google.appengine.api.datastore.Key;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class DbInvocation {
	@PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

	@Persistent private Key who;
	@Persistent private long startTime;
	@Persistent private long endTime;
	
	public Key getKey() {
		return key;
	}
	public Key getWho() {
		return who;
	}
	public void setWho(Key who) {
		this.who = who;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	@Override
	public String toString() {
		return "DbInvocation [endTime=" + endTime + ", key=" + key
				+ ", startTime=" + startTime + ", who=" + who + "]";
	}
}
