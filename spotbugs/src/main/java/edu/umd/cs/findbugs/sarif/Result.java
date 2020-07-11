package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Object which represents result object in {@code run.results} property. (§3.14.23)
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317638">3.27 result object</a>
 */
final class Result {
    final String ruleId;
    final int ruleIndex;
    final Message message;
    final List<Location> locations;
    @NonNull
    final Level level;

    Result(@NonNull String ruleId, int ruleIndex, Message message, List<Location> locations, @NonNull Level level) {
        this.ruleId = Objects.requireNonNull(ruleId);
        this.ruleIndex = ruleIndex;
        this.message = Objects.requireNonNull(message);
        this.locations = Collections.unmodifiableList(Objects.requireNonNull(locations));
        this.level = Objects.requireNonNull(level);
    }

    JSONObject toJSONObject() {
        JSONObject result = new JSONObject().put("ruleId", ruleId).put("ruleIndex", ruleIndex).put("message", message.toJSONObject()).put("level",
                level);
        locations.stream().map(Location::toJSONObject).forEach(location -> result.append("locations", location));
        return result;
    }
}
