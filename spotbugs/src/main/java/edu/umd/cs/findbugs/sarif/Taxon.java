package edu.umd.cs.findbugs.sarif;

import java.util.UUID;

import com.google.gson.JsonObject;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A taxon SARIF element (see: §3.19.3).
 *
 * @author Jeremias Eppler
 */
public class Taxon implements Comparable<Taxon> {
    private final String id;
    private final UUID guid;
    private final String shortDescription;
    private final String fullDescription;
    private final Level severityLevel;

    private Taxon(@NonNull String id, @NonNull UUID guid,
            @NonNull String shortDescription, @NonNull String fullDescription, @NonNull Level severityLevel) {
        this.id = id;
        this.guid = guid;
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
        this.severityLevel = severityLevel;
    }

    /**
     * Create a new taxon element (see: §3.19.3).
     *
     * @param id
     * @param guid
     * @param shortDescription
     * @param fullDescription
     * @param severityLevel
     * @return a new taxon
     */
    public static Taxon from(@NonNull String id, @NonNull UUID guid,
            @NonNull String shortDescription, @NonNull String fullDescription, @NonNull Level severityLevel) {
        return new Taxon(id, guid, shortDescription, fullDescription, severityLevel);
    }

    JsonObject toJsonObject() {
        JsonObject taxonJson = new JsonObject();

        JsonObject shortDescriptionJson = new JsonObject();
        shortDescriptionJson.addProperty("text", shortDescription);

        JsonObject fullDescriptionJson = new JsonObject();
        fullDescriptionJson.addProperty("text", fullDescription);

        JsonObject defaultConfigurationJson = new JsonObject();
        defaultConfigurationJson.addProperty("level", severityLevel.toJsonString());

        taxonJson.addProperty("id", id);
        taxonJson.addProperty("guid", guid.toString());
        taxonJson.add("shortDescription", shortDescriptionJson);
        taxonJson.add("fullDescription", fullDescriptionJson);
        taxonJson.add("defaultConfiguration", defaultConfigurationJson);

        return taxonJson;
    }

    @Override
    public int compareTo(Taxon other) {
        return id.compareTo(other.id);
    }
}
