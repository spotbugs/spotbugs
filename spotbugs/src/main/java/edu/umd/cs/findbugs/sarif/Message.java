package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Object which represents {@code runs.results} property (§3.27.11).
 */
final class Message {
    @NonNull
    final List<String> arguments;

    Message(@NonNull List<String> arguments) {
        this.arguments = Collections.unmodifiableList(Objects.requireNonNull(arguments));
    }

    JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject().put("id", "default");
        if (!arguments.isEmpty()) {
            for (Object arg : arguments) {
                jsonObject.append("arguments", arg);
            }
        }
        return jsonObject;
    }
}
