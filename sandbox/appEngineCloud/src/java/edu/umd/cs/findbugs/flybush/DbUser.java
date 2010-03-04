package edu.umd.cs.findbugs.flybush;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class DbUser implements Comparable<DbUser> {

    @PrimaryKey
    @Persistent
    private String openid;

    @Persistent
    private String email;

    public DbUser(String openid, String email) {
        this.openid = openid;
        this.email = email;
    }

    public int compareTo(DbUser o) {
        return openid.compareTo(o.openid);
    }

    public String getOpenid() {
        return openid;
    }

    public String getEmail() {
        return email;
    }

    public Key createKeyObject() {
        return KeyFactory.createKey(DbUser.class.getSimpleName(), getOpenid());
    }
}
