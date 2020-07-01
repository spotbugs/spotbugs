package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.json.JSONObject;

import java.net.URI;
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
    final String defaultText;
    @Nullable
    final URI helpUri;

    Rule(@NonNull String id, @NonNull String shortDescription, @NonNull String defaultText, @Nullable URI helpUri) {
        this.id = Objects.requireNonNull(id);
        this.shortDescription = Objects.requireNonNull(shortDescription);
        this.defaultText = Objects.requireNonNull(defaultText);
        this.helpUri = helpUri;
    }

    JSONObject toJSONObject() {
        JSONObject messageStrings = new JSONObject().put("default", new JSONObject().put("text", defaultText));
        return new JSONObject().put("id", id).put("shortDescription", new JSONObject().put("text", shortDescription)).put(
                "messageStrings",
                messageStrings).putOpt("helpUri", helpUri);
    }

    @NonNull
    static Rule fromBugPattern(BugPattern bugPattern) {
        URI helpUri = bugPattern.getUri().orElse(null);
        return new Rule(bugPattern.getType(), bugPattern.getShortDescription(), bugPattern.getLongDescription(), helpUri);
    }
}
