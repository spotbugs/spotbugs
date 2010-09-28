package edu.umd.cs.findbugs.flybush.local;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import edu.umd.cs.findbugs.flybush.DbUser;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class LocalDbUser implements DbUser {
    @PrimaryKey
    @Persistent
    protected String openid;

    @Persistent
    protected String email;

    @Persistent
    protected String uploadToken;

    public LocalDbUser(String openid, String email) {
        this.openid = openid;
        this.email = email;
    }

    public LocalDbUser createKeyObject() {
        return this;
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

    public String getUploadToken() {
        return uploadToken;
    }

    public void setUploadToken(String token) {
        this.uploadToken = token;
    }
}