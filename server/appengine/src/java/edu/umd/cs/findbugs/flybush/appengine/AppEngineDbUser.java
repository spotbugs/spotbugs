package edu.umd.cs.findbugs.flybush.appengine;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import edu.umd.cs.findbugs.flybush.DbUser;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class AppEngineDbUser implements DbUser {
    @PrimaryKey
    @Persistent
    protected String openid;

    @Persistent
    protected String email;

    @Persistent
    protected String uploadToken;

    public AppEngineDbUser(String openid, String email) {
        this.openid = openid;
        this.email = email;
    }

    @Override
    public Key createKeyObject() {
        return KeyFactory.createKey(AppEngineDbUser.class.getSimpleName(), getOpenid());
    }

    @Override
    public int compareTo(DbUser o) {
        return getOpenid().compareTo(o.getOpenid());
    }

    @Override
    public String getOpenid() {
        return openid;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getUploadToken() {
        return uploadToken;
    }

    @Override
    public void setUploadToken(String token) {
        this.uploadToken = token;
    }
}
