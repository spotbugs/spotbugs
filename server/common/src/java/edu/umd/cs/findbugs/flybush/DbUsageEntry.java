package edu.umd.cs.findbugs.flybush;

import java.util.Date;

public interface DbUsageEntry {
    String getIpAddress();
    void setIpAddress(String ipAddress);
    String getCountry();
    void setCountry(String country);
    String getVersion();
    void setVersion(String version);
    String getAppName();
    void setAppName(String appName);
    String getAppVersion();
    void setAppVersion(String appVersion);
    String getEntryPoint();
    void setEntryPoint(String entryPoint);
    String getOs();
    void setOs(String os);
    String getJavaVersion();
    void setJavaVersion(String javaVersion);
    String getUuid();
    void setUuid(String uuid);
    String getPlugin();
    void setPlugin(String plugin);
    String getPluginName();
    void setPluginName(String pluginName);
    String getPluginVersion();
    void setPluginVersion(String pluginVersion);
    Date getDate();
    void setDate(Date date);
    String getLanguage();
    void setLanguage(String language);
    String getLocaleCountry();
    void setLocaleCountry(String localeCountry);

    DbUsageEntry copy();
}
