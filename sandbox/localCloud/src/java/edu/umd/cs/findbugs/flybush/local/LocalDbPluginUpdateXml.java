package edu.umd.cs.findbugs.flybush.local;

import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import edu.umd.cs.findbugs.flybush.DbPluginUpdateXml;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class LocalDbPluginUpdateXml implements DbPluginUpdateXml {
    @SuppressWarnings({"UnusedDeclaration"})
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long key;

    @Persistent(columns = @Column(sqlType = "longvarchar"))
    private String contents;

    @Persistent
    private Date date;

    @Persistent
    private String email;

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public void setUser(String email) {
        this.email = email;
    }

    public String getUser() {
        return email;
    }
}
