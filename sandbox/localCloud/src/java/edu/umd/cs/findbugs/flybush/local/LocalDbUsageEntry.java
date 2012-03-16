package edu.umd.cs.findbugs.flybush.local;

import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import edu.umd.cs.findbugs.flybush.DbUsageEntry;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class LocalDbUsageEntry implements DbUsageEntry {
    @SuppressWarnings({"UnusedDeclaration"})
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private Long key;

    @Persistent
    private String ipAddress;
    @Persistent
    private String country;
    @Persistent
    private String version;
    @Persistent
    private String appName;
    @Persistent
    private String appVersion;
    @Persistent
    private String entryPoint;
    @Persistent
    private String os;
    @Persistent
    private String javaVersion;
    @Persistent
    private String language;
    @Persistent
    private String localeCountry;
    @Persistent
    private String uuid;
    @Persistent
    private String plugin;
    @Persistent
    private String pluginName;
    @Persistent
    private String pluginVersion;
    @Persistent
    private String pluginChannel;
    @Persistent
    private Date date;

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLocaleCountry() {
        return localeCountry;
    }

    public void setLocaleCountry(String localeCountry) {
        this.localeCountry = localeCountry;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public void setPluginChannel(String channel) {
        this.pluginChannel = channel;
    }

    public String getPluginChannel() {
        return pluginChannel;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public DbUsageEntry copy() {
        LocalDbUsageEntry copy = new LocalDbUsageEntry();
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