package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.json.JSONObject;

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
    final Object organization;

    Extension(@NonNull String version, @NonNull String name, @Nullable String shortDescription, @Nullable String fullDescription,
            @Nullable URI informationUri, @Nullable String organization) {
        this.version = Objects.requireNonNull(version);
        this.name = Objects.requireNonNull(name);
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
        this.informationUri = informationUri;
        this.organization = organization;
    }

    JSONObject toJSONObject() {
        return new JSONObject().put("version", version).put("name", name).putOpt("shortDescription", shortDescription)
                .putOpt("fullDescription", fullDescription).putOpt("informationUri", informationUri)
                .putOpt("organization", organization);
    }

    static Extension fromPlugin(@NonNull Plugin plugin) {
        Objects.requireNonNull(plugin);
        return new Extension(plugin.getVersion(), plugin.getPluginId(), plugin.getShortDescription(), plugin.getDetailedDescription(), plugin
                .getWebsiteURI(), plugin.getProvider());
    }
}
