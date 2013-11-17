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

    @Persistent Integer javaVersion;
    
    @Persistent
    private String channel;

    @Persistent
    private String message;

    @Persistent
    private String url;

    public AppEngineDbPluginUpdateXml() {
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    @Override
    public Date getReleaseDate() {
        return releaseDate;
    }

    @Override
    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public void setChannel(String channel) {
        this.channel = channel;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
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

    @Override
    public int getJavaVersion() {
        if (javaVersion == null)
            return 5;
        return javaVersion;
    }

    @Override
    public void setJavaVersion(int version) {
        this.javaVersion = version;
        
    }
}
