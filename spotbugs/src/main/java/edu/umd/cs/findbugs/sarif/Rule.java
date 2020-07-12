package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    Rule(@NonNull String id, @NonNull String shortDescription, @NonNull String fullDescription, @NonNull String defaultText, @Nullable URI helpUri,
            @NonNull List<String> tags) {
        this.id = Objects.requireNonNull(id);
        this.shortDescription = Objects.requireNonNull(shortDescription);
        this.fullDescription = Objects.requireNonNull(fullDescription);
        this.defaultText = Objects.requireNonNull(defaultText);
        this.helpUri = helpUri;
        this.tags = Collections.unmodifiableList(tags);
    }

    JSONObject toJSONObject() {
        JSONObject messageStrings = new JSONObject().put("default", new JSONObject().put("text", defaultText));
        // TODO put 'fullDescription' with both of text and markdown representations
        JSONObject result = new JSONObject()
                .put("id", id)
                .put("shortDescription", new JSONObject().put("text", shortDescription))
                .put("messageStrings", messageStrings)
                .putOpt("helpUri", helpUri);
        if (!tags.isEmpty()) {
            JSONObject propertyBag = new JSONObject().put("tags", new JSONArray(tags));
            result.put("properties", propertyBag);
        }
        return result;
    }

    @NonNull
    static Rule fromBugPattern(BugPattern bugPattern, String formatedMessage) {
        URI helpUri = bugPattern.getUri().orElse(null);

        String category = bugPattern.getCategory();
        List<String> tags;
        if (StringUtils.isEmpty(category)) {
            tags = Collections.emptyList();
        } else {
            tags = Collections.singletonList(category);
        }

        return new Rule(bugPattern.getType(), bugPattern.getShortDescription(), bugPattern.getDetailText(), formatedMessage, helpUri,
                tags);
    }
}
