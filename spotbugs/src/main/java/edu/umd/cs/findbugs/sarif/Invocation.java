package edu.umd.cs.findbugs.sarif;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NonNull;

/**
 * @see <a href="https://docs.oasis-open.org/sarif/sarif/v2.1.0/os/sarif-v2.1.0-os.html#_Toc34317567">3.20 invocation object</a>
 */
class Invocation {
    private final int exitCode;

    private final @NonNull String exitCodeDescription;

    private final boolean executionSuccessful;

    private final @NonNull List<Notification> toolExecutionNotifications;

    private final @NonNull List<Notification> toolConfigurationNotifications;

    Invocation(int exitCode, @NonNull String exitCodeDescription, boolean executionSuccessful, @NonNull List<Notification> toolExecutionNotifications,
            @NonNull List<Notification> toolConfigurationNotifications) {
        this.exitCode = exitCode;
        this.exitCodeDescription = Objects.requireNonNull(exitCodeDescription);
        this.executionSuccessful = executionSuccessful;
        this.toolExecutionNotifications = Collections.unmodifiableList(toolExecutionNotifications);
        this.toolConfigurationNotifications = Collections.unmodifiableList(toolConfigurationNotifications);
    }

    @NonNull
    JsonObject toJsonObject() {
        JsonObject result = new JsonObject();
        result.addProperty("exitCode", exitCode);
        result.addProperty("exitCodeDescription", exitCodeDescription);
        result.addProperty("executionSuccessful", executionSuccessful);

        JsonArray execNotificationArray = new JsonArray();
        toolExecutionNotifications.stream()
                .map(Notification::toJsonObject)
                .forEach(execNotificationArray::add);
        if (execNotificationArray.size() > 0) {
            result.add("toolExecutionNotifications", execNotificationArray);
        }

        JsonArray configNotificationArray = new JsonArray();
        toolConfigurationNotifications.stream()
                .map(Notification::toJsonObject)
                .forEach(configNotificationArray::add);
        if (configNotificationArray.size() > 0) {
            result.add("toolConfigurationNotifications", configNotificationArray);
        }
        return result;
    }
}
