package edu.umd.cs.findbugs.flybush.appengine;

import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import edu.umd.cs.findbugs.flybush.DbPluginUpdateXml;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class AppEngineDbPluginUpdateXml implements DbPluginUpdateXml {
    @SuppressWarnings({ "UnusedDeclaration" })
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    @Persistent
    private Text text;

    @Persistent
    private Date date;

    @Persistent
    private String user;

    public AppEngineDbPluginUpdateXml(String value) {
        setContents(value);
    }

    @Override
    public String getContents() {
        return text == null ? null : text.getValue();
    }

    @Override
    public void setContents(String contents) {
        text = contents == null ? null : new Text(contents);
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
