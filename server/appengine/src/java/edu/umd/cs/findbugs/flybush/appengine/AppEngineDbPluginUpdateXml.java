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
    private String user;

    @Persistent
    private Date date;

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

    public AppEngineDbPluginUpdateXml() {
    }

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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AppEngineDbPluginUpdateXml");
        sb.append("{user='").append(user).append('\'');
        sb.append(", date=").append(date);
        sb.append(", pluginId='").append(pluginId).append('\'');
        sb.append(", releaseDate=").append(releaseDate);
        sb.append(", version='").append(version).append('\'');
        sb.append(", channel='").append(channel).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
