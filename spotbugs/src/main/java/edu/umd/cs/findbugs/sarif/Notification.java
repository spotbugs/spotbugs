package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.ba.SourceFinder;
import com.google.gson.JsonObject;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * A class representing {@code notification} object (§3.58) in {@code run.invocations.toolExecutionNotifications} (§3.20.21)
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317894">3.58 notification object</a>
 */
class Notification {
    @NonNull
    final String id;
    @NonNull
    final String message;
    @NonNull
    final Level level;
    @Nullable
    final SarifException exception;

    Notification(@NonNull String id, @NonNull String message, @NonNull Level level, @Nullable SarifException exception) {
        this.id = Objects.requireNonNull(id);
        this.message = Objects.requireNonNull(message);
        this.level = Objects.requireNonNull(level);
        this.exception = exception;
    }

    JsonObject toJsonObject() {
        JsonObject descriptorJson = new JsonObject();
        descriptorJson.addProperty("id", id);

        JsonObject messageJson = new JsonObject();
        messageJson.addProperty("text", message);

        JsonObject result = new JsonObject();
        result.add("descriptor", descriptorJson);
        result.add("message", messageJson);
        result.addProperty("level", level.toJsonString());
        if (exception != null) {
            result.add("exception", exception.toJsonObject());
        }
        return result;
    }

    static Notification fromError(@NonNull AbstractBugReporter.Error error, @NonNull SourceFinder sourceFinder,
            @NonNull Map<URI, String> baseToId) {
        String id = String.format("spotbugs-error-%d", error.getSequence());
        Throwable cause = error.getCause();
        if (cause == null) {
            return new Notification(id, error.getMessage(), Level.ERROR, null);
        } else {
            return new Notification(id, error.getMessage(), Level.ERROR, SarifException.fromThrowable(cause, sourceFinder, baseToId));
        }
    }
}
