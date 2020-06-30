package edu.umd.cs.findbugs.sarif;

import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.json.JSONObject;

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

    JSONObject toJSONObject() {
        JSONObject result = new JSONObject().put("descriptor", new JSONObject().put("id", id)).put("message", new JSONObject().put("text", message))
                .put("level", level);
        if (exception != null) {
            result.put("exception", exception.toJSONObject());
        }
        return result;
    }

    static Notification fromError(@NonNull AbstractBugReporter.Error error) {
        String id = String.format("spotbugs-error-%d", error.getSequence());
        Throwable cause = error.getCause();
        if (cause != null) {
            return new Notification(id, error.getMessage(), Level.ERROR, SarifException.fromThrowable(cause));
        } else {
            return new Notification(id, error.getMessage(), Level.ERROR, null);
        }
    }
}
