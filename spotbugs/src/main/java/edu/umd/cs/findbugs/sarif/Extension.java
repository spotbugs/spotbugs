package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Objects;

class Extension {
    @NonNull
    final String version;
    @NonNull
    final String name;
    @Nullable
    final String shortDescription;
    @Nullable
    final String fullDescription;
    @Nullable
    final URI informationUri;
    @Nullable
    final String organization;

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
