/*
 * Contributions to SpotBugs
 * Copyright (C) 2023, user
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
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
