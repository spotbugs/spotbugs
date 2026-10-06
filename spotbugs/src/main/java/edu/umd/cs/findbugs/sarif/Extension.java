package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.Plugin;

import com.google.gson.JsonObject;

import java.net.URI;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

class Extension {

    final @NonNull String version;

    final @NonNull String name;

    final @Nullable String shortDescription;

    final @Nullable String fullDescription;

    final @Nullable URI informationUri;

    final @Nullable String organization;

    Extension(@NonNull String version, @NonNull String name, @Nullable String shortDescription, @Nullable String fullDescription,
            @Nullable URI informationUri, @Nullable String organization) {
        this.version = Objects.requireNonNull(version);
        this.name = Objects.requireNonNull(name);
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
        this.informationUri = informationUri;
        this.organization = organization;
    }

    JsonObject toJsonObject() {
        // TODO put 'fullDescription' with both of text and markdown representations
        JsonObject desc = null;
        if (shortDescription != null) {
            desc = new JsonObject();
            desc.addProperty("text", shortDescription);
        }
        JsonObject extensionJson = new JsonObject();
        extensionJson.addProperty("version", version);
        extensionJson.addProperty("name", name);
        if (desc != null) {
            extensionJson.add("shortDescription", desc);
        }
        if (informationUri != null) {
            extensionJson.addProperty("informationUri", informationUri.toString());
        }
        if (!StringUtils.isEmpty(organization)) {
            extensionJson.addProperty("organization", organization);
        }
        return extensionJson;
    }

    static Extension fromPlugin(@NonNull Plugin plugin) {
        Objects.requireNonNull(plugin);
        return new Extension(plugin.getVersion(), plugin.getPluginId(), plugin.getShortDescription(), plugin.getDetailedDescription(), plugin
                .getWebsiteURI(), plugin.getProvider());
    }
}
