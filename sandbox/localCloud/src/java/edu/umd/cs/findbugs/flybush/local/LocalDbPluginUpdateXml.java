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

    @Persistent
    private String pluginId;

    @Persistent
    private Date releaseDate;

    @Persistent
    private String version;

    @Persistent
    private String channel;

    @Persistent
    private String message;

    @Persistent
    private String url;

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

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
