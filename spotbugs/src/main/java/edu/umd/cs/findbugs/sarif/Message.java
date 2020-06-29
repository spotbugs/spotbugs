package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Object which represents {@code message} property.
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317459">3.11 message object</a>
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
