package edu.umd.cs.findbugs.cloud;

import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.cloud.username.NameLookup;

public class CloudPluginBuilder {
    private String cloudid;

    private ClassLoader classLoader;

    private Class<? extends Cloud> cloudClass;

    private Class<? extends NameLookup> usernameClass;

    private PropertyBundle properties;

    private String description;

    private String details;

    private boolean hidden;

    private boolean onlineStorage;

    private String findbugsPluginId;

    public CloudPluginBuilder setCloudid(String cloudid) {
        this.cloudid = cloudid;
        return this;
    }

    public CloudPluginBuilder setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public CloudPluginBuilder setCloudClass(Class<? extends Cloud> cloudClass) {
        this.cloudClass = cloudClass;
        return this;
    }

    public CloudPluginBuilder setUsernameClass(Class<? extends NameLookup> usernameClass) {
        this.usernameClass = usernameClass;
        return this;
    }

    public CloudPluginBuilder setProperties(PropertyBundle properties) {
        this.properties = properties;
        return this;
    }

    public CloudPluginBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public CloudPluginBuilder setDetails(String details) {
        this.details = details;
        return this;
    }

    public CloudPluginBuilder setHidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public CloudPlugin createCloudPlugin() {
        return new CloudPlugin(findbugsPluginId, cloudid, classLoader, cloudClass, usernameClass, hidden, properties, description, details);
    }

    public CloudPluginBuilder setOnlineStorage(boolean onlineStorage) {
        this.onlineStorage = onlineStorage;
        return this;
    }

    public boolean isOnlineStorage() {
        return onlineStorage;
    }

    public CloudPluginBuilder setFindbugsPluginId(String findbugsPluginId) {
        this.findbugsPluginId = findbugsPluginId;
        return this;
    }

    public String getFindbugsPluginId() {
        return findbugsPluginId;
    }
}
