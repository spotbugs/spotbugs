package edu.umd.cs.findbugs.flybush;

public interface DbUsageEntry {
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

    DbUsageEntry copy();
}
