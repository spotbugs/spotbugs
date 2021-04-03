package edu.umd.cs.findbugs.sarif;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Object which represents {@code message} property.
 *
 * @see <a
 *     href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317459">3.11
 *     message object</a>
 */
final class Message {
    @NonNull
    final List<String> arguments;

    Message(@NonNull List<String> arguments) {
        this.arguments = Collections.unmodifiableList(Objects.requireNonNull(arguments));
    }

    JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "default");

        JsonArray jsonArray = new JsonArray();
        for (String arg : arguments) {
            jsonArray.add(arg);
        }
        if (jsonArray.size() > 0) {
            jsonObject.add("arguments", jsonArray);
        }
        return jsonObject;
    }
}
