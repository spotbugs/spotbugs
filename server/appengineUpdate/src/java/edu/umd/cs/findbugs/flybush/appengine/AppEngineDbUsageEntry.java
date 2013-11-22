package edu.umd.cs.findbugs.flybush.appengine;

import java.util.Date;

import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

import edu.umd.cs.findbugs.flybush.DbUsageEntry;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class AppEngineDbUsageEntry implements DbUsageEntry {

    @SuppressWarnings({ "UnusedDeclaration" })
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Key key;

    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String ipAddress;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String country;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String version;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String appName;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String appVersion;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String entryPoint;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String os;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String javaVersion;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String language;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String localeCountry;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String uuid;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String plugin;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String pluginName;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String pluginVersion;
    
    @Extension(vendorName="datanucleus", key="gae.unindexed", value="true")
    @Persistent
    private String pluginChannel;
    
    @Persistent
    private Date date;

    @Override
    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String getCountry() {
        return country;
    }

    @Override
    public void setCountry(String country) {
        this.country = country;
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
    public String getAppName() {
        return appName;
    }

    @Override
    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Override
    public String getAppVersion() {
        return appVersion;
    }

    @Override
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    @Override
    public String getEntryPoint() {
        return entryPoint;
    }

    @Override
    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Override
    public String getOs() {
        return os;
    }

    @Override
    public void setOs(String os) {
        this.os = os;
    }

    @Override
    public String getJavaVersion() {
        return javaVersion;
    }

    @Override
    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }
    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String getLocaleCountry() {
        return localeCountry;
    }

    @Override
    public void setLocaleCountry(String localeCountry) {
        this.localeCountry = localeCountry;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getPlugin() {
        return plugin;
    }

    @Override
    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public void setPluginChannel(String channel) {
        this.pluginChannel = channel;
    }

    @Override
    public String getPluginChannel() {
        return pluginChannel;
    }

    @Override
    public String getPluginVersion() {
        return pluginVersion;
    }

    @Override
    public void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
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
    public DbUsageEntry copy() {
        AppEngineDbUsageEntry copy = new AppEngineDbUsageEntry();
        copy.setAppName(appName);
        copy.setAppVersion(appVersion);
        copy.setEntryPoint(entryPoint);
        copy.setJavaVersion(javaVersion);
        copy.setOs(os);
        copy.setPlugin(plugin);
        copy.setPluginName(pluginName);
        copy.setPluginVersion(pluginVersion);
        copy.setLanguage(language);
        copy.setLocaleCountry(localeCountry);
        copy.setUuid(uuid);
        copy.setVersion(version);
        copy.setDate(date);
        copy.setCountry(country);
        copy.setIpAddress(ipAddress);
        return copy;
    }

    @Override
    public String toString() {
        return "AppEngineDbUsageEntry{" +
                "uuid='" + uuid + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", country='" + country + '\'' +
                ", version='" + version + '\'' +
                ", appName='" + appName + '\'' +
                ", appVersion='" + appVersion + '\'' +
                ", entryPoint='" + entryPoint + '\'' +
                ", os='" + os + '\'' +
                ", javaVersion='" + javaVersion + '\'' +
                ", language='" + language + '\'' +
                ", localeCountry='" + localeCountry + '\'' +
                '}';
    }
}
