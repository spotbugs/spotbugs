package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Object which represents result object in {@code run.results} property. (§3.14.23)
 */
final class Result {
    final String ruleId;
    final int ruleIndex;
    final Message message;
    // TODO add locations property

    Result(@NonNull String ruleId, int ruleIndex, Message message) {
        this.ruleId = Objects.requireNonNull(ruleId);
        this.ruleIndex = ruleIndex;
        this.message = Objects.requireNonNull(message);
    }

    JSONObject toJsonObject() {
        return new JSONObject().put("ruleId", ruleId).put("ruleIndex", ruleIndex).put("message", message.toJsonObject());
    }
}
