package edu.umd.cs.findbugs.sarif;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.cwe.WeaknessCatalog;

/**
 * Object which represents reportingDescriptor in {@code run.driver.rules} property. (§3.19.23)
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317836">3.49 reportingDescriptor object</a>
 */
final class Rule {
    @NonNull
    final String id;
    @NonNull
    final String shortDescription;
    @NonNull
    final String fullDescription;
    @NonNull
    final String defaultText;
    @Nullable
    final URI helpUri;
    @NonNull
    final List<String> tags;
    @NonNull
    final int cweid;

    Rule(@NonNull String id, @NonNull String shortDescription, @NonNull String fullDescription, @NonNull String defaultText, @Nullable URI helpUri,
            @NonNull List<String> tags, @NonNull int cweid) {
        this.id = Objects.requireNonNull(id);
        this.shortDescription = Objects.requireNonNull(shortDescription);
        this.fullDescription = Objects.requireNonNull(fullDescription);
        this.defaultText = Objects.requireNonNull(defaultText);
        this.helpUri = helpUri;
        this.tags = Collections.unmodifiableList(tags);
        this.cweid = cweid;
    }

    JsonObject toJsonObject() {
        String textEndsWithPeriod = defaultText.endsWith(".") ? defaultText : defaultText + ".";
        String shortDescriptionEndsWithPeriod = shortDescription.endsWith(".") ? shortDescription : shortDescription + ".";
        JsonObject textJson = new JsonObject();
        textJson.addProperty("text", textEndsWithPeriod);
        JsonObject shortDescJson = new JsonObject();
        shortDescJson.addProperty("text", shortDescriptionEndsWithPeriod);
        JsonObject messageStrings = new JsonObject();
        messageStrings.add("default", textJson);

        // TODO put 'fullDescription' with both of text and markdown representations
        JsonObject result = new JsonObject();
        result.addProperty("id", id);
        result.add("shortDescription", shortDescJson);
        result.add("messageStrings", messageStrings);
        if (helpUri != null) {
            result.addProperty("helpUri", helpUri.toString());
        }
        if (!tags.isEmpty()) {
            JsonArray propertyArray = new JsonArray();
            tags.forEach((prop) -> propertyArray.add(prop));
            JsonObject propertyBag = new JsonObject();
            propertyBag.add("tags", propertyArray);
            result.add("properties", propertyBag);
        }

        if (cweid > 0) {
            JsonArray relationships = new JsonArray();
            relationships.add(createCweRelationship());
            result.add("relationships", relationships);
        }

        return result;
    }

    private JsonObject createCweRelationship() {
        WeaknessCatalog weaknessCatalog = WeaknessCatalog.getInstance();

        String cweId = String.valueOf(cweid);
        String name = weaknessCatalog.getName();
        String version = weaknessCatalog.getVersion();
        UUID guidOfCweTaxonomy = GUIDCalculator.fromString(name + version);
        UUID guidOfCweTaxon = GUIDCalculator.fromNamespaceAndString(guidOfCweTaxonomy, cweId.toString());

        JsonObject cweRelationship = new JsonObject();

        // see: https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317872
        JsonObject target = new JsonObject();
        target.addProperty("id", cweId);
        target.addProperty("guid", guidOfCweTaxon.toString());

        JsonObject toolComponent = new JsonObject();
        toolComponent.addProperty("name", name);
        toolComponent.addProperty("guid", guidOfCweTaxonomy.toString());
        target.add("toolComponent", toolComponent);

        JsonArray kinds = new JsonArray();
        kinds.add("superset");

        cweRelationship.add("target", target);
        cweRelationship.add("kinds", kinds);

        return cweRelationship;
    }

    @NonNull
    static Rule fromBugPattern(BugPattern bugPattern, String formattedMessage) {
        URI helpUri = bugPattern.getUri().orElse(null);

        String category = bugPattern.getCategory();
        List<String> tags;
        if (StringUtils.isEmpty(category)) {
            tags = Collections.emptyList();
        } else {
            tags = Collections.singletonList(category);
        }

        return new Rule(bugPattern.getType(), bugPattern.getShortDescription(), bugPattern.getDetailText(), formattedMessage, helpUri,
                tags, bugPattern.getCWEid());
    }
}
