package edu.umd.cs.findbugs.flybush.appengine;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import edu.umd.cs.findbugs.flybush.DbUser;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class AppEngineDbUser implements DbUser {
    @PrimaryKey
    @Persistent
    protected String openid;

    @Persistent
    protected String email;

    public AppEngineDbUser(String openid, String email) {
        this.openid = openid;
        this.email = email;
    }

    public Key createKeyObject() {
        return KeyFactory.createKey(AppEngineDbUser.class.getSimpleName(), getOpenid());
    }

    public int compareTo(DbUser o) {
        return getOpenid().compareTo(o.getOpenid());
    }

    public String getOpenid() {
        return openid;
    }

    public String getEmail() {
        return email;
    }
}
